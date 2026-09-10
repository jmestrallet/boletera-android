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
  // jsdom has no layout engine. Model only visibility for the selector tests.
  dom.window.HTMLElement.prototype.getClientRects = function () { return this.hidden ? [] : [{width:100,height:30}]; };
  dom.window.eval(code);
  return dom;
}

test('edición explícita del titular actual cambia sus datos y luego conserva correcciones manuales', () => {
  const dom = setup(form(split).replace('<form>', '<form><select formcontrolname="tipoDocumentoControl"><option value="CI">Cédula de Identidad</option><option value="PAS">Pasaporte</option></select>'));
  try {
    const api=dom.window.BoleteraPayer, doc=dom.window.document;
    let submits=0;doc.addEventListener('submit',e=>{submits++;e.preventDefault();});
    api.use(profile);
    assert.equal(api.updateForThisPayment({...profile,id:'other'}),false);
    assert.equal(api.updateForThisPayment({...profile,givenName:'Editada',documentType:'PAS',document:'AB123456'}),true);
    assert.equal(doc.querySelector('[formcontrolname="tipoDocumentoControl"]').value,'PAS');
    assert.equal(doc.querySelector('[formcontrolname="documentoControl"]').value,'AB123456');
    const name=doc.querySelector('[formcontrolname="nombreControl"]');assert.equal(name.value,'Editada');name.value='Manual';api.status();assert.equal(name.value,'Manual');
    assert.equal(submits,0);
  } finally {dom.window.close();}
});
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
test('no rellena un origen ajeno, formulario ambiguo ni pasaporte', () => {
  for (const [html, url] of [
    [form(split), 'https://otro.example/v2/confirmarPago'],
    [form(split) + form(split), undefined],
    [form(split).replace('<form>', '<form><select formcontrolname="tipoDocumentoControl"><option value="PAS">Pasaporte</option></select>'), undefined]
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

test('elige CI por defecto o PAS explícito y conserva el cambio manual posterior', () => {
  for (const desired of ['CI', 'PAS']) {
    const dom = setup(form(split).replace('<form>', '<form><select formcontrolname="tipoDocumentoControl"><option value="">Tipo Documento</option><option value="CI">Cédula de Identidad</option><option value="PAS">Pasaporte</option></select>'));
    try {
      const select = dom.window.document.querySelector('select');
      let changes = 0;
      select.addEventListener('change', () => changes++);
      dom.window.BoleteraPayer.use({...profile, ...(desired === 'PAS' ? {documentType:'PAS', document:'AB12345'} : {})});
      assert.equal(select.value, desired);
      assert.equal(changes, 1);
      select.value = desired === 'CI' ? 'PAS' : 'CI';
      dom.window.BoleteraPayer.status();
      assert.notEqual(select.value, desired);
      assert.equal(changes, 1);
    } finally { dom.window.close(); }
  }
});

test('selecciona la opción del mat-select original y solo en su panel asociado', async () => {
  for (const desired of ['CI', 'PAS']) {
    const dom = setup(form(split).replace('<form>', '<form><mat-select role="combobox" formcontrolname="tipoDocumentoControl"><div class="mat-select-trigger"><span class="mat-select-placeholder">Tipo Documento</span></div></mat-select>') + '<div role="listbox" id="ajeno"><mat-option role="option">Cédula de Identidad</mat-option></div>');
    try {
      const doc = dom.window.document, select = doc.querySelector('mat-select');
      const step = doc.createElement('section');
      step.className = 'mat-horizontal-stepper-content';
      step.setAttribute('aria-expanded', 'false');
      doc.body.append(step); step.append(doc.querySelector('form'));
      let picked = '', unrelated = 0, opens = 0, submits = 0;
      doc.getElementById('ajeno').onclick = () => unrelated++;
      doc.querySelector('form').onsubmit = e => {e.preventDefault(); submits++;};
      const openPanel = () => {
        opens++;
        // Angular attaches its overlay asynchronously, outside the form.
        setTimeout(() => {
          select.setAttribute('aria-controls', 'doc-panel');
          const panel = doc.createElement('div'); panel.id = 'doc-panel'; panel.setAttribute('role','listbox');
          for (const [value, label] of [['CI','Cédula de Identidad'],['PAS','Pasaporte']]) {
            const option = doc.createElement('mat-option'); option.setAttribute('role','option'); option.textContent=label;
            option.onclick = () => {picked=value; select.innerHTML='<div class="mat-select-trigger"><span class="mat-select-value-text">'+label+'</span></div>'; select.querySelector('.mat-select-trigger').onclick=openPanel; panel.remove();};
            panel.append(option);
          }
          doc.body.append(panel);
        }, 0);
      };
      dom.window.BoleteraPayer.use({...profile, documentType:desired});
      await new Promise(resolve => setTimeout(resolve, 5));
      assert.equal(opens, 0, 'No intenta abrir el selector de un paso oculto');
      step.setAttribute('aria-expanded', 'true');
      dom.window.BoleteraPayer.status();
      // Angular may insert the node before registering the trigger's handler.
      select.querySelector('.mat-select-trigger').onclick = openPanel;
      for(let i=0;i<100 && picked!==desired;i++)await new Promise(resolve=>setTimeout(resolve,10));
      assert.equal(picked, desired);
      assert.equal(dom.window.BoleteraPayer.status(), 'filled');
      assert.equal(opens, 1);
      assert.equal(unrelated, 0); assert.equal(submits, 0);
      const next = desired === 'CI' ? 'PAS' : 'CI';
      assert.equal(dom.window.BoleteraPayer.updateForThisPayment({...profile, documentType:next, givenName:'Editada', document:next === 'CI' ? '11111111' : 'AB123456'}), true);
      for(let i=0;i<100 && picked!==next;i++)await new Promise(resolve=>setTimeout(resolve,10));
      assert.equal(picked, next);
      assert.equal(doc.querySelector('[formcontrolname="nombreControl"]').value,'Editada');
      assert.equal(doc.querySelector('[formcontrolname="documentoControl"]').value,next === 'CI' ? '11111111' : 'AB123456');
      assert.equal(opens,2);assert.equal(submits,0);
    } finally { dom.window.close(); }
  }
});
