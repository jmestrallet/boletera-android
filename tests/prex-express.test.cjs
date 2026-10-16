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

test('accepted provider verification advances payer once; Google presence alone and pause do not',()=>{
 const dom=setup();try {
  const w=dom.window,d=w.document;let accepted=false,clicks=0;payer(dom);
  d.querySelector('alta-cliente').insertAdjacentHTML('beforeend','<angular-recaptcha></angular-recaptcha>');
  w.BoleteraProviderVerification={accepted:()=>accepted};
  w.eval(fs.readFileSync('app/src/main/assets/prex-completion.js','utf8'));
  d.querySelector('button').onclick=()=>clicks++;w.BoleteraExpress.start(56400,person);
  assert.equal(w.BoleteraExpress.tick(),'verification');assert.equal(clicks,0);
  w.BoleteraExpress.suspend(true);accepted=true;w.BoleteraExpress.tick();assert.equal(clicks,0);
  w.BoleteraExpress.suspend(false);w.BoleteraExpress.tick();w.BoleteraExpress.tick();assert.equal(clicks,1);
 }finally{dom.window.close()}
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

test('suspension preserves the same request but cannot advance until resumed',()=>{
 const dom=setup();try {
  const w=dom.window;let clicks=0;
  w.document.querySelector('button').onclick=()=>clicks++;
  w.BoleteraExpress.start(56400,person);w.BoleteraExpress.suspend(true);
  w.BoleteraExpress.tick();assert.equal(clicks,0);
  w.BoleteraExpress.suspend(false);w.BoleteraExpress.tick();w.BoleteraExpress.tick();assert.equal(clicks,1);
  w.BoleteraExpress.stop();w.BoleteraExpress.suspend(false);w.BoleteraExpress.tick();assert.equal(clicks,1);
 }finally{dom.window.close();}
});

const nextRender=()=>new Promise(resolve=>setTimeout(resolve,30));
test('Express follows completed renders without waiting for Android polling',async()=>{
 const dom=setup();try {
  const w=dom.window,d=w.document;let summaries=0,payers=0,cards=0;
  d.querySelector('button').onclick=()=>{
   summaries++;payer(dom);d.querySelector('form').className='ng-pending';
   d.querySelector('button').onclick=()=>{
    payers++;d.querySelector('stepper-pago').innerHTML='<alta-tarjeta><input formcontrolname="nroTarjetaControl"><input formcontrolname="expiracionControl"><input formcontrolname="cvvControl"><button type="button">Continuar</button></alta-tarjeta>';
    d.querySelector('button').onclick=()=>cards++;
   };
  };
  w.BoleteraExpress.start(56400,person);
  await nextRender();assert.equal(summaries,1);assert.equal(payers,0);
  d.querySelector('form').className='ng-valid';
  await nextRender();assert.equal(payers,1);assert.equal(cards,0);
  assert.equal(w.BoleteraExpress.tick(),'done');
  d.querySelector('button').disabled=false;await nextRender();assert.equal(payers,1);
 }finally{dom.window.close();}
});
test('pending validation remains bounded and never overrides errors or suspension',async()=>{
 for(const mode of ['pause','stop','error','changed','timeout']) {
  const dom=setup();try {
   const w=dom.window,d=w.document;let clicks=0;payer(dom);
   d.querySelector('form').className='ng-pending';d.querySelector('button').onclick=()=>clicks++;
   w.BoleteraExpress.start(56400,person);w.BoleteraExpress.tick();
   if(mode==='pause')w.BoleteraExpress.suspend(true);
   if(mode==='stop')w.BoleteraExpress.stop();
   if(mode==='error')d.querySelector('form').insertAdjacentHTML('beforeend','<mat-error>Error del proveedor</mat-error>');
   if(mode==='changed')d.querySelector('input').value='Otra persona';
   if(mode==='timeout'){const now=w.Date.now();w.Date.now=()=>now+21000;}
   d.querySelector('form').className='ng-valid';await nextRender();
   assert.equal(clicks,0,mode);
   if(mode==='pause'){w.BoleteraExpress.suspend(false);await nextRender();assert.equal(clicks,1);}
  }finally{dom.window.close();}
 }
});

// The whole Express journey on a closer copy of Prex's page: a stepper whose later steps are built hidden, the
// document type as an Angular dropdown that ignores its first press, and the check shown only after «Continuar»
// on the payer step (Lab 15–16 reports, 16/10). The loop below plays Android's polling.
test('full journey: hidden steps, a dropdown that ignores its first press and a check shown after Continuar',async()=>{
 const dom=new JSDOM(`<stepper-pago>
  <section class="mat-horizontal-stepper-content" aria-expanded="true"><confirmar-pago><form><div><b>Moneda:</b><p>UYU</p></div><div><b>Total:</b><p>564,00</p></div><button type="button" id="s">Continuar</button></form></confirmar-pago></section>
  <section class="mat-horizontal-stepper-content" aria-expanded="false"><alta-cliente><form class="ng-valid"><mat-select role="combobox" formcontrolname="tipoDocumentoControl"><div class="mat-select-trigger"><span class="mat-select-placeholder">Tipo Documento</span></div></mat-select>${['nombreControl','apellidoControl','documentoControl','emailControl','celularControl'].map(n=>`<input formcontrolname="${n}">`).join('')}<button type="button" id="p">Continuar</button></form></alta-cliente></section>
  <section class="mat-horizontal-stepper-content" aria-expanded="false"><alta-tarjeta><input formcontrolname="nroTarjetaControl"><input formcontrolname="expiracionControl"><input formcontrolname="cvvControl"><button type="button">Continuar</button></alta-tarjeta></section>
 </stepper-pago>`,{url:'https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago',runScripts:'outside-only'});
 const w=dom.window,d=w.document;
 // Only the open step has layout.
 w.HTMLElement.prototype.getClientRects=function(){return this.closest('.mat-horizontal-stepper-content[aria-expanded="false"]')?[]:[{width:100,height:30}];};
 for(const name of ['prex-native','prex-card','prex-payer','prex-express'])w.eval(fs.readFileSync(`app/src/main/assets/${name}.js`,'utf8'));
 try {
  const sections=d.querySelectorAll('section'),show=i=>sections.forEach((s,k)=>s.setAttribute('aria-expanded',String(k===i)));
  const select=d.querySelector('mat-select');let presses=0,opened=0,payerSends=0,captcha=false,accepted=false;
  // The dropdown ignores its first press, then opens its panel outside the form, like Angular's overlay.
  select.querySelector('.mat-select-trigger').onclick=()=>{
   presses++;if(presses===1)return;opened++;
   setTimeout(()=>{select.setAttribute('aria-controls','doc-panel');select.setAttribute('aria-expanded','true');
    const panel=d.createElement('div');panel.id='doc-panel';panel.setAttribute('role','listbox');
    const ci=d.createElement('mat-option');ci.setAttribute('role','option');ci.textContent='Cédula de Identidad';
    ci.onclick=()=>{select.querySelector('.mat-select-trigger').innerHTML='<span class="mat-select-value-text">Cédula de Identidad</span>';select.setAttribute('aria-expanded','false');panel.remove();};
    panel.append(ci);d.body.append(panel);},0);
  };
  d.getElementById('s').onclick=()=>show(1);
  // Prex answers the first payer «Continuar» with its check; once accepted, it moves on to the card.
  d.getElementById('p').onclick=()=>{
   payerSends++;
   if(!accepted){ if(!captcha){captcha=true;d.querySelector('alta-cliente form').insertAdjacentHTML('beforeend','<angular-recaptcha></angular-recaptcha>');} return; }
   show(2);
  };
  w.BoleteraProviderVerification={accepted:()=>accepted};
  w.BoleteraPayer.use(person);assert.equal(w.BoleteraExpress.start(56400,person),true);
  const poll=async(ms)=>{for(let t=0;t<ms;t+=100){w.BoleteraExpress.tick();w.BoleteraNative.snapshot();w.BoleteraPayer.status();await new Promise(r=>setTimeout(r,100));}};
  await poll(4000);
  // The payer got filled even though the first press on the dropdown did nothing, and the check stopped Express.
  assert.ok(presses>=2,'presses the dropdown again');assert.equal(opened,1);
  assert.equal(w.BoleteraPayer.status(),'filled');
  assert.deepEqual([...d.querySelectorAll('alta-cliente input')].map(e=>e.value.length>0),[true,true,true,true,true]);
  assert.equal(captcha,true);assert.equal(w.BoleteraNative.snapshot().stage,'payer');
  const sendsBefore=payerSends;
  // The person solves the check: Express continues by itself, once, and stops at the card step.
  accepted=true;await poll(1500);
  assert.equal(w.BoleteraNative.snapshot().stage,'card');assert.equal(payerSends,sendsBefore+1);
 }finally{w.close();}
});
