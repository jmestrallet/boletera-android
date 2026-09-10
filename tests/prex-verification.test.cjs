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
test('native mask follows closing frames before the next native snapshot and restores the original page',()=>{
 const dom=new JSDOM('<style>body{background:red}</style><alta-cliente><button>Continuar</button><angular-recaptcha><iframe src="https://www.google.com/recaptcha/api2/anchor?size=normal"></iframe></angular-recaptcha></alta-cliente><div id="overlay"><iframe src="https://www.google.com/recaptcha/api2/bframe"></iframe></div>',{url:'https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago',runScripts:'outside-only'});
 try {
  const w=dom.window,d=w.document;let nextFrame=null;w.requestAnimationFrame=fn=>{nextFrame=fn;return 1};w.cancelAnimationFrame=()=>{nextFrame=null};
  w.getComputedStyle=e=>({display:e.style.display||'block',visibility:'visible',opacity:'1'});
  w.HTMLElement.prototype.getClientRects=function(){return [{width:100,height:100}]};
  d.documentElement.getBoundingClientRect=()=>({x:0,y:0});
  const frames=[...d.querySelectorAll('iframe')];frames[0].getBoundingClientRect=()=>({x:0,y:0,width:304,height:78});frames[1].getBoundingClientRect=()=>({x:20,y:100,width:304,height:680});
  frames.forEach(f=>Object.defineProperty(f,'contentDocument',{get(){throw Error('Must not inspect challenge contents')}}));
  const initial=frames[0],button=d.querySelector('button');w.eval(code);const api=w.BoleteraVerification;
  api.nativeMask(true);let style=d.querySelector('[data-boletera-captcha-mask]');assert.match(style.textContent,/v680/);
  d.querySelector('#overlay').remove();nextFrame();assert.doesNotMatch(style.textContent,/v680/);assert.match(style.textContent,/v78/);
  initial.style.display='none';nextFrame();assert.match(style.textContent,/inset\(100%\)/);
  assert.equal(w.getComputedStyle(button).visibility,'visible');assert.equal(d.querySelector('iframe'),initial);
  api.nativeMask(false);assert.equal(d.querySelector('[data-boletera-captcha-mask]'),null);assert.equal(nextFrame,null);assert.equal(d.querySelector('button'),button);
 }finally{dom.window.close()}
});
