(() => {
  if (location.origin !== 'https://pasarelaspe.sistarbanc.com.uy' || !location.pathname.startsWith('/v2/') || window.top !== window.self || window.BoleteraVerification) return;
  let component = null;
  let style = null;
  let maskStyle = null, maskFrame = null;
  const visibleFrame = frame => {
    if(!frame.getClientRects().length)return false;
    for(let e=frame;e;e=e.parentElement){const s=getComputedStyle(e);if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0')return false;}
    try {const u=new URL(frame.src);return ['www.google.com','www.recaptcha.net','www.gstatic.com'].includes(u.hostname)&&/\/recaptcha\//.test(u.pathname)&&(/\/bframe/.test(u.pathname)||u.searchParams.get('size')!=='invisible');}catch{return false;}
  };
  function paintMask() {
    if(!maskStyle)return;
    const origin=document.documentElement.getBoundingClientRect();
    const paths=[...document.querySelectorAll('iframe')].filter(visibleFrame).map(frame=>{
      const r=frame.getBoundingClientRect(),x=r.x-origin.x,y=r.y-origin.y;
      return `M${x} ${y}h${r.width}v${r.height}h${-r.width}Z`;
    });
    // Clip presentation only. DOM visibility, form state, iframe and callbacks stay untouched.
    // Refresh before each paint so a closed challenge cannot expose the provider behind the old native crop.
    const css=`html{background:transparent!important;clip-path:${paths.length?`path('${paths.join(' ')}')`:'inset(100%)'}!important}body{background:transparent!important}`;
    if(maskStyle.textContent!==css)maskStyle.textContent=css;
    maskFrame=requestAnimationFrame(paintMask);
  }
  function nativeMask(enabled) {
    if(!enabled){if(maskFrame!==null)cancelAnimationFrame(maskFrame);maskFrame=null;maskStyle?.remove();maskStyle=null;return;}
    if(maskStyle)return;
    maskStyle=document.createElement('style');maskStyle.setAttribute('data-boletera-captcha-mask','true');document.head.append(maskStyle);paintMask();
  }
  function restore() {
    component?.removeAttribute('data-boletera-verification');
    style?.remove(); component = null; style = null;
  }
  window.BoleteraVerification = {
    restore,
    nativeMask,
    present(frame) {
      const root = frame?.closest('angular-recaptcha');
      if (!root || !root.closest('alta-cliente,alta-tarjeta')) return false;
      if (component === root && root.isConnected) return true;
      restore();
      component = root;
      // Position the EXISTING component. Never move/recreate the iframe, load its
      // source separately, inspect its contents or replace the provider callback.
      component.setAttribute('data-boletera-verification', 'true');
      style = document.createElement('style');
      // The checkbox must stay BELOW the provider's image/audio challenge overlay.
      style.textContent = `[data-boletera-verification="true"]{position:fixed!important;top:0!important;left:0!important;right:auto!important;bottom:auto!important;display:block!important;width:max-content!important;max-width:none!important;min-width:0!important;height:auto!important;min-height:0!important;margin:0!important;padding:0!important;z-index:1!important}`;
      document.head.append(style);
      return true;
    }
  };
})();
