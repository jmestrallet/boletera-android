(() => {
  if (location.origin !== 'https://pasarelaspe.sistarbanc.com.uy' || !location.pathname.startsWith('/v2/') || window.top !== window.self) return;
  if (window.BoleteraPayer) return;
  let profile = null;
  let state = 'waiting';
  const attempted = new WeakSet();
  const split = ['nombreControl', 'apellidoControl', 'documentoControl', 'emailControl', 'celularControl'];
  const combined = ['nombreControl', 'documentoControl', 'correoControl', 'nroTarjetaControl', 'expiracionControl', 'cvvControl'];
  function controls(form, names) {
    return names.every(name => form.querySelectorAll(`input[formcontrolname="${name}"]`).length === 1);
  }
  function fill(force = false) {
    if (!profile || !window.document?.body) return;
    const forms = [...document.querySelectorAll('form')].filter(form => controls(form, split) || controls(form, combined));
    if (forms.length !== 1) { state = 'waiting'; return; }
    const form = forms[0];
    const type = form.querySelector('[formcontrolname="tipoDocumentoControl"]');
    // This profile is explicitly a Uruguayan identity document; never fill it as a foreign document.
    if (type && ((type.tagName === 'SELECT' && type.value && type.value !== 'CI') || /extranjero/i.test(type.textContent))) { state = 'foreign-document'; return; }
    const values = controls(form, split) ? {
      nombreControl: profile.givenName, apellidoControl: profile.familyName,
      documentoControl: profile.document, emailControl: profile.email, celularControl: profile.phone
    } : {
      nombreControl: `${profile.givenName} ${profile.familyName}`.trim(),
      documentoControl: profile.document, correoControl: profile.email
    };
    const normalize = value => value.trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    if (!force && Object.entries(values).some(([name, value]) => {
      const input = form.querySelector(`input[formcontrolname="${name}"]`);
      return !attempted.has(input) && input.value && normalize(input.value) !== normalize(value);
    })) { state = 'conflict'; return; }
    for (const [name, value] of Object.entries(values)) {
      const input = form.querySelector(`input[formcontrolname="${name}"]`);
      if ((!force && attempted.has(input)) || input.disabled || input.readOnly || !['text','email','tel','number'].includes(input.type)) continue;
      attempted.add(input);
      if ((!force && input.value) || typeof value !== 'string' || !value) continue;
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, value);
      input.dispatchEvent(new Event('input', {bubbles:true}));
      input.dispatchEvent(new Event('change', {bubbles:true}));
    }
    state = 'filled';
  }
  window.BoleteraPayer = {
    use(value) {
      if (profile || !value || typeof value.id !== 'string') return false;
      profile = value;
      fill();
      return true;
    },
    status() { fill(); return state; },
    applyChosenProfile() { fill(true); return state === 'filled'; }
  };
  new MutationObserver(() => fill()).observe(document.body, {childList:true, subtree:true});
})();
