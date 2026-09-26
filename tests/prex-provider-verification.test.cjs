const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {JSDOM}=require('jsdom');
const code=fs.readFileSync('app/src/main/assets/prex-provider-verification.js','utf8');
function fixture() {
 const dom=new JSDOM('<script src="/v2/main-es2015.c4dd4374250f3678bcc8.js"></script><app-root ng-version="11.2.14"><stepper-pago><alta-cliente><angular-recaptcha><re-captcha><textarea name="g-recaptcha-response"></textarea></re-captcha></angular-recaptcha></alta-cliente></stepper-pago></app-root>',{url:'https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago',runScripts:'outside-only'});
 const w=dom.window,d=w.document;
 w.HTMLElement.prototype.getClientRects=function(){return [{}]};
 w.getComputedStyle=e=>({display:e.style.display||'block',visibility:e.style.visibility||'visible',opacity:e.style.opacity||'1'});
 function bind(host,selector) {
  class Component{};Component['\u0275cmp']={type:Component,selectors:[[selector]]};
  const instance=new Component(),child=[];child[0]=host;child[1]={};child[8]=instance;
  const parent=[];parent[1]={components:[20]};parent[20]=child;host.__ngContext__=parent;
  return instance;
 }
 bind(d.querySelector('app-root'),'app-root');
 const host=d.querySelector('angular-recaptcha'),instance=bind(host,'angular-recaptcha');
 instance.recaptchaRef={elementRef:{nativeElement:d.querySelector('re-captcha')}};
 instance.recaptchaSuccess=false;
 const root=d.querySelector('alta-cliente'),response=d.querySelector('textarea');
 w.eval(code);
 return {dom,w,d,host,instance,root,response,api:w.BoleteraProviderVerification};
}
test('Google response alone never means provider acceptance; provider success plus live response does',()=>{
 const a=fixture();try {
  assert.equal(a.api.supported(),true);assert.equal(a.api.accepted(a.root),false);
  a.response.value='synthetic';assert.equal(a.api.accepted(a.root),false);
  a.instance.recaptchaSuccess=true;assert.equal(a.api.accepted(a.root),true);
  a.response.value='';assert.equal(a.api.accepted(a.root),false);
  a.response.value='synthetic';a.instance.recaptchaSuccess=false;assert.equal(a.api.accepted(a.root),false);
 }finally{a.w.close()}
});
test('unknown provider versions, mismatched hosts, hidden widgets and string success remain manual',()=>{
 const changes=[
  a=>a.d.querySelector('script').src='/v2/main-es2015.new-build.js',
  a=>a.d.querySelector('app-root').setAttribute('ng-version','20.0.0'),
  a=>a.host.__ngContext__=42,
  a=>a.host.__ngContext__[20][0]=a.root,
  a=>a.instance.recaptchaRef.elementRef.nativeElement=a.root,
  a=>a.instance.recaptchaSuccess='true',
  a=>a.host.style.display='none',
  a=>Object.defineProperty(a.instance,'recaptchaSuccess',{get(){throw Error('A getter must not be invoked')}})
 ];
 for(const change of changes){const a=fixture();try{a.instance.recaptchaSuccess=true;a.response.value='synthetic';change(a);assert.equal(a.api.accepted(a.root),false)}finally{a.w.close()}}
});
test('does not read tokens or component form fields and does not replace provider methods',()=>{
 const a=fixture();try {
  const poison=()=>{throw Error('Forbidden sensitive access')};
  Object.defineProperty(a.instance,'authService',{get:poison});Object.defineProperty(a.instance,'form',{get:poison});
  Object.defineProperty(a.w,'localStorage',{get:poison});Object.defineProperty(a.w,'grecaptcha',{get:poison});
  const callback=()=>{};a.instance.resolved=callback;
  a.instance.recaptchaSuccess=true;a.response.value='synthetic';
  assert.equal(a.api.accepted(a.root),true);assert.equal(a.instance.resolved,callback);
 }finally{a.w.close()}
});
