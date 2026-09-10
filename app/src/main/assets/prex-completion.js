/* Visible provider components only. Financial confirmation always needs an explicit tap. */
(() => {
  if (location.origin !== 'https://pasarelaspe.sistarbanc.com.uy' || !location.pathname.startsWith('/v2/') ||
      window.top !== window.self || !window.BoleteraNative || window.BoleteraCompletion) return;
  const base = window.BoleteraNative;
  const text = e => (e?.textContent || '').replace(/\s+/g,' ').trim();
  const normal = s => s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/:$/,'').trim();
  const visible = e => {
    if (!e || !e.getClientRects().length || e.closest('[hidden],[aria-hidden="true"],.mat-horizontal-stepper-content[aria-expanded="false"]')) return false;
    for (let p=e;p;p=p.parentElement) { const s=getComputedStyle(p); if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0')return false; }
    return true;
  };
  const single = selector => {const found=[...document.querySelectorAll(selector)].filter(visible);return found.length===1?found[0]:null;};
  const ready = e => !!e && !e.disabled && e.getAttribute('aria-disabled')!=='true';
  const action = (root, label) => {const found=[...root.querySelectorAll('button,a')].filter(e=>visible(e)&&normal(text(e))===label);return found.length===1?found[0]:null;};
  const labels = new Set(['comercio','cliente','transaccion','medio de pago','importe','total','numero de autorizacion','fecha de pago']);
  function rows(root) {
    return [...root.querySelectorAll('div')].filter(visible).map(div=>{
      const children=[...div.children].filter(e=>['B','P'].includes(e.tagName)&&visible(e));
      if(children.length!==2 || !labels.has(normal(text(children[0]))))return null;
      const label=text(children[0]), value=text(children[1]).slice(0,160);
      // Only an already masked card label can cross into app state; never read any input.
      if(normal(label)==='medio de pago' && !/^[A-Za-z ]+\s[*•xX]{4,}\s\d{4}$/.test(value))return null;
      return [label,value];
    }).filter(Boolean);
  }
  function cents(value) {
    let s=value.replace(/^(?:\$U?|UYU)\s*/,'').replace(/\s/g,'');
    if(/^\d+\.\d{1,2}$/.test(s))s=s.replace('.',',');
    if(!/^(?:\d{1,3}(?:\.\d{3})+|\d+)(?:,\d{1,2})?$/.test(s))return null;
    const [whole,decimal='']=s.replace(/\./g,'').split(',');
    const n=Number(whole)*100+Number(decimal.padEnd(2,'0'));return Number.isSafeInteger(n)?n:null;
  }
  let expected=null, finalSent=false, returnSent=false;
  const value = (data,key) => {const found=data.filter(r=>normal(r[0])===key);return found.length===1?found[0][1]:null;};
  function snapshot() {
    const final=single('stepper-pago finalizar-pago'), receipt=single('resultado-pago');
    if(!final&&!receipt)return base.snapshot();
    if(final&&receipt || [...document.querySelectorAll('[role="dialog"],[role="alertdialog"],mat-dialog-container,.swal2-popup,[role="alert"],.mat-snack-bar-container')].some(visible))return {stage:'original'};
    const root=final||receipt, data=rows(root);
    const total=value(data,receipt?'total':'importe');
    if(!Number.isSafeInteger(expected)||expected<=0||normal(value(data,'comercio')||'')!=='stm recargas'||cents(total||'')!==expected)return {stage:'original'};
    if(final) {
      // Keep unhandled choices and verification challenges in the original interactive page.
      if([...root.querySelectorAll('input:not([type="hidden"]),select,textarea,mat-select')].some(visible) ||
        [...document.querySelectorAll('iframe')].some(e=>visible(e)&&/\/recaptcha\/(?:api2|enterprise)\/(?:anchor|bframe)/.test(e.src)&&(/\/bframe/.test(e.src)||!/[?&]size=invisible(?:&|$)/.test(e.src))))return {stage:'original'};
      if(!value(data,'transaccion')||!value(data,'medio de pago'))return {stage:'original'};
      return {stage:'finalConfirmation',rows:data,canContinue:!finalSent&&ready(action(root,'confirmar')),submitted:finalSent};
    }
    const statuses=[...receipt.querySelectorAll('b')].filter(visible).map(e=>normal(text(e)));
    const success=statuses.filter(s=>s==='el pago se realizo con exito').length===1;
    const rejected=statuses.some(s=>s==='la transaccion fue rechazada');
    const pending=statuses.find(s=>s.startsWith('el pago se encuentra en estado:'));
    if(Number(success)+Number(rejected)+Number(!!pending)!==1 || success&&(!value(data,'numero de autorizacion')||!value(data,'transaccion')))return {stage:'original'};
    return {stage:success?'receipt':rejected?'paymentRejected':'paymentPending',rows:data,
      completionNotice:pending?pending.slice(0,160):'',canContinue:!returnSent&&ready(action(root,'volver a la pagina inicial')),submitted:returnSent};
  }
  window.BoleteraCompletion={configure(amount,submitted=false){expected=amount;finalSent=finalSent||submitted;}};
  window.BoleteraNative={...base,snapshot,advance(stage){
    if(!['finalConfirmation','receipt','paymentRejected','paymentPending'].includes(stage))return base.advance(stage);
    const state=snapshot();if(state.stage!==stage||!state.canContinue)return false;
    const root=single(stage==='finalConfirmation'?'stepper-pago finalizar-pago':'resultado-pago');
    const button=root&&action(root,stage==='finalConfirmation'?'confirmar':'volver a la pagina inicial');
    if(!ready(button))return false;
    if(stage==='finalConfirmation')finalSent=true;else returnSent=true;
    button.click();return true;
  }};
})();
