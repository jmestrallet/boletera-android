# Estado de la prueba — 9 de septiembre de 2026

## Resultado

Hay una APK Android con interfaz propia y motor local implementados. **No está validada para una recarga real ni para publicación.**

| Comprobación | Resultado |
|---|---|
| Adaptador JavaScript: acceso con descripción dentro del botón, dinero, deuda, mínimo variable, tarjetas, origen, privacidad, CAPTCHA y límite de pago | 12 pruebas aprobadas; incluye números y estados en elementos contiguos sin espacios |
| Modelos JVM: importes exactos, mínimo, política de navegación y origen de respuestas durante transiciones | 4 pruebas aprobadas |
| Compilación desde clon público limpio de GitHub (`a6724d5`) | APK debug, 3 pruebas JVM, lint y 11 pruebas JS aprobados. Sin `local.properties` ni claves del proyecto; se usaron JDK/SDK instalados y caché de dependencias de la PC |
| Emulador Android 16: arranque nativo, fixture en WebView, ausencia de guardado plano sin biometría, capturas habilitadas y recorrido completo offline desde tabla compacta hasta saldo/mínimo usando StmEngine | 5 pruebas locales aprobadas |
| Entrada pública real de STM desde WebView | Aprobada en Android 16 tras corregir la cadena TLS incompleta y reconocer la descripción incluida en el botón de identidad. Solo navegación pública; sin enviar documento ni contraseña |
| Login real con credenciales del usuario en la APK | Usuario informó que 0.1.1 llegó a selección de boletera con lista vacía y luego se bloqueó. Correcciones en 0.1.2 pendientes de confirmar en su teléfono |
| CAPTCHA real completo dentro del recorte | No validado |
| Guardar y descifrar con huella física | Implementado; pendiente de dispositivo con biometría configurada |
| Tarjeta guardada de Google y huella en pago | No validado. Solo hay una prueba local de autocompletado |
| Pago o solicitud a Sistarbanc | No realizados por esta APK. Se detiene antes de seleccionar proveedor |

La versión 0.1.0 fallaba con `TLS_REJECTED_3` en Android. El servidor STM enviaba solamente el certificado final y omitía el intermedio. La versión 0.1.1 incluye el certificado público oficial Certum DV TLS G2 R39 como ancla adicional de confianza **solo para el dominio exacto stm.gub.uy dentro de esta aplicación**. No se modifica el almacén de Android. La cadena se verificó contra la raíz oficial; un nombre de servidor incorrecto fue rechazado. Android sigue comprobando TLS y el manejador de errores sigue cancelando, sin `proceed()`.

Fuente del certificado: https://repository.certum.pl/certumdvtlsg2r39ca.pem. SHA-256 del certificado DER: `83C0A5A76844C840DFAF820FFD02ADF6573A26823EF6AF758A3384A0AC044083`.

La ejecución final de 0.1.2 en Android 16 aprobó las seis pruebas (cinco locales y el sondeo público). Los fixtures usan datos ficticios; no acreditan un login completo ni una recarga real. El test de tabla compacta falló con cero boleteras antes de la corrección y luego aprobó con dos; el recorrido Android llegó al saldo y mínimo sin mostrar páginas completas. La corrección de respuestas de un documento anterior está probada con datos controlados, pero no se demostró que sea la única causa del bloqueo informado en el teléfono.

## Evidencia local

- JVM: `app/build/test-results/testDebugUnitTest/`.
- Emulador: `app/build/outputs/androidTest-results/connected/debug/` y `app/build/reports/androidTests/connected/debug/`.
- Lint: `app/build/reports/lint-results-debug.html`.
- Captura de pantalla nativa sin datos personales: `outputs/welcome-test.png`.
- Entrega: `outputs/boletera-prueba-0.1.2.apk` (7.900.966 bytes), SHA-256 `1491181b78fcbb4008f9bb8458c3b2537f484cd8dc5c1da1ea4f5a405d946f52`. Firma verificada, igual que 0.1.1.

## Criterio para continuar

1. Instalar la APK de prueba en un Android físico y comprobar si la conexión segura a STM funciona.
2. Si funciona, validar ingreso, CAPTCHA y saldo/mínimo reales; ante una pantalla no reconocida, reparar el adaptador sin ampliar la excepción a páginas completas.
3. Probar guardado con huella, reinicio de app, cancelación biométrica y eliminación del acceso. No reemplazar un fallo biométrico por almacenamiento plano.
4. Resolver una ruta de pago compatible con interfaz propia antes de habilitar cobros. No representar el monto preparado ni un autocompletado exitoso como dinero acreditado.
