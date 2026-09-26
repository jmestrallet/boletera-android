(() => {
  if(location.origin!=='https://pasarelaspe.sistarbanc.com.uy'||!location.pathname.startsWith('/v2/')||window.top!==window.self||window.BoleteraProviderVerification)return;
  const own=(object,key)=>object==null?undefined:Object.getOwnPropertyDescriptor(object,key)?.value;
  // Narrow adapter for the inspected Angular 11.2.14 build. Never serialize an instance/LView.
  function component(host,selector) {
    const context=own(host,'__ngContext__');
    const view=Array.isArray(context)?context:own(context,'lView');
    if(!Array.isArray(view)||typeof view[1]!=='object'||view[1]===null)return null;
    const indices=own(view[1],'components');
    const candidates=[view];
    if(Array.isArray(indices)&&indices.length<1000)for(const index of indices) {
      if(Number.isInteger(index)&&index>=20&&index<view.length)candidates.push(view[index]);
    }
    const found=new Set();
    for(const entry of candidates) {
      if(!Array.isArray(entry)||entry[0]!==host||typeof entry[1]!=='object'||entry[1]===null)continue;
      const instance=entry[8];
      if(!instance||typeof instance!=='object')continue;
      const ctor=own(Object.getPrototypeOf(instance),'constructor');
      const definition=own(ctor,'\u0275cmp');
      const selectors=own(definition,'selectors');
      if(own(definition,'type')===ctor&&Array.isArray(selectors)&&selectors.some(s=>Array.isArray(s)&&s.length===1&&s[0]===selector))found.add(instance);
    }
    return found.size===1?[...found][0]:null;
  }
  const visible=e=>{
    if(!e?.getClientRects().length)return false;
    for(let p=e;p;p=p.parentElement){const s=getComputedStyle(p);if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0'||p.getAttribute('aria-hidden')==='true')return false;}
    return true;
  };
  function supported() {
    const root=document.querySelector('app-root[ng-version="11.2.14"]');
    const bundle=[...document.scripts].some(s=>{
      try{const u=new URL(s.src);return u.origin===location.origin&&u.pathname==='/v2/main-es2015.c4dd4374250f3678bcc8.js';}catch{return false;}
    });
    return !!root&&bundle&&!!component(root,'app-root');
  }
  window.BoleteraProviderVerification={
    supported,
    accepted(root) {
      if(!supported()||!root?.matches('alta-cliente,alta-tarjeta')||!root.isConnected||!visible(root))return false;
      const hosts=[...root.querySelectorAll('angular-recaptcha')].filter(visible);
      if(hosts.length!==1)return false;
      const host=hosts[0],instance=component(host,'angular-recaptcha');
      if(!instance||own(instance,'recaptchaSuccess')!==true)return false;
      const ref=own(instance,'recaptchaRef');
      const element=own(own(ref,'elementRef'),'nativeElement');
      if(!element||element!==host.querySelector('re-captcha'))return false;
      // Google response presence prevents an expired/reset widget retaining an old success flag.
      // The provider flag, not this presence check, establishes server acceptance.
      return [...element.querySelectorAll('textarea[name="g-recaptcha-response"]')].some(e=>e.value.length>0);
    }
  };
})();
