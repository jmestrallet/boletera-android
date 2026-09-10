const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');
const code = fs.readFileSync(path.join(__dirname, '../experiments/pago-apariencia/boletera.user.js'), 'utf8');
const names = ['nombreControl', 'apellidoControl', 'documentoControl', 'emailControl', 'celularControl'];
const profile = Object.fromEntries(names.map((name, i) => [name, `Ficticio${i}`]));
const form = () => `<form>${names.map(name => `<input formcontrolname="${name}">`).join('')}<input name="cardnumber"><input name="cvv"><input name="g-recaptcha-response"><button type="submit">Pagar</button></form>`;
function setup(html = form(), url = 'https://pasarelaspe.sistarbanc.com.uy/v2/prueba', stored = profile) {
  const dom = new JSDOM(html, { url, runScripts: 'outside-only' });
  let data = stored;
  let submitted = 0;
  dom.window.GM_getValue = () => data;
  dom.window.GM_setValue = (_, value) => { data = value; };
  dom.window.GM_deleteValue = () => { data = null; };
  dom.window.document.addEventListener('submit', e => { submitted++; e.preventDefault(); });
  dom.window.eval(code);
  return { dom, doc: dom.window.document, data: () => data, submitted: () => submitted };
}
test('completa solo datos del titular, sin tarjeta, CAPTCHA ni envío', () => {
  const s = setup();
  try {
    for (const name of names) assert.equal(s.doc.querySelector(`[formcontrolname="${name}"]`).value, profile[name]);
    for (const name of ['cardnumber', 'cvv', 'g-recaptcha-response']) assert.equal(s.doc.querySelector(`[name="${name}"]`).value, '');
    assert.equal(s.submitted(), 0);
  } finally { s.dom.window.close(); }
});
test('rechaza origen ajeno, rutas ajenas y formularios ambiguos o incompletos', () => {
  for (const [html, url] of [
    [form(), 'https://otro.example/v2/prueba'],
    [form(), 'https://pasarelaspe.sistarbanc.com.uy/otra/prueba'],
    [form() + form(), undefined],
    ['<form><input formcontrolname="nombreControl"><input name="cardnumber"></form>', undefined],
  ]) {
    const s = setup(html, url);
    try { for (const input of s.doc.querySelectorAll('form input')) assert.equal(input.value, ''); }
    finally { s.dom.window.close(); }
  }
});
test('conserva valores existentes y correcciones humanas después de cambios de página', async () => {
  const s = setup(form().replace('formcontrolname="nombreControl"', 'formcontrolname="nombreControl" value="Ya escrito"'));
  try {
    assert.equal(s.doc.querySelector('[formcontrolname="nombreControl"]').value, 'Ya escrito');
    const input = s.doc.querySelector('[formcontrolname="celularControl"]');
    input.value = '';
    s.doc.body.append(s.doc.createElement('div'));
    await new Promise(resolve => setTimeout(resolve, 0));
    assert.equal(input.value, '');
    assert.equal(s.submitted(), 0);
  } finally { s.dom.window.close(); }
});
test('detecta formulario insertado por navegación y permite borrar el perfil', async () => {
  const s = setup('<main></main>');
  try {
    s.doc.querySelector('main').innerHTML = form();
    await new Promise(resolve => setTimeout(resolve, 0));
    assert.equal(s.doc.querySelector('[formcontrolname="nombreControl"]').value, profile.nombreControl);
    [...s.doc.querySelectorAll('#boletera-local-tools button')].find(b => b.textContent === 'Borrar datos guardados').click();
    assert.equal(s.data(), null);
    assert.equal(s.doc.querySelector('[formcontrolname="nombreControl"]').value, profile.nombreControl);
  } finally { s.dom.window.close(); }
});
