const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {JSDOM}=require('jsdom');
const person={id:'test-payer',givenName:'Persona',familyName:'Ficticia',document:'00000000',email:'persona@example.invalid',phone:'099123456',documentType:'CI'};
function setup() {
 const dom=new JSDOM(`<stepper-pago><confirmar-pago><form><div><b>Moneda:</b><p>UYU</p></div><div><b>Total:</b><p>564,00</p></div><button type="button">Continuar</button></form></confirmar-pago></stepper-pago>`,{url:'https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago',runScripts:'outside-only'});
 dom.window.HTMLElement.prototype.getClientRects=function(){return [{width:100,height:30}];};
 for(const name of ['prex-native','prex-card','prex-payer','prex-express'])dom.window.eval(fs.readFileSync(`app/src/main/assets/${name}.js`,'utf8'));
 dom.window.BoleteraPayer.use(person);return dom;
}
function payer(dom) {
 const root=dom.window.document.querySelector('stepper-pago');
 root.innerHTML=`<alta-cliente><form class="ng-valid">${['nombreControl','apellidoControl','documentoControl','emailControl','celularControl'].map(name=>`<input formcontrolname="${name}">`).join('')}<button type="button">Continuar</button></form></alta-cliente>`;
 dom.window.BoleteraPayer.status();
}
test('shortcut skips two redundant steps once and stops at native card without submitting it',()=>{
 const dom=setup();try {const w=dom.window,d=w.document,api=w.BoleteraExpress;let summaries=0,payers=0,cards=0;
 d.querySelector('button').onclick=()=>{summaries++;payer(dom);d.querySelector('button').onclick=()=>{payers++;d.querySelector('stepper-pago').innerHTML='<alta-tarjeta><input formcontrolname="nroTarjetaControl"><input formcontrolname="expiracionControl"><input formcontrolname="cvvControl"><button type="button">Continuar</button></alta-tarjeta>';d.querySelector('button').onclick=()=>cards++;};};
 assert.equal(api.tick(),'off');assert.equal(summaries,0);assert.equal(api.start(56400,person),true);
 api.tick();api.tick();assert.equal(api.tick(),'done');api.tick();
 assert.equal(summaries,1);assert.equal(payers,1);assert.equal(cards,0);assert.equal(w.BoleteraNative.snapshot().stage,'card');
 assert.equal(api.start(56400,person),false);
 }finally{dom.window.close();}
});
test('visible CAPTCHA stops before payer continuation; no response or iframe content is read',()=>{
 const dom=setup();try {const w=dom.window,d=w.document;let clicks=0;payer(dom);
 const cap=d.createElement('angular-recaptcha');cap.innerHTML='<textarea name="g-recaptcha-response"></textarea>';d.querySelector('alta-cliente').append(cap);
 Object.defineProperty(cap.firstChild,'value',{get(){throw Error('No CAPTCHA token reads');}});
 d.querySelector('button').onclick=()=>clicks++;w.BoleteraExpress.start(56400,person);
 assert.equal(w.BoleteraExpress.tick(),'verification');assert.equal(w.BoleteraExpress.tick(),'verification');assert.equal(clicks,0);
 w.BoleteraExpress.stop();assert.equal(w.BoleteraNative.advance('payer'),true);assert.equal(clicks,1);
 }finally{dom.window.close();}
});
test('unexpected amount, currency, consents or changed payer do not advance',()=>{
 for(const mutation of [d=>d.querySelectorAll('p')[1].textContent='565,00',d=>d.querySelector('p').textContent='USD',d=>d.querySelector('form').insertAdjacentHTML('beforeend','<input type="checkbox">')]) {
 const dom=setup();try {let clicks=0;mutation(dom.window.document);dom.window.document.querySelector('button').onclick=()=>clicks++;dom.window.BoleteraExpress.start(56400,person);assert.equal(dom.window.BoleteraExpress.tick(),'manual');assert.equal(clicks,0);}finally{dom.window.close();}}
 const dom=setup();try {payer(dom);dom.window.document.querySelector('input').value='Otra persona';let clicks=0;dom.window.document.querySelector('button').onclick=()=>clicks++;dom.window.BoleteraExpress.start(56400,person);assert.equal(dom.window.BoleteraExpress.tick(),'manual');assert.equal(clicks,0);}finally{dom.window.close();}
});
test('stalled advance, pause and unknown forms never replay or resume automatically',()=>{
 const dom=setup();try {const w=dom.window;let clicks=0;w.document.querySelector('button').onclick=()=>clicks++;w.BoleteraExpress.start(56400,person);
 w.BoleteraExpress.tick();w.BoleteraExpress.tick();assert.equal(clicks,1);
 w.BoleteraExpress.stop();payer(dom);w.document.querySelector('button').onclick=()=>clicks++;assert.equal(w.BoleteraExpress.tick(),'manual');assert.equal(clicks,1);
 }finally{dom.window.close();}
});
