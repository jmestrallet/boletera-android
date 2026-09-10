# Identificación de navegador: resultado medido

Prueba realizada el 9 de septiembre de 2026, hora de Uruguay. Android 16 en el emulador local; motor WebView 133.0.6943.137. No se instaló una versión nueva en el teléfono del usuario.

## Resultado

La identificación alternativa evitó el rechazo inicial por navegador en la pantalla pública de confirmación de Sistarbanc. **No acredita una recarga, un formulario de pago usable ni la resolución de CAPTCHA o autorización.**

| Medición | Resultado |
|---|---|
| WebView estándar, comprobaciones reproducidas localmente | Detectado como WebView. |
| Identificación parecida a Chrome, comprobaciones locales | Sigue detectado por ausencia de una función de Chrome. |
| Identificación alternativa Chromium, comprobaciones locales | No activa los predicados generales de WebView; ofrece cinco de las seis capacidades inspeccionadas, superando el mínimo de cuatro. |
| Página pública real, WebView estándar | El código original ejecuta su protección y registra acceso bloqueado. |
| Misma página pública real, identificación alternativa | El código original ejecuta su protección y no registra bloqueo por navegador. |

La segunda medición pública se limitó a modificar la identificación de WebView. No se modificó el JavaScript recibido del proveedor, no se suplantaron funciones de JavaScript ni se deshabilitó su detector.

## Alcance de la prueba pública

URL: `https://pasarelaspe.sistarbanc.com.uy/v2/confirmarPago`, sin query ni identificador de operación. En ese recorrido el código original inicializa la protección antes de tratar la ausencia de operación; por eso permite medir el detector sin cargar un pago.

La prueba emplea un directorio de navegador nuevo y rechaza cookies. Solo admite GET a la página y a archivos estáticos delimitados del mismo origen. Bloquea APIs, solicitudes con query, terceros y métodos diferentes de GET. En ambas variantes rechazó localmente siete solicitudes adicionales. No introduce documento, contraseña, tarjeta ni datos personales. No envía formularios ni crea recargas. Cancela errores TLS sin excepciones.

La prueba local usa una reproducción de los predicados generales inspeccionados en el archivo público; no reproduce todas las reglas específicas de navegadores sociales ni posibles verificaciones del servidor. La comprobación pública sí ejecuta el archivo servido por el proveedor.

## Reproducción

- `BrowserIdentityProbe`: medición sin red, con tres identificaciones.
- `BrowserPublicCompatibilityProbe`: requiere el argumento explícito `publicIdentityProbe=true`; queda omitida en una ejecución normal de la suite. Debe ejecutarse sola en un proceso de instrumentación nuevo por su directorio de datos aislado.
- Ambas pruebas están en `app/src/androidTest/java/uy/boletera/prueba/`.
- Los resultados estructurados quedaron en `outputs/browser-identity-probe.json`, excluido de Git.

## Qué falta

Comprobar el comportamiento con una solicitud STM/Prex real, conservar el mismo identificador al continuar, verificar formulario y reconocimiento de datos, completar humanamente las verificaciones que correspondan y controlar la vuelta y el estado final. La prueba sin operación no puede demostrar esos puntos.

En el momento de esta prueba no se cambió el comportamiento de pago de la APK 0.1.7, que abría Chrome. Después se incorporó la variante en la pantalla de Prex de 0.1.8, con perfiles de titular y pruebas de recorrido ficticio. El cambio no acredita un pago real completo. No se contactó a terceros.
