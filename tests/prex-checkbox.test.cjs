const {test}=require('node:test');
const assert=require('node:assert/strict');
const {JSDOM}=require('jsdom');
const fs=require('node:fs');
const script=fs.readFileSync('app/src/main/assets/prex-checkbox.js','utf8');
function fixture(change=()=>{}) {
 const dom=new JSDOM('<stepper-pago><alta-cliente><angular-recaptcha><iframe src="https://www.google.com/recaptcha/api2/anchor?size=normal"></iframe><textarea name="g-recaptcha-response"></textarea></angular-recaptcha></alta-cliente></stepper-pago>',{url:'https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago',runScripts:'outside-only',pretendToBeVisual:true});
 const w=dom.window,d=w.document,f=d.querySelector('iframe');
 w.HTMLElement.prototype.getClientRects=function(){return [{}]};
 w.getComputedStyle=e=>({display:e.style.display||'block',visibility:e.style.visibility||'visible',opacity:e.style.opacity||'1'});
 f.getBoundingClientRect=()=>({x:10,y:20,width:304,height:78,right:314,bottom:98});
 d.elementFromPoint=()=>f;
 Object.defineProperty(f,'contentDocument',{get(){throw Error('Must not inspect cross-origin challenge')}});
 change(w,d,f);w.eval(script);
 return {dom,w,d,f,claim:()=>w.BoleteraCheckbox.claim('payer')};
}
test('claims one ordinary checkbox, returns only coordinates and never clicks or submits a form',()=>{
 const a=fixture();try {
  let clicks=0;a.d.addEventListener('click',()=>clicks++);
  assert.deepEqual(JSON.parse(JSON.stringify(a.claim())),{x:38,y:59,viewport:1024});
  assert.equal(a.claim(),null);assert.equal(clicks,0);
 }finally{a.dom.window.close()}
});
test('leaves resolved, hidden, covered, ambiguous and nonstandard widgets alone',()=>{
 const variations=[
  (w,d)=>d.querySelector('textarea').value='synthetic-response-do-not-export',
  (w,d)=>d.querySelector('alta-cliente').style.display='none',
  (w,d)=>d.elementFromPoint=()=>d.body,
  (w,d,f)=>f.src='https://www.google.com/recaptcha/api2/anchor?size=compact',
  (w,d,f)=>f.src='https://www.google.com.evil.invalid/recaptcha/api2/anchor?size=normal',
  (w,d,f)=>f.getBoundingClientRect=()=>({x:0,y:0,width:608,height:156,right:608,bottom:156}),
  (w,d,f)=>f.after(f.cloneNode()),
  (w,d)=>d.body.insertAdjacentHTML('beforeend','<iframe src="https://www.google.com/recaptcha/api2/bframe"></iframe>'),
  (w,d)=>d.body.insertAdjacentHTML('beforeend','<div role="dialog">Notice</div>')
 ];
 for(const variation of variations){const a=fixture(variation);try{assert.equal(a.claim(),null)}finally{a.dom.window.close()}}
});
test('does not install outside the original gateway or accept an unrelated stage',()=>{
 const a=fixture();try{assert.equal(a.w.BoleteraCheckbox.claim('finalConfirmation'),null)}finally{a.dom.window.close()}
 const dom=new JSDOM('',{url:'https://example.invalid/v2/',runScripts:'outside-only'});try{dom.window.eval(script);assert.equal(dom.window.BoleteraCheckbox,undefined)}finally{dom.window.close()}
});
