const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {JSDOM}=require('jsdom');
const script=fs.readFileSync('app/src/main/assets/prex-card.js','utf8');
const native=fs.readFileSync('app/src/main/assets/prex-native.js','utf8');
function setup(url='https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago') {
 const dom=new JSDOM(`<stepper-pago><alta-tarjeta><form><input formcontrolname="nroTarjetaControl" minlength="16" maxlength="19"><input formcontrolname="expiracionControl" maxlength="5"><input formcontrolname="cvvControl" minlength="3" maxlength="4"><button type="button">Continuar</button></form></alta-tarjeta></stepper-pago>`,{url,runScripts:'outside-only'});
 dom.window.HTMLElement.prototype.getClientRects=function(){return [{width:100,height:30}];};
 dom.window.eval(script);dom.window.eval(native);return dom;
}
const settle=()=>new Promise(r=>setTimeout(r,15));
test('only explicit valid submit fills existing inputs and clicks once; snapshots never contain card data',async()=>{
 const dom=setup();try {
 const w=dom.window,d=w.document,api=w.BoleteraCard;let clicks=0,events=0;
 d.querySelector('button').onclick=()=>clicks++;
 d.querySelector('form').addEventListener('input',()=>events++);
 assert.equal(w.BoleteraNative.snapshot().stage,'card');assert.equal(w.BoleteraNative.advance('card'),false);
 assert.equal(api.submit('4111111111111112','12/39','123'),false);
 assert.equal(api.submit('4111111111111111','01/20','123'),false);
 assert.equal(api.submit('4111111111111111','12/39','12'),false);assert.equal(events,0);
 assert.equal(api.submit('4111111111111111','12/39','123'),true);
 assert.equal(api.submit('4111111111111111','12/39','123'),false);
 await settle();assert.equal(clicks,1);assert.equal(events,3);
 assert.deepEqual([...d.querySelectorAll('input')].map(e=>e.value),['4111 1111 1111 1111','12/39','123']);
 d.querySelectorAll('input').forEach(e=>Object.defineProperty(e,'value',{get(){throw Error('Snapshot read a card field');}}));
 assert.equal(api.snapshot().busy,true);assert.equal(w.BoleteraNative.snapshot().values.length,0);
 assert.ok(!JSON.stringify(w.BoleteraNative.snapshot()).includes('4111'));
 }finally{dom.window.close();}
});
test('consents, unknown controls, duplicate fields, foreign origins and dialogs stay original',()=>{
 const foreign=setup('https://example.invalid/v2/confirmarPago');assert.equal(foreign.window.BoleteraCard,undefined);foreign.window.close();
 for(const extra of ['<input type="checkbox">','<mat-checkbox>Recordar</mat-checkbox>','<select></select>','<input formcontrolname="cvvControl">','<div role="dialog">Autorizar</div>']) {
  const dom=setup();try {dom.window.document.querySelector('alta-tarjeta').insertAdjacentHTML('beforeend',extra);
   assert.equal(dom.window.BoleteraCard.snapshot().available,false);assert.equal(dom.window.BoleteraNative.snapshot().stage,'original');
   assert.equal(dom.window.BoleteraCard.submit('4111111111111111','12/39','123'),false);
  }finally{dom.window.close();}
 }
});
test('Angular validation blocks send and server rejection requires a new explicit submit',async()=>{
 const dom=setup();try {const d=dom.window.document,api=dom.window.BoleteraCard;let clicks=0;
 const b=d.querySelector('button');b.onclick=()=>{clicks++;b.disabled=true;};
 d.querySelector('form').classList.add('ng-invalid');api.submit('4111111111111111','12/39','123');await settle();assert.equal(clicks,0);assert.equal(api.snapshot().error,true);
 d.querySelector('form').classList.remove('ng-invalid');assert.equal(api.submit('4111111111111111','12/39','123'),true);await settle();assert.equal(clicks,1);assert.equal(api.snapshot().busy,true);
 b.disabled=false;assert.equal(api.snapshot().error,true);assert.equal(clicks,1);
 assert.equal(api.submit('4111111111111111','12/39','123'),true);await settle();assert.equal(clicks,2);
 }finally{dom.window.close();}
});
test('additional authorization preserves the in-flight guard; no timeout or hidden retry',async()=>{
 const dom=setup();try {const d=dom.window.document,api=dom.window.BoleteraCard;let clicks=0;
 d.querySelector('button').onclick=()=>clicks++;api.submit('4111111111111111','12/39','123');await settle();
 const modal=d.createElement('div');modal.setAttribute('role','dialog');d.body.append(modal);assert.equal(api.snapshot().available,false);
 modal.remove();assert.equal(api.snapshot().busy,true);assert.equal(api.submit('4111111111111111','12/39','123'),false);assert.equal(clicks,1);
 }finally{dom.window.close();}
});
