/* Runs in the ORIGINAL top-level page. Never returns input values, tokens or HTML.
   No CAPTCHA automation or bank/card credentials. Bank authorization stays in Chrome. */
(() => {
  'use strict';
  if (window.BoleteraAdapter) return;
  const trusted = () => location.protocol === 'https:' &&
    ['stm.gub.uy', 'mi.iduruguay.gub.uy', 'auth.iduruguay.gub.uy', 'ih.montevideo.gub.uy'].includes(location.hostname);
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
  const formButton = (field, re) => {
    const form = field?.closest('form');
    return form ? [...form.querySelectorAll('button, a, [role="button"]')].filter(visible)
      .find(el => re.test(text(el) || el.getAttribute('aria-label') || '')) : button(re);
  };
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
  const captchaFrame = () => {
    const frames = [...document.querySelectorAll('iframe')].filter(el => {
      try {
        const u = new URL(el.src);
        return ['www.google.com', 'www.recaptcha.net', 'www.gstatic.com'].includes(u.hostname) &&
          /\/recaptcha\//.test(u.pathname) && u.searchParams.get('size') !== 'invisible' && visible(el);
      } catch { return false; }
    });
    return frames.find(el => /\/bframe/.test(el.src)) || frames.find(el => /\/anchor/.test(el.src));
  };
  const captcha = () => {
    const frame = captchaFrame();
    if (!frame) return null;
    const r = frame.getBoundingClientRect();
    // The original iframe is cropped by the native parent, never moved into another document.
    return { x: r.left, y: r.top, width: r.width, height: r.height,
      viewportWidth: innerWidth, viewportHeight: innerHeight,
      inViewport: r.left >= -1 && r.top >= -1 && r.left + r.width <= innerWidth + 1 && r.top + r.height <= innerHeight + 1,
      expanded: /\/bframe/.test(frame.src) };
  };
  const rowText = el => {
    // Labels may be separate spans/divs with NO whitespace between them in the HTML.
    // textContent would turn the card number and its status into a single word.
    const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
    const parts = [];
    for (let node = walker.nextNode(); node; node = walker.nextNode()) {
      if (node.parentElement?.closest('script, style')) continue;
      parts.push(node.textContent);
    }
    return parts.join(' ').replace(/\s+/g, ' ').trim();
  };
  const rows = () => [...document.querySelectorAll('tbody tr, [role="row"]')].filter(visible)
    .map((el, index) => {
      const s = rowText(el), id = s.match(/\b[A-F0-9]{8}\b/i)?.[0];
      const active = /\bOperativa\b/i.test(s) && !/no operativa/i.test(s);
      return id ? { index, id, status: active ? 'Operativa' : 'No operativa', active } : null;
    }).filter(Boolean);
  function snapshot() {
    if (!trusted()) return { stage: 'blocked' };
    if (document.readyState !== 'complete') return { stage: 'loading' };
    if (window.BoleteraSession?.snapshot() === 'expired') return { stage: 'sessionExpired' };
    const path = location.pathname;
    const cap = captcha();
    const error = [...document.querySelectorAll('[role="alert"], .ui-messages-error, .ui-message-error')]
      .some(el => visible(el) && /incorrect|inv[aá]lid|error|bloquead|no coincide|no pud|fall[oó]|intenta de nuevo|intent[aá] nuevamente/i.test(text(el)));
    const base = { captcha: cap, error };
    // Observed SAML handoff from Usuario gub.uy to the Intendencia identity service.
    // Its own form transfers the session. Never read or recreate its SAML fields.
    if (location.hostname === 'ih.montevideo.gub.uy' ||
        (location.hostname !== 'stm.gub.uy' && path === '/sending-saml-response')) return { ...base, stage: 'handoff' };
    if (location.hostname !== 'stm.gub.uy') {
      if (path !== '/login') return { ...base, stage: 'unknown' };
      if (input(/password|contrase/i)) return { ...base, stage: 'password' };
      if (input(/documento|document|dni/i)) return { ...base, stage: 'document' };
      if (button(/^Usuario Gub\.uy(?:$|\s|Realiza)/i)) return { ...base, stage: 'identity' };
      return { ...base, stage: cap ? 'verification' : 'unknown' };
    }
    // A protected URL may still contain the sign-in page during authentication.
    // Its path alone is never evidence that the account is authenticated.
    if (button(/INGRESAR CON USUARIO GUB\.UY/i)) return { ...base, stage: 'start' };
    if (path.endsWith('tarjetas.xhtml')) {
      const cards = rows();
      return { ...base, stage: cards.length ? 'cards' : 'cardsLoading', cards,
        rowCount: document.querySelectorAll('tbody tr, [role="row"]').length,
        visibleRowCount: [...document.querySelectorAll('tbody tr, [role="row"]')].filter(visible).length,
        tablePresent: !!document.getElementById('form1:tablaTarjetas_data') };
    }
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
    if (path.endsWith('recarga2.xhtml')) {
      // IDs and names verified against the live STM logos; list only options present now.
      const names = { '1023': 'Bandes', '1032': 'BBVA', '1002': 'BROU', '94': 'Cabal',
        '1014': 'Heritage', '1031': 'HSBC', '1019': 'Itaú', '1048': 'Mastercard',
        '91': 'OCA', '101': 'PassCard', '1033': 'Prex', '1013': 'Santander',
        '1017': 'Scotiabank', '97': 'Tarjeta D', '1049': 'Visa' };
      const providers = [...document.querySelectorAll('div.banco[id^="id-"]')].filter(visible)
        .map(el => ({ id: el.id.slice(3), name: names[el.id.slice(3)] }))
        .filter(p => p.name);
      const selected = document.querySelector('div.banco.selected[id^="id-"]');
      const next = button(/^CONTINUAR$/i);
      return { ...base, stage: 'paymentBoundary', providers, selectedProvider: selected?.id.slice(3) || null,
        providerReady: !!next && !next.disabled };
    }
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
    // Positioning scrolls the original page only: no frame content, click, answer or token is read or changed.
    if (action === 'positionCaptcha' && state.captcha && !state.captcha.inViewport) {
      const frame = captchaFrame();
      if (!frame || typeof frame.scrollIntoView !== 'function') return false;
      frame.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'instant' });
      return true;
    }
    if (action === 'start' && state.stage === 'start') return click(button(/INGRESAR CON USUARIO GUB\.UY/i));
    if (action === 'identity' && state.stage === 'identity') return click(button(/^Usuario Gub\.uy(?:$|\s|Realiza)/i));
    if (action === 'document' && state.stage === 'document') {
      const field = input(/documento|document|dni/i);
      return setValue(field, value) && click(formButton(field, /^Continuar$/i));
    }
    if (action === 'password' && state.stage === 'password') {
      const field = input(/password|contrase/i);
      return setValue(field, value) && click(formButton(field, /^Continuar$/i));
    }
    if (action === 'card' && state.stage === 'cards') {
      const card = state.cards.find(c => c.id === value && c.active);
      if (!card) return false;
      const row = [...document.querySelectorAll('tbody tr, [role="row"]')].filter(visible)[card.index];
      // PrimeFaces handles clicks originating inside a cell, not on the TR element itself.
      return click(row?.querySelector('td, [role="cell"]') || row);
    }
    if (action === 'minimum' && state.stage === 'balance') return click(button(/^Recargar$/i) ||
      [...document.querySelectorAll('button')].find(el => el.id.endsWith(':btnRecargar') && visible(el)));
    if (action === 'amount' && state.stage === 'amount') {
      const cents = Number(value);
      if (!Number.isSafeInteger(cents) || state.minimum === null || cents < state.minimum || cents <= 0) return false;
      const field = document.getElementById('recarga1:monto_input');
      // PrimeFaces submits its numeric widget's hidden value, not the visible text.
      if (document.getElementById('recarga1:monto_hinput')) {
        const widget = window.PrimeFaces?.widgets?.widget_recarga1_monto;
        if (widget?.id !== 'recarga1:monto' || typeof widget.setValue !== 'function' || typeof widget.getValue !== 'function') return false;
        widget.setValue((cents / 100).toFixed(2));
        if (Math.round(Number(widget.getValue()) * 100) !== cents) return false;
        return click(button(/^CONTINUAR$/i));
      }
      return setValue(field, (cents / 100).toFixed(2).replace('.', ',')) && click(button(/^CONTINUAR$/i));
    }
    if (!state.error && !state.captcha && state.stage === 'paymentBoundary' && ['1033', '1002'].includes(value) && state.providers.some(p => p.id === value)) {
      if (action === 'provider') return click(document.getElementById('id-' + value));
      if (action === 'providerContinue' && state.selectedProvider === value && state.providerReady) return click(button(/^CONTINUAR$/i));
    }
    return false;
  }
  window.BoleteraAdapter = Object.freeze({ snapshot, command, money });
})();
