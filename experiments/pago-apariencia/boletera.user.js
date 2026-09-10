// ==UserScript==
// @name Boletera · apariencia de pago (experimental)
// @namespace boletera-local
// @version 0.0.1
// @description Apariencia y datos del titular. Sin envío automático ni integración de pago.
// @match https://pasarelaspe.sistarbanc.com.uy/v2/*
// @run-at document-idle
// @noframes
// @inject-into content
// @grant GM_getValue
// @grant GM_setValue
// @grant GM_deleteValue
// ==/UserScript==

(() => {
  'use strict';
  if (location.origin !== 'https://pasarelaspe.sistarbanc.com.uy' ||
      !location.pathname.startsWith('/v2/') || window.top !== window.self ||
      document.getElementById('boletera-local-tools')) return;

  const fields = [
    ['nombreControl', 'Nombre', 'text'],
    ['apellidoControl', 'Apellido', 'text'],
    ['documentoControl', 'Documento', 'text'],
    ['emailControl', 'Email', 'email'],
    ['celularControl', 'Celular', 'tel'],
  ];
  const key = 'titular-v1';
  let profile = GM_getValue(key, null);
  const filled = new WeakSet();
  const node = (tag, text) => {
    const item = document.createElement(tag);
    if (text) item.textContent = text;
    return item;
  };
  const style = node('style');
  style.textContent = `
    #boletera-local-tools {box-sizing:border-box;margin:12px;padding:18px;border-radius:20px;background:#101d2a;color:#fff;font:15px/1.5 system-ui,sans-serif}
    #boletera-local-tools * {box-sizing:border-box}
    #boletera-local-tools h2 {margin:0;font:700 26px system-ui,sans-serif;color:#fff}
    #boletera-local-tools p {margin:8px 0;color:#e2e7e5}
    #boletera-local-tools button {border:0;border-radius:12px;background:#b9f375;color:#101d2a;padding:12px;margin:6px 6px 0 0;font:600 15px system-ui,sans-serif;cursor:pointer}
    #boletera-local-tools label {display:block;margin:10px 0;color:#fff}
    #boletera-local-tools input {display:block;width:100%;padding:10px;border:1px solid #b4c0bc;border-radius:8px;background:#fff;color:#101d2a;font:16px system-ui,sans-serif}
    #boletera-local-tools [hidden] {display:none!important}
    body.boletera-local-theme {background:#f4f5ef!important}
    .boletera-local-theme form.boletera-titular {padding:18px;border-radius:20px;background:#fff;color:#101d2a}
    .boletera-local-theme form.boletera-titular mat-card {box-shadow:none;border-radius:16px}
    .boletera-local-theme form.boletera-titular mat-form-field {font-family:system-ui,sans-serif}
  `;
  document.head.append(style);
  const panel = node('section');
  panel.id = 'boletera-local-tools';
  panel.append(node('h2', 'boletera'), node('p', 'Vista experimental sobre Sistarbanc. El pago sigue en la página original.'));
  const status = node('p');
  status.setAttribute('role', 'status');
  panel.append(status);
  const button = (label, action) => {
    const item = node('button', label);
    item.type = 'button';
    item.addEventListener('click', action);
    return item;
  };
  const editor = node('div');
  editor.hidden = true;
  editor.append(node('p', 'Guardá una vez tus datos del titular en este navegador. No se guardan tarjeta, vencimiento ni código de seguridad.'));
  const editors = new Map();
  for (const [name, label, type] of fields) {
    const item = node('label', label);
    const input = node('input');
    input.type = type;
    input.maxLength = 120;
    input.autocomplete = 'off';
    input.dataset.field = name;
    item.append(input);
    editor.append(item);
    editors.set(name, input);
  }
  const findForm = () => {
    const matches = [...document.querySelectorAll('form')].filter(form => fields.every(([name]) => {
      const inputs = form.querySelectorAll(`input[formcontrolname="${name}"]`);
      return inputs.length === 1 && !inputs[0].disabled && !inputs[0].readOnly &&
        ['text', 'email', 'tel', 'number'].includes(inputs[0].type);
    }));
    return matches.length === 1 ? matches[0] : null;
  };
  function refresh() {
    if (!window.document?.body) return;
    const form = findForm();
    const message = form ? (profile ? 'Datos guardados. Revisá el formulario y seguí con la verificación original.' : 'Podés guardar tus datos del titular para completar este formulario.') : 'Esperando un formulario de titular compatible. No se completan otros formularios.';
    if (status.textContent !== message) status.textContent = message;
    if (!form) return;
    form.classList.add('boletera-titular');
    if (!profile || typeof profile !== 'object') return;
    for (const [name] of fields) {
      const input = form.querySelector(`input[formcontrolname="${name}"]`);
      if (filled.has(input)) continue;
      filled.add(input);
      if (input.value || typeof profile[name] !== 'string' || !profile[name]) continue;
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, profile[name]);
      input.dispatchEvent(new Event('input', { bubbles: true }));
      input.dispatchEvent(new Event('change', { bubbles: true }));
    }
  }
  editor.append(button('Guardar y completar', () => {
    if ([...editors.values()].some(input => !input.reportValidity())) return;
    profile = Object.fromEntries([...editors].map(([name, input]) => [name, input.value.trim()]));
    GM_setValue(key, profile);
    editor.hidden = true;
    refresh();
  }), button('Cerrar', () => { editor.hidden = true; }));
  panel.append(button('Mis datos', () => {
    for (const [name, input] of editors) input.value = typeof profile?.[name] === 'string' ? profile[name] : '';
    editor.hidden = !editor.hidden;
  }), button('Vista original', event => {
    const active = document.body.classList.toggle('boletera-local-theme');
    event.currentTarget.textContent = active ? 'Vista original' : 'Vista Boletera';
  }), button('Borrar datos guardados', () => {
    GM_deleteValue(key);
    profile = null;
    for (const input of editors.values()) input.value = '';
    refresh();
    status.textContent = 'Datos guardados borrados. Los campos ya completados en la página se conservan.';
  }), editor);
  document.body.prepend(panel);
  document.body.classList.add('boletera-local-theme');
  refresh();
  new MutationObserver(refresh).observe(document.body, { childList: true, subtree: true });
})();
