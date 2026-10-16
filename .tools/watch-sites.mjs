// Daily watch of the public pages Boletera drives (STM, ID Uruguay, the Prex gateway).
// It never signs in: it reads only what anyone can open, takes a fingerprint (where each page lands, its scripts,
// form fields and buttons, and whether the gateway's bundle still contains the pieces the app reads), and compares it
// with the last one. Exit code 1 and a short Spanish summary when something changed.
// Usage: node .tools/watch-sites.mjs [--update]   (baseline in .tools/site-baseline.json, log in .tools/site-watch.log)
import fs from 'node:fs';
const here = new URL('.', import.meta.url);
const baselineFile = new URL('site-baseline.json', here), logFile = new URL('site-watch.log', here);
const UA = 'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0 Mobile Safari/537.36';
// What prex-*.js depends on inside the gateway's Angular bundle.
const PREX_PIECES = ['alta-cliente', 'alta-tarjeta', 'stepper-pago', 'finalizar-pago', 'angular-recaptcha', 're-captcha',
  'nombreControl', 'apellidoControl', 'documentoControl', 'emailControl', 'celularControl', 'tipoDocumentoControl', 'recaptchaSuccess'];

async function get(url) {
  const r = await fetch(url, { headers: { 'user-agent': UA, 'accept-language': 'es-UY,es;q=0.9' }, redirect: 'follow' });
  return { status: r.status, url: r.url, text: await r.text() };
}
const all = (re, text) => [...text.matchAll(re)].map(m => m[1]);
function shape(page) {
  const t = page.text;
  const u = new URL(page.url);
  return {
    status: page.status,
    lands: u.host + u.pathname.replace(/\d+/g, '#').replace(/;jsessionid=[^/]*/i, ''),
    scripts: [...new Set(all(/<script[^>]+src="([^"]+)"/gi, t).map(s => s.replace(/[?#].*$/, '').replace(/;jsessionid=[^/]*/i, '')))].sort(),
    fields: [...new Set(all(/<(?:input|select|textarea)[^>]*\bname="([^"]+)"/gi, t).filter(n => !/viewstate|csrf|token|nonce/i.test(n)))].sort(),
    buttons: [...new Set(all(/<button[^>]*>([^<]{1,60})</gi, t).map(s => s.trim()).filter(Boolean))].sort(),
    title: (t.match(/<title>([^<]*)<\/title>/i)?.[1] || '').trim().replace(/\d/g, '#'),
  };
}

const now = {};
const problems = [];
for (const [name, url] of [
  ['STM (entrada a Mi STM)', 'https://stm.gub.uy/app/mistm/cuenta/'],
  ['ID Uruguay', 'https://mi.iduruguay.gub.uy/'],
  ['Pasarela de Prex', 'https://pasarelaspe.sistarbanc.com.uy/v2/'],
]) {
  try { now[name] = shape(await get(url)); } catch (e) { problems.push(`${name}: no respondió (${e.message})`); }
}
// The gateway bundle: still has the pieces the app reads?
const gw = now['Pasarela de Prex'];
const bundle = gw?.scripts.find(s => /main(-es2015)?\.[0-9a-f]+\.js$/.test(s));
if (bundle) {
  try {
    const js = (await get(new URL(bundle, 'https://pasarelaspe.sistarbanc.com.uy/v2/').href)).text;
    gw.bundle = bundle.split('/').pop();
    gw.angular = js.match(/VERSION\s*=\s*new\s+\w+\("([\d.]+)"\)/)?.[1] || js.match(/"(\d+\.\d+\.\d+)"\)\s*;?\s*\w*\s*$/m)?.[1] || '';
    gw.missing = PREX_PIECES.filter(p => !js.includes(p));
  } catch (e) { problems.push(`Pasarela: no se pudo leer el bundle (${e.message})`); }
}

const update = process.argv.includes('--update');
const before = fs.existsSync(baselineFile) ? JSON.parse(fs.readFileSync(baselineFile, 'utf8')) : null;
const changes = [];
if (before) for (const name of Object.keys(now)) {
  const a = before[name], b = now[name];
  if (!a) { changes.push(`${name}: página nueva en el chequeo`); continue; }
  for (const key of ['status', 'lands', 'title', 'bundle', 'angular']) if ((a[key] ?? '') !== (b[key] ?? '')) changes.push(`${name}: ${key} «${a[key] ?? ''}» → «${b[key] ?? ''}»`);
  for (const key of ['scripts', 'fields', 'buttons']) {
    const added = (b[key] || []).filter(x => !(a[key] || []).includes(x)), gone = (a[key] || []).filter(x => !(b[key] || []).includes(x));
    if (added.length || gone.length) changes.push(`${name}: ${key}${added.length ? ' +' + added.join(', +') : ''}${gone.length ? ' −' + gone.join(', −') : ''}`);
  }
}
if (gw?.missing?.length) problems.push(`Pasarela: el código de Prex ya no tiene ${gw.missing.join(', ')} (la app lo usa)`);

const stamp = new Date().toISOString().slice(0, 16).replace('T', ' ');
const summary = !before ? 'Primera huella guardada.' : changes.length || problems.length ? [...problems, ...changes].join('\n') : 'Sin cambios.';
fs.appendFileSync(logFile, `${stamp}  ${summary.replace(/\n/g, '\n                  ')}\n`);
if (!before || update || (!changes.length && !problems.length)) fs.writeFileSync(baselineFile, JSON.stringify(now, null, 1));
console.log(summary);
process.exit(before && (changes.length || problems.length) && !update ? 1 : 0);
