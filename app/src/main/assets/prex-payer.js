(() => {
  if (location.origin !== 'https://pasarelaspe.sistarbanc.com.uy' || !location.pathname.startsWith('/v2/') || window.top !== window.self) return;
  if (window.BoleteraPayer) return;
  let profile = null;
  let state = 'waiting';
  const attempted = new WeakSet();
  const typeAttempts = new WeakSet();
  let pendingType = null;
  const normalize = value => value.trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  function typeCode(text) {
    const label = normalize(text);
    if (['ci', 'cedula de identidad', 'cedula uruguaya'].includes(label)) return 'CI';
    if (['pas', 'pasaporte'].includes(label)) return 'PAS';
    return null;
  }
  function selectedType(type) {
    if (type.tagName === 'SELECT') return typeCode(type.selectedOptions[0]?.textContent || '') || type.value || null;
    return typeCode(type.querySelector('.mat-select-value-text, .mat-mdc-select-value-text')?.textContent || '');
  }
  function chooseDocumentType(type, desired, force) {
    if (!type) return true; // Some provider steps have no document-type control.
    // The stepper creates later forms while they are still hidden. Opening a
    // hidden Material select does not work; wait until its step is displayed.
    if (!type.getClientRects().length || getComputedStyle(type).visibility === 'hidden' ||
        type.closest('[aria-hidden="true"], .mat-horizontal-stepper-content[aria-expanded="false"]')) return false;
    if (typeAttempts.has(type) && !force && pendingType !== type) return true;
    const selected = selectedType(type);
    if (selected && selected !== desired && !force) { state = 'conflict'; return false; }
    if (selected === desired) { typeAttempts.add(type); pendingType = null; return true; }
    if (type.disabled || type.getAttribute('aria-disabled') === 'true') return false;
    if (type.tagName === 'SELECT') {
      const options = [...type.options].filter(option => (option.value === desired || typeCode(option.textContent) === desired) && !option.disabled);
      if (options.length !== 1) return false;
      typeAttempts.add(type);
      Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype, 'value').set.call(type, options[0].value);
      type.dispatchEvent(new Event('input', {bubbles:true}));
      type.dispatchEvent(new Event('change', {bubbles:true}));
      return true;
    }
    if (type.tagName !== 'MAT-SELECT') return false;
    if (pendingType !== type) {
      const trigger = type.querySelector('.mat-select-trigger, .mat-mdc-select-trigger');
      if (!trigger) return false;
      pendingType = type;
      typeAttempts.add(type);
      // Use the original select and its options so Angular receives the selection.
      // A form can appear before Angular attaches its click handlers. Wait until
      // the current render completes, then open the original dropdown once.
      setTimeout(() => {
        if (pendingType !== type || !type.isConnected) return;
        if (type.getAttribute('aria-expanded') !== 'true') trigger.click();
        fill();
      }, 0);
      return false;
    }
    const ownedIds = (type.getAttribute('aria-controls') || type.getAttribute('aria-owns') || '').split(/\s+/).filter(Boolean);
    const panels = ownedIds.map(id => document.getElementById(id)).filter(panel => panel?.getAttribute('role') === 'listbox');
    if (panels.length !== 1) return false;
    const options = [...panels[0].querySelectorAll('mat-option[role="option"]')].filter(option =>
      option.getAttribute('aria-disabled') !== 'true' && typeCode(option.textContent) === desired);
    if (options.length !== 1) return false;
    // Clear before the click; option events may synchronously update the document.
    pendingType = null;
    options[0].click();
    return selectedType(type) === desired;
  }
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
    const desiredType = profile.documentType || 'CI';
    if (!['CI', 'PAS'].includes(desiredType)) { state = 'waiting'; return; }
    if (type && !typeAttempts.has(type) && selectedType(type) && selectedType(type) !== desiredType && !force) { state = 'conflict'; return; }
    const values = controls(form, split) ? {
      nombreControl: profile.givenName, apellidoControl: profile.familyName,
      documentoControl: profile.document, emailControl: profile.email, celularControl: profile.phone
    } : {
      nombreControl: `${profile.givenName} ${profile.familyName}`.trim(),
      documentoControl: profile.document, correoControl: profile.email
    };
    if (!force && Object.entries(values).some(([name, value]) => {
      const input = form.querySelector(`input[formcontrolname="${name}"]`);
      return !attempted.has(input) && input.value && normalize(input.value) !== normalize(value);
    })) { state = 'conflict'; return; }
    if (!chooseDocumentType(type, desiredType, force)) { if (state !== 'conflict') state = 'waiting'; return; }
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
