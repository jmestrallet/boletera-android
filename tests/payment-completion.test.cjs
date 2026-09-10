const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {JSDOM}=require('jsdom');
const pairs=(items)=>items.map(([a,b])=>`<div><p>${a}:</p><p>${b}</p></div>`).join('');
const confirmation=()=>`<stepper-pago><finalizar-pago><form>${pairs([['Comercio','STM Recargas'],['Cliente','999'],['Transacción','TEST-901'],['Medio de Pago','PREX **** 1234'],['Importe','$ 564,00']])}<button type="button">Confirmar</button><input type="hidden" id="secret"></form></finalizar-pago></stepper-pago>`;
const receipt=(status='El pago se realizó con éxito')=>`<resultado-pago><h4>Comprobante de Pago</h4><b>${status}</b>${pairs([['Número de autorización','TEST-123'],['Fecha de Pago','01/01/2026 10:00:00'],['Comercio','STM Recargas'],['Transacción','TEST-901'],['TOTAL','$ 564,00']])}<a>Volver a la página inicial</a></resultado-pago>`;
function page(html,url='https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago') {
 const dom=new JSDOM(html,{url,runScripts:'outside-only'}),w=dom.window;
 w.HTMLElement.prototype.getClientRects=function(){return [{width:100,height:30}];};
 for(const name of ['prex-native','prex-completion','stm-payment-return'])w.eval(fs.readFileSync(`app/src/main/assets/${name}.js`,'utf8'));
 w.BoleteraCompletion?.configure(56400);return dom;
}
test('final confirmation reads only masked display rows, requires a tap, and cannot repeat after stage changes',()=>{
 const dom=page(confirmation());try {const w=dom.window,d=w.document,api=w.BoleteraNative;let clicks=0;
 Object.defineProperty(d.getElementById('secret'),'value',{get(){throw Error('no secret reads');}});
 d.querySelector('button').onclick=()=>clicks++;
 assert.equal(api.snapshot().stage,'finalConfirmation');assert.equal(clicks,0);
 w.eval(fs.readFileSync('app/src/main/assets/prex-express.js','utf8'));
 w.BoleteraExpress.start(56400,{});w.BoleteraExpress.tick();assert.equal(clicks,0);
 assert.equal(api.advance('finalConfirmation'),true);assert.equal(api.advance('finalConfirmation'),false);assert.equal(clicks,1);
 d.body.innerHTML=receipt();assert.equal(api.snapshot().stage,'receipt');d.body.innerHTML=confirmation();
 assert.equal(api.advance('finalConfirmation'),false);
 }finally{dom.window.close();}
});
test('amount mismatch, unmasked card, extra controls, ambiguous actions and CAPTCHA fail closed',()=>{
 for(const mutate of [
  d=>d.querySelector('finalizar-pago').innerHTML=d.querySelector('finalizar-pago').innerHTML.replace('564,00','999,00'),
  d=>d.querySelector('finalizar-pago').innerHTML=d.querySelector('finalizar-pago').innerHTML.replace('PREX **** 1234','PREX 4111111111111111'),
  d=>d.querySelector('form').insertAdjacentHTML('beforeend','<select><option>3 cuotas</option></select>'),
  d=>d.querySelector('form').insertAdjacentHTML('beforeend','<button>Confirmar</button>'),
  d=>d.querySelector('form').insertAdjacentHTML('beforeend','<iframe src="https://www.google.com/recaptcha/api2/bframe"></iframe>'),
  d=>d.body.insertAdjacentHTML('beforeend','<div role="dialog">Verificación</div>')]) {
  const dom=page(confirmation());try {mutate(dom.window.document);assert.equal(dom.window.BoleteraNative.advance('finalConfirmation'),false);}finally{dom.window.close();}
 }
});
test('success, rejection and pending are distinct; return uses original action once, never a fabricated URL',()=>{
 for(const [status,stage] of [['El pago se realizó con éxito','receipt'],['La transacción fue rechazada','paymentRejected'],['El pago se encuentra en estado: Pendiente','paymentPending']]) {
  const dom=page(receipt(status));try {const api=dom.window.BoleteraNative;let returns=0;dom.window.document.querySelector('a').onclick=()=>returns++;
   assert.equal(api.snapshot().stage,stage);assert.equal(returns,0);assert.equal(api.advance(stage),true);assert.equal(api.advance(stage),false);assert.equal(returns,1);
  }finally{dom.window.close();}
 }
 for(const html of [receipt('Estado desconocido'),receipt().replace('564,00','565,00'),receipt().replace('STM Recargas','Otro comercio')]) {
  const dom=page(html);assert.equal(dom.window.BoleteraNative.snapshot().stage,'original');dom.window.close();
 }
});
test('STM success requires both explicit statements; continuation is one-shot and never allowed on amount/provider pages',()=>{
 const html='<h1>¡Recarga exitosa!</h1><p>Recarga Confirmada.</p><button>CONTINUAR</button>';
 for(const path of ['resultado.xhtml','recarga1.xhtml','recarga2.xhtml']) {
  const dom=page(html,`https://stm.gub.uy/app/mistm/cuenta/pages/${path}`);try {let clicks=0;dom.window.document.querySelector('button').onclick=()=>clicks++;
   const api=dom.window.BoleteraNative;assert.equal(api.snapshot().stage,path==='resultado.xhtml'?'stmSuccess':'original');
   assert.equal(api.advance('stmSuccess'),path==='resultado.xhtml');assert.equal(api.advance('stmSuccess'),false);assert.equal(clicks,path==='resultado.xhtml'?1:0);
  }finally{dom.window.close();}
 }
 const dom=page(html.replace('Recarga Confirmada.','Pendiente.'),'https://stm.gub.uy/app/mistm/cuenta/pages/resultado.xhtml');assert.equal(dom.window.BoleteraNative.snapshot().stage,'original');dom.window.close();
});
test('STM balance requires actual displayed balance and recharge control; wrong origin and login never close the panel',()=>{
 for(const [html,url,stage] of [
  ['<div>Operativa <label>Saldo disponible*:</label> $ 824</div><button>Recargar</button>','https://stm.gub.uy/app/mistm/cuenta/pages/principal.xhtml','returnBalance'],
  ['<button>INGRESAR CON USUARIO GUB.UY</button>','https://stm.gub.uy/app/mistm/cuenta/pages/principal.xhtml','original'],
  ['<p>Saldo disponible*: $ 824</p>','https://stm.gub.uy/app/mistm/cuenta/pages/principal.xhtml','original']]) {
  const dom=page(html,url);assert.equal(dom.window.BoleteraNative.snapshot().stage,stage);dom.window.close();
 }
 const dom=page('<p>Recarga Confirmada.</p>','https://example.invalid/app/mistm/cuenta/pages/principal.xhtml');assert.equal(dom.window.BoleteraNative,undefined);dom.window.close();
});

test('invisible reCAPTCHA badge does not hide final confirmation; expanded challenge keeps original UI',()=>{
 const dom=page(confirmation());try {const d=dom.window.document,api=dom.window.BoleteraNative;
 d.querySelector('form').insertAdjacentHTML('beforeend','<iframe src="https://www.google.com/recaptcha/api2/anchor?size=invisible"></iframe>');
 assert.equal(api.snapshot().stage,'finalConfirmation');
 d.querySelector('iframe').src='https://www.google.com/recaptcha/api2/bframe?size=invisible';
 assert.equal(api.snapshot().stage,'original');
 }finally{dom.window.close();}
});
