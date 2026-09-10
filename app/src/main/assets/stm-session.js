/* Read-only session signals. Never reads credentials, cookies or tokens. */
(() => {
  if(window.BoleteraSession || window.top!==window.self)return;
  const trusted=()=>location.protocol==='https:' && (!location.port||location.port==='443') &&
    ['stm.gub.uy','mi.iduruguay.gub.uy','auth.iduruguay.gub.uy','ih.montevideo.gub.uy'].includes(location.hostname);
  const text=e=>(e?.textContent||'').replace(/\s+/g,' ').trim();
  const visible=e=>{
    if(!e||!e.getBoundingClientRect||e.closest('[hidden],[aria-hidden="true"]'))return false;
    const r=e.getBoundingClientRect();if(r.width<1||r.height<1)return false;
    for(let p=e;p;p=p.parentElement){const s=getComputedStyle(p);if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0')return false;}
    return true;
  };
  window.BoleteraSession={snapshot(){
    if(!trusted()||document.readyState!=='complete')return 'none';
    const inStm=location.hostname==='stm.gub.uy'&&location.pathname.startsWith('/app/mistm/cuenta/');
    if(inStm) {
      const expired=[...document.querySelectorAll('[role="alert"],.ui-messages-error,.ui-message-error,h1,h2,h3,p,span,div')].filter(visible).some(e=>{
        const s=text(e);if(s.length>500)return false;
        return /(?:sesi[oó]n\s+(?:(?:ha|se ha)\s+)?(?:expirada|expirado|vencida|vencido|caducada|caducado|finalizada|finalizado|expir[oó]|venci[oó]|caduc[oó])|session\s+(?:has\s+)?expired|(?:javax|jakarta)\.faces\.application\.ViewExpiredException)/i.test(s);
      });
      if(expired)return 'expired';
      if([...document.querySelectorAll('button,a,[role="button"]')].some(e=>visible(e)&&/INGRESAR CON USUARIO GUB\.UY/i.test(text(e))))return 'login';
    }
    if(['mi.iduruguay.gub.uy','auth.iduruguay.gub.uy'].includes(location.hostname)&&location.pathname==='/login' &&
       [...document.querySelectorAll('input')].some(e=>visible(e)&&/password|documento|document|dni/i.test([e.type,e.id,e.name].join(' '))))return 'login';
    return 'none';
  }};
})();
