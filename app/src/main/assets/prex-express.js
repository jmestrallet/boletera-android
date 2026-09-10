(() => {
  const trusted=()=>location.origin==='https://pasarelaspe.sistarbanc.com.uy'&&location.pathname.startsWith('/v2/')&&window.top===window.self;
  if(!trusted()||window.BoleteraExpress)return;
  let plan=null,phase='off',lastStage='',stageSince=0;
  const visible=e=>{
    if(!e||!e.getClientRects().length||e.closest('[hidden],[aria-hidden="true"],.mat-horizontal-stepper-content[aria-expanded="false"]'))return false;
    for(let p=e;p;p=p.parentElement){const s=getComputedStyle(p);if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0')return false;}
    return true;
  };
  const single=selector=>{const rows=[...document.querySelectorAll(selector)].filter(visible);return rows.length===1?rows[0]:null;};
  const normal=value=>String(value).trim().normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
  function cents(value) {
    let text=value.trim().replace(/^(?:\$U?|UYU)\s*/,'').replace(/\s/g,'');
    if(/^\d+\.\d{1,2}$/.test(text))text=text.replace('.',',');
    if(!/^(?:\d{1,3}(?:\.\d{3})+|\d+)(?:,\d{1,2})?$/.test(text))return null;
    const [whole,decimal='']=text.replace(/\./g,'').split(',');
    const amount=Number(whole)*100+Number(decimal.padEnd(2,'0'));
    return Number.isSafeInteger(amount)?amount:null;
  }
  function stop(next='manual'){plan=null;phase=next;return phase;}
  window.BoleteraExpress={
    start(amount,payer) {
      if(phase!=='off'||!Number.isSafeInteger(amount)||amount<=0||!payer)return false;
      plan={amount,payer};phase='advancing';return true;
    },
    stop(){return stop();},
    tick() {
      if(!plan)return phase;
      if(!trusted()||!window.BoleteraNative)return stop();
      const state=window.BoleteraNative.snapshot(),stage=state.stage;
      if(stage!==lastStage){lastStage=stage;stageSince=Date.now();}
      if(stage==='card')return stop('done');
      if(stage==='original')return stop();
      if([...document.querySelectorAll('mat-error,[role="alert"],.mat-snack-bar-container,.mat-mdc-snack-bar-container')].some(visible))return stop();
      // Never replay a navigation that stalled or was rejected; offer the ordinary controls.
      if(Date.now()-stageSince>20000 && phase!=='verification')return stop();
      if(stage==='loading')return phase='advancing';
      if(stage==='summary') {
        const total=state.rows.filter(row=>normal(row[0])==='total:');
        const currency=state.rows.filter(row=>normal(row[0])==='moneda:');
        if(total.length!==1||cents(total[0][1])!==plan.amount||currency.length!==1||
          !['uyu','858','$','$u','pesos','pesos uruguayos','peso uruguayo'].includes(normal(currency[0][1])))return stop();
        const root=single('stepper-pago confirmar-pago');
        if(!root||[...root.querySelectorAll('input,select,textarea,mat-checkbox')].some(visible))return stop();
        phase='advancing';if(state.canContinue)window.BoleteraNative.advance('summary');return phase;
      }
      if(stage==='payer') {
        const root=single('stepper-pago alta-cliente'),form=root&&single('stepper-pago alta-cliente form');
        if(!root||!form)return stop();
        const expected={nombreControl:plan.payer.givenName,apellidoControl:plan.payer.familyName,
          documentoControl:plan.payer.document,emailControl:plan.payer.email,celularControl:plan.payer.phone};
        const inputs=[...form.querySelectorAll('input,select,textarea,mat-checkbox,mat-select')].filter(visible);
        if(inputs.some(e=>!Object.hasOwn(expected,e.getAttribute('formcontrolname'))&&e.getAttribute('formcontrolname')!=='tipoDocumentoControl'))return stop();
        const payerStatus=window.BoleteraPayer?.status();
        if(payerStatus==='conflict')return stop();
        if(payerStatus==='waiting')return phase='advancing';
        if(Object.entries(expected).some(([name,value])=>{
          const found=form.querySelectorAll(`input[formcontrolname="${name}"]`);
          return found.length!==1||normal(found[0].value)!==normal(value);
        }))return stop();
        const type=form.querySelector('[formcontrolname="tipoDocumentoControl"]');
        if(type) {
          const selected=normal(type.tagName==='SELECT'?type.selectedOptions[0]?.textContent||'':type.querySelector('.mat-select-value-text,.mat-mdc-select-value-text')?.textContent||'');
          const allowed=plan.payer.documentType==='PAS'?['pas','pasaporte']:['ci','cedula de identidad','cedula uruguaya'];
          if(!allowed.includes(selected))return stop();
        }
        // The user solves the original CAPTCHA. No token, iframe content or response is read.
        if(state.challenge||[...root.querySelectorAll('angular-recaptcha')].some(visible))return phase='verification';
        if(!form.classList.contains('ng-valid'))return stop();
        phase='advancing';if(state.canContinue)window.BoleteraNative.advance('payer');return phase;
      }
      return stop();
    }
  };
})();
