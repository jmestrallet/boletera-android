const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {JSDOM}=require('jsdom');
const script=fs.readFileSync('app/src/main/assets/prex-native.js','utf8');
function setup(url='https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago') {
 const dom=new JSDOM(`<stepper-pago><section class="mat-horizontal-stepper-content" aria-expanded="true"><confirmar-pago><form><div><b>Importe</b><p>260</p></div><button>CONTINUAR</button></form></confirmar-pago></section><section class="mat-horizontal-stepper-content" aria-expanded="false"><alta-cliente><input formcontrolname="nombreControl" value="Ficticia"><button disabled>Continuar</button></alta-cliente></section><alta-tarjeta><input id="pan"></alta-tarjeta></stepper-pago>`,{url,runScripts:'outside-only'});
 dom.window.HTMLElement.prototype.getClientRects=function(){return [{width:100,height:30}];};
 dom.window.document.querySelector('form').addEventListener('submit',e=>e.preventDefault());
 Object.defineProperty(dom.window.document.getElementById('pan'),'value',{get(){throw Error('Card value must not be read');}});
 dom.window.eval(script);return dom;
}
test('native snapshot ignores hidden steps and never reads card data; explicit continuation happens once',()=>{
 const dom=setup();try {const d=dom.window.document, api=dom.window.BoleteraNative;let clicks=0;
 d.querySelector('confirmar-pago button').onclick=()=>clicks++;
 assert.equal(api.snapshot().stage,'summary');assert.deepEqual(JSON.parse(JSON.stringify(api.snapshot().rows)),[['Importe','260']]);
 assert.equal(clicks,0);assert.equal(api.advance('payer'),false);assert.equal(api.advance('summary'),true);assert.equal(api.advance('summary'),false);assert.equal(clicks,1);
 const sections=d.querySelectorAll('section');sections[0].setAttribute('aria-expanded','false');sections[1].setAttribute('aria-expanded','true');
 assert.equal(api.snapshot().stage,'payer');assert.equal(api.snapshot().values[0],'Ficticia');assert.equal(api.advance('payer'),false);
 d.querySelector('alta-cliente button').disabled=false;assert.equal(api.advance('payer'),true);assert.equal(api.advance('payer'),false);
 }finally{dom.window.close();}
});
test('foreign origin, duplicate actions, and unknown card steps do not receive continuation',()=>{
 const foreign=setup('https://example.invalid/v2/confirmarPago');assert.equal(foreign.window.BoleteraNative,undefined);foreign.window.close();
 const dom=setup();try {const d=dom.window.document,api=dom.window.BoleteraNative;
 d.querySelector('confirmar-pago').append(d.querySelector('confirmar-pago button').cloneNode(true));assert.equal(api.advance('summary'),false);
 d.querySelectorAll('section').forEach(e=>e.setAttribute('aria-expanded','false'));assert.equal(api.snapshot().stage,'original');assert.equal(api.advance('original'),false);
 }finally{dom.window.close();}
});

test('keeps the native loading screen while the provider builds or replaces its form',()=>{
 const dom=setup();try {
  const d=dom.window.document,api=dom.window.BoleteraNative;
  const summary=d.querySelector('section');
  d.body.replaceChildren(d.createElement('stepper-pago'));
  assert.equal(api.snapshot().stage,'loading');
  d.querySelector('stepper-pago').append(summary);
  assert.equal(api.snapshot().stage,'summary');
  summary.remove();assert.equal(api.snapshot().stage,'loading');
  const card=d.createElement('alta-tarjeta');card.innerHTML='<input autocomplete="cc-number">';d.querySelector('stepper-pago').append(card);
  assert.equal(api.snapshot().stage,'original');
 }finally{dom.window.close();}
});

test('only original visible CAPTCHA geometry is exposed; provider dialogs remain visible',()=>{
 const dom=setup();try {const d=dom.window.document,api=dom.window.BoleteraNative;
 const sections=d.querySelectorAll('section');sections[0].setAttribute('aria-expanded','false');sections[1].setAttribute('aria-expanded','true');
 const frame=d.createElement('iframe');frame.src='https://www.google.com/recaptcha/api2/anchor?size=normal';frame.getBoundingClientRect=()=>({x:12,y:40,width:304,height:78});const wrap=d.createElement('div');wrap.append(frame);d.querySelector('alta-cliente').append(wrap);
 let scrolls=0;frame.scrollIntoView=()=>scrolls++;assert.equal(api.snapshot().challenge.width,304);assert.equal(scrolls,0);api.showVerification();assert.equal(scrolls,1);
 frame.parentElement.style.opacity='0';assert.equal(api.snapshot().challenge,null);frame.parentElement.style.opacity='1';
 const modal=d.createElement('div');modal.setAttribute('role','alertdialog');d.body.append(modal);assert.equal(api.snapshot().stage,'original');assert.equal(api.advance('payer'),false);
 }finally{dom.window.close();}
});
