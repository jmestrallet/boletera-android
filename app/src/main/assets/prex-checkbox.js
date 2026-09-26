(() => {
  if (location.origin !== 'https://pasarelaspe.sistarbanc.com.uy' || !location.pathname.startsWith('/v2/') || window.top !== window.self || window.BoleteraCheckbox) return;
  const attempted = new WeakSet();
  const visible = e => {
    if (!e?.getClientRects().length) return false;
    for (let p=e;p;p=p.parentElement) {
      const s=getComputedStyle(p);
      if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0'||p.getAttribute('aria-hidden')==='true'||p.getAttribute('aria-expanded')==='false')return false;
    }
    return true;
  };
  const googleFrame = (e,kind) => {
    try {
      const u=new URL(e.src);
      return u.protocol==='https:' && ['www.google.com','www.recaptcha.net'].includes(u.hostname) &&
        new RegExp('^/recaptcha/(api2|enterprise)/'+kind+'$').test(u.pathname);
    } catch {return false;}
  };
  window.BoleteraCheckbox = {
    claim(stage) {
      if(!['payer','card'].includes(stage)||document.visibilityState==='hidden')return null;
      if([...document.querySelectorAll('[role="dialog"],[role="alertdialog"],mat-dialog-container,.swal2-popup')].some(visible))return null;
      const roots=[...document.querySelectorAll(stage==='payer'?'stepper-pago alta-cliente':'stepper-pago alta-tarjeta')].filter(visible);
      if(roots.length!==1)return null;
      const root=roots[0];
      if(attempted.has(root))return null;
      const frames=[...document.querySelectorAll('iframe')].filter(visible);
      if(frames.some(e=>googleFrame(e,'bframe')))return null;
      const anchors=frames.filter(e=>googleFrame(e,'anchor'));
      if(anchors.length!==1 || !root.contains(anchors[0]))return null;
      const frame=anchors[0];
      if(new URL(frame.src).searchParams.get('size')!=='normal')return null;
      if(/^(ar|fa|he|iw|ur)(-|$)/i.test(new URL(frame.src).searchParams.get('hl')||'') || getComputedStyle(frame).direction==='rtl')return null;
      // Only absence/presence is tested locally. Never return, log or retain the response.
      if([...root.querySelectorAll('textarea[name="g-recaptcha-response"]')].some(e=>e.value.length>0))return null;
      const r=frame.getBoundingClientRect(),x=r.x+28,y=r.y+39;
      if(Math.abs(r.width-304)>2 || Math.abs(r.height-78)>2 || r.x<0 || r.y<0 || r.right>innerWidth || r.bottom>innerHeight)return null;
      if(document.elementFromPoint(x,y)!==frame)return null;
      attempted.add(root);
      return {x,y,viewport:innerWidth};
    }
  };
})();
