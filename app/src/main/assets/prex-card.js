(() => {
  const trusted = () => location.origin === 'https://pasarelaspe.sistarbanc.com.uy' && location.pathname.startsWith('/v2/') && window.top === window.self;
  if (!trusted() || window.BoleteraCard) return;
  const names = ['nroTarjetaControl', 'expiracionControl', 'cvvControl'];
  const visible = e => {
    if (!e || !e.getClientRects().length || e.closest('[aria-hidden="true"],.mat-horizontal-stepper-content[aria-expanded="false"]')) return false;
    for (let p=e;p;p=p.parentElement) {const s=getComputedStyle(p);if(s.display==='none'||s.visibility==='hidden'||s.opacity==='0')return false;}
    return true;
  };
  let owner = null, state = 'idle', errorSince = null;
  const fields = root => names.map(name => root.querySelector(`input[formcontrolname="${name}"]`));
  function root() {
    if (!trusted() || [...document.querySelectorAll('[role="dialog"],[role="alertdialog"],.swal2-popup,mat-dialog-container')].some(visible)) return null;
    const roots=[...document.querySelectorAll('stepper-pago alta-tarjeta')].filter(visible);
    if(roots.length!==1)return null;
    const r=roots[0];
    // Unknown controls/consents remain in the original page, never silently accepted or hidden.
    const inputs=[...r.querySelectorAll('input,select,textarea,mat-checkbox')].filter(visible);
    if(inputs.length!==3 || names.some(name=>inputs.filter(e=>e.tagName==='INPUT'&&e.getAttribute('formcontrolname')===name).length!==1))return null;
    if([...r.querySelectorAll('button')].filter(e=>visible(e)&&e.textContent.trim().toUpperCase()==='CONTINUAR').length!==1)return null;
    return r;
  }
  const button = r => [...r.querySelectorAll('button')].find(e=>visible(e)&&e.textContent.trim().toUpperCase()==='CONTINUAR');
  const ready = r => {const b=button(r);return !!b&&!b.disabled&&b.getAttribute('aria-disabled')!=='true';};
  const providerError = () => [...document.querySelectorAll('mat-error,.mat-snack-bar-container,.mat-mdc-snack-bar-container,[role="alert"]')].some(visible);
  function snapshot() {
    const r=root();
    if(!r)return {available:false};
    if(r!==owner){owner=r;state='idle';errorSince=null;}
    if(state==='submitted') {
      if(ready(r) && providerError()) {
        if(errorSince===null)errorSince=Date.now();
        if(Date.now()-errorSince>=2000)state='rejected';
      } else errorSince=null;
    }
    return {available:true,busy:state==='preparing'||state==='submitted',error:state==='rejected',canContinue:ready(r)&&state!=='preparing'&&state!=='submitted'};
  }
  function valid(pan, expiry, cvv) {
    if(!/^\d{13,16}$/.test(pan)||!/^(0[1-9]|1[0-2])\/\d{2}$/.test(expiry)||!/^\d{3,4}$/.test(cvv))return false;
    let sum=0;
    for(let i=pan.length-1,j=0;i>=0;i--,j++){let n=Number(pan[i]);if(j%2)n=n*2>9?n*2-9:n*2;sum+=n;}
    const now=new Date(),year=2000+Number(expiry.slice(3)),month=Number(expiry.slice(0,2));
    return sum%10===0&&(year>now.getFullYear()||year===now.getFullYear()&&month>=now.getMonth()+1);
  }
  window.BoleteraCard = {
    snapshot,
    submit(pan, expiry, cvv) {
      const current=snapshot(),r=root();
      if(!r||!current.canContinue||!valid(pan,expiry,cvv))return false;
      const inputs=fields(r),action=button(r);
      if(inputs.some(e=>e.disabled||e.readOnly))return false;
      state='preparing';errorSince=null;
      const values=[pan.match(/.{1,4}/g).join(' '),expiry,cvv];
      const setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;
      inputs.forEach((input,i)=>{
        setter.call(input,values[i]);input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));input.dispatchEvent(new Event('blur',{bubbles:true}));
      });
      values.fill('');pan=expiry=cvv='';
      // Let Angular settle its existing validators. No HTTP client, token access or alternate payment API.
      setTimeout(()=>{
        if(root()!==r || owner!==r || state!=='preparing')return;
        if(!ready(r)||inputs.some(e=>!e.checkValidity()||e.classList.contains('ng-invalid'))||r.querySelector('form.ng-invalid')) {state='rejected';return;}
        state='submitted';
        action.click(); // Exactly one original action per explicit native Continue.
      },0);
      return true;
    }
  };
})();
