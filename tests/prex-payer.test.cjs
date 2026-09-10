const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');
const code = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/prex-payer.js'), 'utf8');
const profile = {id:'fixture-person', givenName:'Persona', familyName:'Ficticia', document:'00000000', email:'persona@example.invalid', phone:'000000000'};
const split = ['nombreControl','apellidoControl','documentoControl','emailControl','celularControl'];
const combined = ['nombreControl','documentoControl','correoControl','nroTarjetaControl','expiracionControl','cvvControl'];
const form = names => `<form>${names.map(n => `<input formcontrolname="${n}">`).join('')}<input name="g-recaptcha-response"><button>Continuar</button></form>`;
function setup(html, url = 'https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago') {
  const dom = new JSDOM(html, {url, runScripts:'outside-only'});
  dom.window.eval(code);
  return dom;
}
test('ambos formatos de titular reciben sus campos y los eventos, sin enviar ni leer la tarjeta', () => {
  for (const names of [split, combined]) {
    const dom = setup(form(names));
    try {
      const doc = dom.window.document;
      let reads = 0, submits = 0, inputs = 0;
      for (const name of ['nroTarjetaControl','expiracionControl','cvvControl']) {
        const input = doc.querySelector(`[formcontrolname="${name}"]`);
        if (input) Object.defineProperty(input, 'value', { get() { reads++; return ''; }, set() { throw new Error('No debe escribir la tarjeta'); } });
      }
      doc.addEventListener('submit', e => { submits++; e.preventDefault(); });
      doc.addEventListener('input', () => inputs++);
      assert.equal(dom.window.BoleteraPayer.use(profile), true);
      assert.equal(doc.querySelector('[formcontrolname="nombreControl"]').value, names === split ? 'Persona' : 'Persona Ficticia');
      assert.equal(doc.querySelector('[formcontrolname="documentoControl"]').value, '00000000');
      assert.equal(inputs, names === split ? 5 : 3);
      assert.equal(reads, 0);
      assert.equal(submits, 0);
      assert.equal(doc.querySelector('[name="g-recaptcha-response"]').value, '');
    } finally { dom.window.close(); }
  }
});
test('conserva ediciones humanas, impide sustituir el perfil en la misma página y trata nombres como texto', async () => {
  const dom = setup(form(split));
  try {
    const doc = dom.window.document;
    const hostileName = '<img src=x onerror=alert(1)>';
    dom.window.BoleteraPayer.use({...profile, givenName:hostileName});
    assert.equal(doc.querySelector('[formcontrolname="nombreControl"]').value, hostileName);
    assert.equal(doc.querySelectorAll('img').length, 0);
    doc.querySelector('[formcontrolname="documentoControl"]').value = '';
    assert.equal(dom.window.BoleteraPayer.use({...profile, id:'another-person'}), false);
    doc.body.append(doc.createElement('div'));
    await new Promise(resolve => setTimeout(resolve, 0));
    assert.equal(doc.querySelector('[formcontrolname="documentoControl"]').value, '');
  } finally { dom.window.close(); }
});
test('no rellena un origen ajeno, formulario ambiguo ni documento extranjero', () => {
  for (const [html, url] of [
    [form(split), 'https://otro.example/v2/confirmarPago'],
    [form(split) + form(split), undefined],
    [form(split).replace('<form>', '<form><select formcontrolname="tipoDocumentoControl"><option value="EXT">Documento Extranjero</option></select>'), undefined]
  ]) {
    const dom = setup(html, url);
    try {
      dom.window.BoleteraPayer?.use(profile);
      for (const input of dom.window.document.querySelectorAll('input')) assert.equal(input.value, '');
    } finally { dom.window.close(); }
  }
});
test('completa campos incorporados después por la página', async () => {
  const dom = setup('<main></main>');
  try {
    dom.window.BoleteraPayer.use(profile);
    dom.window.document.querySelector('main').innerHTML = form(combined);
    await new Promise(resolve => setTimeout(resolve, 0));
    assert.equal(dom.window.document.querySelector('[formcontrolname="correoControl"]').value, profile.email);
  } finally { dom.window.close(); }
});
test('los datos previos de otro titular requieren una acción explícita, sin tocar la tarjeta', () => {
  const dom = setup(form(combined).replace('formcontrolname="nombreControl"', 'formcontrolname="nombreControl" value="Otro titular"'));
  try {
    const doc = dom.window.document;
    dom.window.BoleteraPayer.use(profile);
    assert.equal(dom.window.BoleteraPayer.status(), 'conflict');
    assert.equal(doc.querySelector('[formcontrolname="nombreControl"]').value, 'Otro titular');
    assert.equal(doc.querySelector('[formcontrolname="documentoControl"]').value, '');
    assert.equal(dom.window.BoleteraPayer.applyChosenProfile(), true);
    assert.equal(doc.querySelector('[formcontrolname="nombreControl"]').value, 'Persona Ficticia');
    assert.equal(doc.querySelector('[formcontrolname="nroTarjetaControl"]').value, '');
    assert.equal(dom.window.BoleteraPayer.status(), 'filled');
  } finally { dom.window.close(); }
});
