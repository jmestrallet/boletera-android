const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { JSDOM } = require('jsdom');
const source = fs.readFileSync('app/src/main/assets/stm-adapter.js', 'utf8');
test('identity provider button may contain its descriptive children', () => {
  const { dom, adapter } = page('login', '<button>Usuario Gub.uy<span>Realiza trámites con tu número de documento y contraseña</span><span>Básico o intermedio</span></button>', 'mi.iduruguay.gub.uy');
  let clicks = 0;
  dom.window.document.querySelector('button').onclick = () => clicks++;
  assert.equal(adapter.snapshot().stage, 'identity');
  assert.equal(adapter.command('identity', ''), true);
  assert.equal(clicks, 1);
});
function page(path, html, host = 'stm.gub.uy') {
  const dom = new JSDOM(html, { url: `https://${host}/${path}`, runScripts: 'outside-only' });
  dom.window.HTMLElement.prototype.getBoundingClientRect = function () {
    return { x: 0, y: 0, left: 12, top: 20, width: 304, height: 78, right: 316, bottom: 98 };
  };
  dom.window.eval(source);
  return { dom, adapter: dom.window.BoleteraAdapter };
}
test('read the observed positive balance and locale formatting', () => {
  const { adapter } = page('app/mistm/cuenta/pages/principal.xhtml', '<p>Saldo disponible*: $ 314</p><p>* El saldo real puede diferir</p><button>Recargar</button>');
  assert.equal(adapter.snapshot().balance, 31400);
  assert.equal(adapter.money('$ -304'), -30400);
  assert.equal(adapter.money('- $ 304'), -30400);
  assert.equal(adapter.money('$ 1.234,56'), 123456);
  assert.equal(adapter.money('$ 1.234'), 123400);
});
test('minimum comes from the site including debt, never inferred', () => {
  const { dom, adapter } = page('app/mistm/cuenta/pages/recarga1.xhtml', `
    <p>Tu recarga mínima deberá ser de $ 564 .</p><label>Saldo actual *</label>
    <input id="recarga1:saldoActual" value="$ -304"><input id="recarga1:monto_input"><button>CONTINUAR</button>`);
  assert.equal(adapter.snapshot().minimum, 56400);
  assert.equal(adapter.snapshot().balance, -30400);
  let clicks = 0;
  dom.window.document.querySelector('button').onclick = () => clicks++;
  assert.equal(adapter.command('amount', '56399'), false);
  assert.equal(clicks, 0);
  assert.equal(adapter.command('amount', '56400'), true);
  assert.equal(clicks, 1);
  assert.equal(dom.window.document.getElementById('recarga1:monto_input').value, '564,00');
  dom.window.document.querySelector('p').textContent = 'Tu recarga mínima deberá ser de $ 600 .';
  assert.equal(adapter.command('amount', '56400'), false);
  assert.equal(clicks, 1);
});
test('unrecognized minimum fails closed', () => {
  const { adapter } = page('app/mistm/cuenta/pages/recarga1.xhtml', '<p>Nueva pantalla</p><input id="recarga1:monto_input"><button>CONTINUAR</button>');
  assert.equal(adapter.snapshot().minimum, null);
  assert.equal(adapter.command('amount', '56400'), false);
});
test('only operational cards can be selected', () => {
  const { dom, adapter } = page('app/mistm/cuenta/pages/tarjetas.xhtml', '<table><tbody><tr><td>ABCD1234 Operativa</td></tr><tr><td>DEAD5678 Pte. Anular (Caducidad G.U.)</td></tr></tbody></table>');
  let clicks = 0;
  dom.window.document.querySelectorAll('tr').forEach(el => { el.onclick = () => clicks++; });
  assert.equal(adapter.snapshot().cards.length, 2);
  assert.equal(adapter.command('card', 'DEAD5678'), false);
  assert.equal(adapter.command('card', 'ABCD1234'), true);
  assert.equal(clicks, 1);
});

test('card labels in adjacent elements remain separate without whitespace in HTML', () => {
  const { dom, adapter } = page('app/mistm/cuenta/pages/tarjetas.xhtml', '<table><tbody><tr><td><img><span>ABCD1234</span><span>Operativa</span></td><td><a>›</a></td></tr><tr><td><span>DEAD5678</span><span>Pte. Anular (Caducidad G.U.)</span></td></tr></tbody></table>');
  assert.equal(adapter.snapshot().cards.length, 2);
  assert.equal(adapter.snapshot().cards[0].active, true);
  assert.equal(adapter.snapshot().cards[1].active, false);
  let clicks = 0;
  dom.window.document.querySelector('tr').onclick = () => clicks++;
  assert.equal(adapter.command('card', 'ABCD1234'), true);
  assert.equal(clicks, 1);
});
test('credential contents never appear in snapshots; input is a literal', () => {
  const { dom, adapter } = page('login', '<input type="password" placeholder="Ingresá tu contraseña"><button>Continuar</button>', 'mi.iduruguay.gub.uy');
  const secret = `synthetic'\"\\);alert(1);`;
  assert.equal(adapter.command('password', secret), true);
  assert.equal(dom.window.document.querySelector('input').value, secret);
  assert.equal(adapter.snapshot().stage, 'password');
  assert.equal(JSON.stringify(adapter.snapshot()).includes(secret), false);
});
test('third party pages cannot receive credential commands', () => {
  const { dom, adapter } = page('login', '<input type="password"><button>Continuar</button>', 'mi.iduruguay.gub.uy.evil.test');
  assert.equal(adapter.snapshot().stage, 'blocked');
  assert.equal(adapter.command('password', 'synthetic'), false);
  assert.equal(dom.window.document.querySelector('input').value, '');
});
test('payment boundary has no submit implementation', () => {
  const { dom, adapter } = page('app/mistm/cuenta/pages/recarga2.xhtml', '<button>CONTINUAR</button>');
  let clicks = 0;
  dom.window.document.querySelector('button').onclick = () => clicks++;
  assert.equal(adapter.snapshot().stage, 'paymentBoundary');
  for (const command of ['pay', 'confirm', 'amount', 'password']) assert.equal(adapter.command(command, '26000'), false);
  assert.equal(clicks, 0);
});
test('CAPTCHA exposes only geometry, never clicks or copies challenge', () => {
  const { adapter } = page('login', `<iframe src="https://www.google.com/recaptcha/api2/anchor?size=normal"></iframe>`, 'mi.iduruguay.gub.uy');
  assert.equal(adapter.snapshot().captcha.width, 304);
  assert.equal(adapter.snapshot().captcha.height, 78);
  assert.equal(adapter.command('captcha', ''), false);
});
test('invisible CAPTCHA is not exposed as an interactive panel', () => {
  const { adapter } = page('login', `<iframe src="https://www.google.com/recaptcha/api2/anchor?size=invisible"></iframe>`, 'mi.iduruguay.gub.uy');
  assert.equal(adapter.snapshot().captcha, null);
});
test('expanded challenge is prioritized over checkbox; hidden frames ignored', () => {
  const { adapter } = page('login', `
    <iframe src="https://www.google.com/recaptcha/api2/anchor?size=normal"></iframe>
    <div style="visibility:hidden"><iframe src="https://www.google.com/recaptcha/api2/bframe?hidden=1"></iframe></div>
    <iframe src="https://www.google.com/recaptcha/api2/bframe?expanded=1"></iframe>`, 'mi.iduruguay.gub.uy');
  assert.equal(adapter.snapshot().captcha.expanded, true);
});
