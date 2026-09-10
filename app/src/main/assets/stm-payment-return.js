/* Runs after the original provider return. Does not submit amounts or infer credit. */
(() => {
  if(window.top!==window.self||window.BoleteraNative)return;
  const stmPage=location.origin==='https://stm.gub.uy'&&location.pathname.startsWith('/app/mistm/cuenta/');
  const loginPage=['https://mi.iduruguay.gub.uy','https://auth.iduruguay.gub.uy'].includes(location.origin)&&location.pathname==='/login';
  if(!stmPage&&!loginPage)return;
  const text=e=>(e?.textContent||'').replace(/\s+/g,' ').trim();
  const visible=e=>{
    if(!e||!e.getClientRects().length||e.closest('[hidden],[aria-hidden="true"]'))return false;
    for(let p=e;p;p=p.parentElement){const s=getComputedStyle(p);if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0')return false;}
    return true;
  };
  const exact=re=>[...document.querySelectorAll('h1,h2,h3,h4,p,label,span,div')].some(e=>visible(e)&&re.test(text(e)));
  const actions=()=>[...document.querySelectorAll('button,a,[role="button"]')].filter(visible);
  const next=()=>{const found=actions().filter(e=>/^CONTINUAR$/i.test(text(e)));return found.length===1?found[0]:null;};
  let sent=false;
  function snapshot(){
    if(['expired','login'].includes(window.BoleteraSession?.snapshot()))return {stage:'sessionExpired'};
    if(!stmPage)return {stage:'original'};
    if([...document.querySelectorAll('[role="alert"],[role="dialog"],.ui-messages-error')].some(visible))return {stage:'original'};
    if(/\/(?:recarga1|recarga2)\.xhtml$/.test(location.pathname))return {stage:'original'};
    if(location.pathname.endsWith('/principal.xhtml') && /Saldo disponible\*?\s*:\s*\$\s*-?\s*\d[\d.,]*/i.test([...document.querySelectorAll('p,label,span,div')].filter(visible).map(text).join(' ')) &&
       actions().some(e=>/^Recargar$/i.test(text(e))||e.id.endsWith(':btnRecargar')))return {stage:'returnBalance'};
    if(exact(/^¡Recarga exitosa!$/i)&&exact(/^Recarga Confirmada\.$/i)) {
      const button=next();return {stage:'stmSuccess',canContinue:!sent&&!!button&&!button.disabled&&button.getAttribute('aria-disabled')!=='true',submitted:sent};
    }
    return {stage:'original'};
  }
  window.BoleteraNative={snapshot,advance(stage){const state=snapshot();if(stage!=='stmSuccess'||state.stage!==stage||!state.canContinue)return false;sent=true;next().click();return true;}};
})();
