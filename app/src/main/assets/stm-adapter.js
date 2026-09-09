/* Runs in the ORIGINAL top-level page. Never returns input values, tokens or HTML.
   Deliberately no CAPTCHA clicks and no payment submission command. */
(() => {
  'use strict';
  if (window.BoleteraAdapter) return;
  const trusted = () => location.protocol === 'https:' &&
    ['stm.gub.uy', 'mi.iduruguay.gub.uy', 'auth.iduruguay.gub.uy'].includes(location.hostname);
  const text = el => (el?.textContent || '').replace(/\s+/g, ' ').trim();
  const visible = el => {
    if (!el || !el.getBoundingClientRect) return false;
    const r = el.getBoundingClientRect();
    if (r.width < 1 || r.height < 1) return false;
    for (let p = el; p; p = p.parentElement) {
      const s = getComputedStyle(p);
      if (s.display === 'none' || s.visibility === 'hidden' || s.opacity === '0') return false;
    }
    return true;
  };
  const buttons = () => [...document.querySelectorAll('button, a, [role="button"]')].filter(visible);
  const button = re => buttons().find(el => re.test(text(el) || el.getAttribute('aria-label') || ''));
  const input = re => [...document.querySelectorAll('input')].filter(visible).find(el =>
    re.test([el.id, el.name, el.type, el.placeholder, el.getAttribute('aria-label')].join(' ')));
  const money = s => {
    const m = String(s).match(/(-?\s*\$?\s*-?\s*\d[\d.,]*)/);
    if (!m) return null;
    let raw = m[1].replace(/[\s$]/g, '');
    if (raw.includes(',')) raw = raw.replace(/\./g, '').replace(',', '.');
    else if (/^-?\d{1,3}(\.\d{3})+$/.test(raw)) raw = raw.replace(/\./g, '');
    const value = Number(raw);
    return Number.isFinite(value) ? Math.round(value * 100) : null;
  };
  const captcha = () => {
    const frames = [...document.querySelectorAll('iframe')].filter(el => {
      try {
        const u = new URL(el.src);
        return ['www.google.com', 'www.recaptcha.net', 'www.gstatic.com'].includes(u.hostname) &&
          /\/recaptcha\//.test(u.pathname) && u.searchParams.get('size') !== 'invisible' && visible(el);
      } catch { return false; }
    });
    const frame = frames.find(el => /\/bframe/.test(el.src)) || frames.find(el => /\/anchor/.test(el.src));
    if (!frame) return null;
    const r = frame.getBoundingClientRect();
    // The original iframe is cropped by the native parent, never moved into another document.
    return { x: Math.max(0, r.left), y: Math.max(0, r.top), width: r.width, height: r.height,
      expanded: /\/bframe/.test(frame.src) };
  };
  const rows = () => [...document.querySelectorAll('tbody tr, [role="row"]')].filter(visible)
    .map((el, index) => {
      const s = text(el), id = s.match(/\b[A-F0-9]{8}\b/i)?.[0];
      const active = /\bOperativa\b/i.test(s) && !/no operativa/i.test(s);
      return id ? { index, id, status: active ? 'Operativa' : 'No operativa', active } : null;
    }).filter(Boolean);
  function snapshot() {
    if (!trusted()) return { stage: 'blocked' };
    const path = location.pathname;
    const cap = captcha();
    const error = [...document.querySelectorAll('[role="alert"], .ui-messages-error, .ui-message-error')]
      .some(el => visible(el) && /incorrect|inv[aá]lid|error|bloquead|no coincide|no pud|fall[oó]|intenta de nuevo|intent[aá] nuevamente/i.test(text(el)));
    const base = { captcha: cap, error };
    if (location.hostname !== 'stm.gub.uy') {
      if (path !== '/login') return { ...base, stage: 'unknown' };
      if (input(/password|contrase/i)) return { ...base, stage: 'password' };
      if (input(/documento|document|dni/i)) return { ...base, stage: 'document' };
      if (button(/^Usuario Gub\.uy(?:$|\s|Realiza)/i)) return { ...base, stage: 'identity' };
      return { ...base, stage: cap ? 'verification' : 'unknown' };
    }
    if (path.endsWith('tarjetas.xhtml')) return { ...base, stage: 'cards', cards: rows() };
    if (path.endsWith('principal.xhtml')) {
      const s = text(document.body);
      const match = s.match(/Saldo disponible\*?\s*:\s*(-?\s*\$?\s*-?\s*\d[\d.,]*)/i);
      return { ...base, stage: 'balance', balance: match ? money(match[1]) : null };
    }
    if (path.endsWith('recarga1.xhtml')) {
      const s = text(document.body), match = s.match(/recarga m[ií]nima deber[aá] ser de\s*([^\n]+?)(?=Saldo actual|$)/i);
      // Only this read-only balance field is read. Never inspect credential or payment values.
      const balanceField = document.getElementById('recarga1:saldoActual');
      return { ...base, stage: 'amount', minimum: match ? money(match[1]) : null,
        balance: balanceField ? money(balanceField.value) : null };
    }
    if (path.endsWith('recarga2.xhtml')) return { ...base, stage: 'paymentBoundary' };
    if (button(/INGRESAR CON USUARIO GUB\.UY/i)) return { ...base, stage: 'start' };
    if (path.endsWith('logout.xhtml')) return { ...base, stage: 'signedOut' };
    return { ...base, stage: 'unknown' };
  }
  function setValue(el, value) {
    if (!el) return false;
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
    setter.call(el, value);
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));
    return true;
  }
  function click(el) { if (!el || el.disabled || !visible(el)) return false; el.click(); return true; }
  function command(action, value) {
    if (!trusted()) return false;
    const state = snapshot();
    // A CAPTCHA must be handled by the user, not by commands or retries.
    if (action === 'start' && state.stage === 'start') return click(button(/INGRESAR CON USUARIO GUB\.UY/i));
    if (action === 'identity' && state.stage === 'identity') return click(button(/^Usuario Gub\.uy(?:$|\s|Realiza)/i));
    if (action === 'document' && state.stage === 'document') {
      return setValue(input(/documento|document|dni/i), value) && click(button(/^Continuar$/i));
    }
    if (action === 'password' && state.stage === 'password') {
      return setValue(input(/password|contrase/i), value) && click(button(/^Continuar$/i));
    }
    if (action === 'card' && state.stage === 'cards') {
      const card = state.cards.find(c => c.id === value && c.active);
      if (!card) return false;
      return click([...document.querySelectorAll('tbody tr, [role="row"]')].filter(visible)[card.index]);
    }
    if (action === 'minimum' && state.stage === 'balance') return click(button(/^Recargar$/i));
    if (action === 'amount' && state.stage === 'amount') {
      const cents = Number(value);
      if (!Number.isSafeInteger(cents) || state.minimum === null || cents < state.minimum || cents <= 0) return false;
      const field = document.getElementById('recarga1:monto_input');
      return setValue(field, (cents / 100).toFixed(2).replace('.', ',')) && click(button(/^CONTINUAR$/i));
    }
    return false;
  }
  window.BoleteraAdapter = Object.freeze({ snapshot, command, money });
})();
