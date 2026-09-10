const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {JSDOM}=require('jsdom');
const code=fs.readFileSync('app/src/main/assets/prex-verification.js','utf8');
test('positions the existing component without replacing its iframe or provider connection; restores original styling',()=>{
 const dom=new JSDOM('<alta-cliente><angular-recaptcha style="margin:12px"><iframe src="https://www.google.com/recaptcha/api2/anchor"></iframe></angular-recaptcha><button>Continue</button></alta-cliente>',{url:'https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago',runScripts:'outside-only'});
 try {
  const d=dom.window.document,frame=d.querySelector('iframe'),root=frame.parentElement;let calls=0;
  root.addEventListener('fixture-provider-callback',()=>calls++);
  const originalStyle=root.getAttribute('style');dom.window.eval(code);
  const api=dom.window.BoleteraVerification;assert.equal(api.present(frame),true);assert.equal(api.present(frame),true);
  assert.equal(d.querySelectorAll('iframe').length,1);assert.equal(d.querySelector('iframe'),frame);assert.equal(frame.parentElement,root);
  assert.equal(d.querySelectorAll('style').length,1);root.dispatchEvent(new dom.window.Event('fixture-provider-callback'));assert.equal(calls,1);
  api.restore();assert.equal(root.hasAttribute('data-boletera-verification'),false);assert.equal(root.getAttribute('style'),originalStyle);assert.equal(d.querySelectorAll('style').length,0);assert.equal(frame.parentElement,root);
 }finally{dom.window.close();}
});
test('does not install on another site',()=>{
 const dom=new JSDOM('',{url:'https://example.invalid/v2/confirmarPago',runScripts:'outside-only'});try{dom.window.eval(code);assert.equal(dom.window.BoleteraVerification,undefined);}finally{dom.window.close();}
});
