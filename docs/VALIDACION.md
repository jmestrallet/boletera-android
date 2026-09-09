# Estado de la prueba — 9 de septiembre de 2026

## Resultado

Hay una APK Android con interfaz propia y motor local implementados. **No está validada para una recarga real ni para publicación.**

| Comprobación | Resultado |
|---|---|
| Adaptador JavaScript: acceso con descripción dentro del botón, dinero, deuda, mínimo variable, tarjetas, origen, privacidad, CAPTCHA y límite de pago | 11 pruebas aprobadas |
| Modelos JVM: importes exactos, mínimo y política de navegación | 3 pruebas aprobadas |
| Emulador Android 16: arranque nativo, lectura de fixture en WebView real, ausencia de guardado plano sin biometría y captura de pantalla vacía para revisar diseño | 4 pruebas aprobadas |
| Entrada pública real de STM desde WebView | Aprobada en Android 16 tras corregir la cadena TLS incompleta y reconocer la descripción incluida en el botón de identidad. Solo navegación pública; sin enviar documento ni contraseña |
| Login real con credenciales del usuario en la APK | No realizado |
| CAPTCHA real completo dentro del recorte | No validado |
| Guardar y descifrar con huella física | Implementado; pendiente de dispositivo con biometría configurada |
| Tarjeta guardada de Google y huella en pago | No validado. Solo hay una prueba local de autocompletado |
| Pago o solicitud a Sistarbanc | No realizados por esta APK. Se detiene antes de seleccionar proveedor |

La versión 0.1.0 fallaba con `TLS_REJECTED_3` en Android. El servidor STM enviaba solamente el certificado final y omitía el intermedio. La versión 0.1.1 incluye el certificado público oficial Certum DV TLS G2 R39 como ancla adicional de confianza **solo para el dominio exacto stm.gub.uy dentro de esta aplicación**. No se modifica el almacén de Android. La cadena se verificó contra la raíz oficial; un nombre de servidor incorrecto fue rechazado. Android sigue comprobando TLS y el manejador de errores sigue cancelando, sin `proceed()`.

Fuente del certificado: https://repository.certum.pl/certumdvtlsg2r39ca.pem. SHA-256 del certificado DER: `83C0A5A76844C840DFAF820FFD02ADF6573A26823EF6AF758A3384A0AC044083`.

La ejecución final en Android 16 aprobó las cinco pruebas (cuatro locales y el sondeo público). Los fixtures reproducen estructura conocida con datos ficticios y solo verifican la implementación; no acreditan un login completo ni una recarga real.

## Evidencia local

- JVM: `app/build/test-results/testDebugUnitTest/`.
- Emulador: `app/build/outputs/androidTest-results/connected/debug/` y `app/build/reports/androidTests/connected/debug/`.
- Lint: `app/build/reports/lint-results-debug.html`.
- Captura de pantalla nativa sin datos personales: `outputs/welcome-test.png`.
- Entrega: `outputs/boletera-prueba-0.1.1.apk`.

## Criterio para continuar

1. Instalar la APK de prueba en un Android físico y comprobar si la conexión segura a STM funciona.
2. Si funciona, validar ingreso, CAPTCHA y saldo/mínimo reales; ante una pantalla no reconocida, reparar el adaptador sin ampliar la excepción a páginas completas.
3. Probar guardado con huella, reinicio de app, cancelación biométrica y eliminación del acceso. No reemplazar un fallo biométrico por almacenamiento plano.
4. Resolver una ruta de pago compatible con interfaz propia antes de habilitar cobros. No representar el monto preparado ni un autocompletado exitoso como dinero acreditado.
