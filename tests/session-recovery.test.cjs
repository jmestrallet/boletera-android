const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {JSDOM}=require('jsdom');
function page(html,url='https://stm.gub.uy/app/mistm/cuenta/pages/recarga1.xhtml') {
 const dom=new JSDOM(html,{url,runScripts:'outside-only'}),w=dom.window;
 Object.defineProperty(w.document,'readyState',{value:'complete',configurable:true});
 w.HTMLElement.prototype.getBoundingClientRect=()=>({x:0,y:0,width:100,height:30});
 w.HTMLElement.prototype.getClientRects=()=>[{width:100,height:30}];
 for(const asset of ['stm-session','stm-adapter','stm-payment-return'])w.eval(fs.readFileSync(`app/src/main/assets/${asset}.js`,'utf8'));
 return dom;
}
test('expired-session messages outrank amount and generic alert errors, without submitting anything',()=>{
 for(const message of ['Tu sesión ha expirado. Volvé a ingresar.','Sesión vencida','La sesión caducó','javax.faces.application.ViewExpiredException']) {
  const dom=page(`<div role="alert">${message}</div><input id="recarga1:monto_input"><button>CONTINUAR</button>`);try {
   const api=dom.window.BoleteraAdapter;
   assert.equal(dom.window.BoleteraSession.snapshot(),'expired');assert.equal(api.snapshot().stage,'sessionExpired');assert.equal(api.command('amount','26000'),false);
  }finally{dom.window.close();}
 }
});
test('hidden messages, credential errors, outages and foreign pages are not session expiration',()=>{
 for(const [html,url] of [
  ['<p hidden>Sesión vencida</p>'],['<p role="alert">La contraseña es incorrecta</p>'],['<p role="alert">El servicio no está disponible</p>'],
  ['<p>Sesión vencida</p>','https://example.invalid/app/mistm/cuenta/pages/recarga1.xhtml'],
  ['<p>Sesión vencida</p>','https://pasarelaspe.sistarbanc.com.uy/v2/resultadoPago']]) {
   const dom=page(html,url);assert.equal(dom.window.BoleteraSession.snapshot(),'none');dom.window.close();
  }
});
test('STM payment return recognizes login and expiry before showing the original error page',()=>{
 for(const html of ['<button>INGRESAR CON USUARIO GUB.UY</button>','<div role="alert">Sesión expirada</div>']) {
  const dom=page(html,'https://stm.gub.uy/app/mistm/cuenta/pages/resultado.xhtml');assert.equal(dom.window.BoleteraNative.snapshot().stage,'sessionExpired');dom.window.close();
 }
 const dom=page('<input type="password" id="password">','https://mi.iduruguay.gub.uy/login');
 Object.defineProperty(dom.window.document.querySelector('input'),'value',{get(){throw Error('no password reads');}});
 assert.equal(dom.window.BoleteraSession.snapshot(),'login');assert.equal(dom.window.BoleteraNative.snapshot().stage,'sessionExpired');dom.window.close();
});
