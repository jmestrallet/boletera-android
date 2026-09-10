(() => {
  if (location.origin !== 'https://pasarelaspe.sistarbanc.com.uy' || !location.pathname.startsWith('/v2/') || window.top !== window.self || window.BoleteraNative) return;
  const visible = e => {
    if (!e || !e.getClientRects().length || e.closest('[aria-hidden="true"],.mat-horizontal-stepper-content[aria-expanded="false"]')) return false;
    for (let p=e; p; p=p.parentElement) {const s=getComputedStyle(p); if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0') return false;}
    return true;
  };
  const challengeFrames = () => [...document.querySelectorAll('iframe')].filter(e => {
    try {const u=new URL(e.src);return ['www.google.com','www.recaptcha.net','www.gstatic.com'].includes(u.hostname) && /\/recaptcha\//.test(u.pathname) && u.searchParams.get('size') !== 'invisible' && visible(e);} catch {return false;}
  });
  const single = selector => { const found = [...document.querySelectorAll(selector)].filter(visible); return found.length === 1 ? found[0] : null; };
  const button = root => {
    const found = [...root.querySelectorAll('button')].filter(e => visible(e) && e.textContent.trim().toUpperCase() === 'CONTINUAR');
    return found.length === 1 ? found[0] : null;
  };
  const ready = e => !!e && !e.disabled && e.getAttribute('aria-disabled') !== 'true';
  let sentStep = null;
  function snapshot() {
    if ([...document.querySelectorAll('[role="dialog"],[role="alertdialog"],.swal2-popup,mat-dialog-container')].some(visible)) return {stage:'original'};
    const summary = single('stepper-pago confirmar-pago');
    const client = single('stepper-pago alta-cliente');
    const cardState = window.BoleteraCard?.snapshot();
    const card = cardState?.available ? single('stepper-pago alta-tarjeta') : null;
    const root = summary || client || card;
    const original = single('alta-tarjeta,alta-tarjeta-externo,seleccion-tarjeta,finalizar-pago,resultado-pago,pago-redes');
    const stage = summary ? 'summary' : client ? 'payer' : card ? 'card' : original ? 'original' : 'loading';
    if (sentStep && sentStep !== stage) sentStep = null;
    if (!root) return {stage};
    // Only summary text and explicitly named ordinary payer fields are read.
    const rows = summary ? [...summary.querySelectorAll('div')].map(div => {
      const label = div.querySelector(':scope > b');
      const value = div.querySelector(':scope > p');
      return label && value ? [label.textContent.trim().slice(0,200), value.textContent.trim().slice(0,200)] : [];
    }).filter(row => row.length === 2) : [];
    const names = ['nombreControl','apellidoControl','documentoControl','emailControl','celularControl'];
    const values = client ? names.map(name => client.querySelector(`input[formcontrolname="${name}"]`)?.value || '') : [];
    const frames = client || card ? challengeFrames() : [];
    const challenge = frames.find(e => /\/bframe/.test(e.src)) || frames.find(e => /\/anchor/.test(e.src));
    const expanded = !!challenge && /\/bframe/.test(challenge.src);
    const r = challenge?.getBoundingClientRect();
    const measurable = r && r.width > 0 && r.height > 0;
    return {stage, rows, values, submitted: sentStep === stage, canContinue: card ? cardState.canContinue : ready(button(root)) && sentStep !== stage,
      cardBusy:!!card && cardState.busy, cardError:!!card && cardState.error,
      challenge: measurable ? {x:r.x,y:r.y,width:r.width,height:r.height} : null, viewportWidth:innerWidth, expanded:!!measurable && expanded};
  }
  window.BoleteraNative = {
    snapshot,
    showVerification() {
      const frames = challengeFrames();
      window.BoleteraVerification?.present(frames.find(e => /\/anchor/.test(e.src)));
      const frame = frames.find(e => /\/bframe/.test(e.src)) || frames.find(e => /\/anchor/.test(e.src));
      frame?.scrollIntoView({block:'center',inline:'center'});
    },
    restoreVerification() { window.BoleteraVerification?.restore(); },
    advance(expected) {
      const state = snapshot();
      if (!['summary','payer'].includes(expected) || state.stage !== expected || !state.canContinue) return false;
      const root = single(expected === 'summary' ? 'stepper-pago confirmar-pago' : 'stepper-pago alta-cliente');
      const action = root && button(root);
      if (!ready(action)) return false;
      sentStep = expected;
      action.click();
      return true;
    }
  };
})();
