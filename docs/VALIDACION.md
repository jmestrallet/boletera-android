# Estado de la prueba — 9 de septiembre de 2026

## Resultado

La APK release 0.1.7 protege la revisión de pagos hasta completar el ingreso y corrige el encuadre de verificaciones fuera de la vista. Conserva Google y la recuperación de Prex. El inicio real de Prex y eBROU se comprobó con 0.1.5; las comprobaciones nuevas de 0.1.6 usan datos ficticios e interceptan la apertura del navegador. **No se validó una autorización bancaria ni acreditación. No está lista para publicación general.**

| Comprobación | Resultado |
|---|---|
| Adaptador JavaScript: acceso, dinero, deuda, mínimo variable, tarjetas, origen, privacidad, CAPTCHA y límite de pago | 22 pruebas aprobadas; incluye formularios superpuestos, traspaso de identidad, selección de celda, importe mediante el control numérico de STM y medios de pago |
| Modelos JVM: importes exactos, mínimo, política de navegación y origen de respuestas durante transiciones | 6 pruebas aprobadas, incluyendo destinos y formulario de pago |
| Compilación desde clon público limpio de GitHub (`a6724d5`) | APK debug, 3 pruebas JVM, lint y 11 pruebas JS aprobados. Sin `local.properties` ni claves del proyecto; se usaron JDK/SDK instalados y caché de dependencias de la PC |
| Emulador Android 16: arranque nativo, fixture en WebView, ausencia de guardado plano sin biometría, capturas habilitadas y recorrido offline usando StmEngine con login en URL protegida y tabla demorada hasta saldo/mínimo | 10 pruebas locales aprobadas; también preferencias por cuenta, guardia pendiente, traspaso eBROU y recuperación cifrada de Prex |
| Entrada pública real de STM desde WebView | Aprobada en Android 16 tras corregir la cadena TLS incompleta y reconocer la descripción incluida en el botón de identidad. Solo navegación pública; sin enviar documento ni contraseña |
| Login real con credenciales del usuario en la APK | Comprobado en emulador Android 16 con credenciales autorizadas: la APK release 0.1.5 ingresó, recordó la boletera operativa y llegó automáticamente al saldo y mínimo. Pendiente de confirmación en teléfono físico |
| CAPTCHA real completo dentro del recorte | No validado |
| Guardar y descifrar con huella física | Implementado; pendiente de dispositivo con biometría configurada |
| Tarjeta guardada de Google y huella en pago | No validado. Solo hay una prueba local de autocompletado |
| Inicio real Prex y eBROU | Comprobado con la APK release 0.1.5: Prex hasta resumen oficial con el importe elegido y eBROU hasta ingreso oficial del banco. Sin datos bancarios ni autorización |
| Regreso y preferencias reales | Consultar saldo sin reingreso, aviso pendiente persistente, cambio de boletera y medio recordado comprobados |
| Débito y acreditación | No comprobados |

La versión 0.1.0 fallaba con `TLS_REJECTED_3` en Android. El servidor STM enviaba solamente el certificado final y omitía el intermedio. La versión 0.1.1 incluye el certificado público oficial Certum DV TLS G2 R39 como ancla adicional de confianza **solo para el dominio exacto stm.gub.uy dentro de esta aplicación**. No se modifica el almacén de Android. La cadena se verificó contra la raíz oficial; un nombre de servidor incorrecto fue rechazado. Android sigue comprobando TLS y el manejador de errores sigue cancelando, sin `proceed()`.

Fuente del certificado: https://repository.certum.pl/certumdvtlsg2r39ca.pem. SHA-256 del certificado DER: `83C0A5A76844C840DFAF820FFD02ADF6573A26823EF6AF758A3384A0AC044083`.

La ejecución final de 0.1.2 en Android 16 aprobó las seis pruebas (cinco locales y el sondeo público). Los fixtures usan datos ficticios; no acreditan un login completo ni una recarga real. El test de tabla compacta falló con cero boleteras antes de la corrección y luego aprobó con dos; el recorrido Android llegó al saldo y mínimo sin mostrar páginas completas. La corrección de respuestas de un documento anterior está probada con datos controlados, pero no se demostró que sea la única causa del bloqueo informado en el teléfono.

La ejecución de 0.1.3 también aprobó las seis pruebas Android, incluyendo un recorrido ampliado: URL de boleteras con formulario de login, ingreso con contraseña sintética, demora de la tabla y selección de boletera hasta saldo y mínimo. Dos regresiones JS fallaron antes de corregir el reconocimiento por URL y luego aprobaron. Ahora también se exige que el documento termine de cargar antes de clasificarlo. Esto no sustituye confirmar el resultado en el teléfono del usuario. La referencia visible solo incluye etapas y conteos; las capturas recibidas no se publican.

La prueba real de 0.1.4 encontró problemas que los fixtures anteriores no reproducían: formularios de documento y contraseña superpuestos, el traspaso por `ih.montevideo.gub.uy`, selección PrimeFaces que requiere un clic en una celda y el botón de recarga sin etiqueta propia. Corregidos esos pasos, la APK release llegó automáticamente a la consulta real de saldo y mínimo. No se publican datos de la cuenta ni capturas del recorrido autenticado.

Al elegir el importe, apareció además un rechazo porque el campo visible no actualizaba el valor interno de PrimeFaces. Se corrigió usando el método del control numérico observado. Ese adaptador corregido se ejecutó contra la sesión real en una compilación de desarrollo y llegó a `recarga2.xhtml` sin errores, sin seleccionar proveedor ni solicitar pago. La APK final incorpora ese mismo código y pasó 19 pruebas JS, 4 JVM y lint; el recorrido Android de seis pruebas aprobó antes de esta última corrección exclusiva del importe. No se repitió un ingreso completo con el binario final tras esa corrección. Durante el ingreso real no apareció un desafío CAPTCHA; no se lo considera validado.
## Validación final de 0.1.5

Con la APK release final se abrió el ingreso real de eBROU y se regresó a la consulta de saldo sin iniciar sesión otra vez. También se verificaron el diálogo de revisión, la liberación manual tras salir sin autorizar y el cambio de boletera. Prex había llegado a su resumen oficial con la misma implementación de pago; las correcciones posteriores afectaron el regreso y el diálogo. La elección de boletera y Prex sobrevivió a una actualización e ingreso nuevo. Ningún recorrido ingresó credenciales bancarias, autorizó un débito o comprobó acreditación. No se publican datos de la cuenta ni capturas autenticadas.

Después de la última corrección del regreso se ejecutaron las ocho pruebas Android: todas aprobadas, incluyendo el sondeo público. El build final aprobó 21 pruebas JS, 6 JVM y lint. El diálogo se inspeccionó visualmente en el emulador.

## Validación de 0.1.6

Se probó la persistencia cifrada del enlace tras recrear las preferencias, su separación por cuenta y operación, el rechazo de datos modificados, su eliminación y la conservación del aviso pendiente. Un recorrido completo con formularios ficticios verificó que el botón no se habilita antes del ingreso, abrió el diálogo nativo y capturó el enlace exacto enviado a Chrome, sin solicitudes adicionales al motor STM. La apertura de Chrome se interceptó en las pruebas: no hubo conexión al banco ni se comprobó cuánto dura un enlace real.

La suite final comprende 21 pruebas JavaScript, 6 JVM, lint y 10 pruebas Android (9 locales y el sondeo público). No se agregó un servicio de autocompletado ni se cambiaron los ajustes de Google.

## Validación de 0.1.7

Se reprodujo que un ingreso sin verificar podía eliminar el aviso pendiente; la regresión falló antes del cambio y aprobó después. Los avisos no se cargan en la interfaz ni pueden reconocerse hasta leer las boleteras de la sesión autenticada.

Otra regresión Android reprodujo un iframe fuera del área visible del WebView. Se agregó una orden que desplaza la página original antes del recorte, sin leer ni modificar el contenido del desafío. Con un iframe completamente ficticio, servido localmente bajo URLs interceptadas, se verificaron el panel chico, el expandido y los toques a través del contenedor nativo. Se esperó la entrega asíncrona del evento; la comprobación de posición admite un píxel CSS de redondeo. Se inspeccionó la captura del panel expandido: texto y controles sin recortes.

Resultado final: 22 pruebas JavaScript, 6 JVM, lint y 11 Android aprobadas. No se contactó Google para resolver un CAPTCHA ni se validó uno real; tampoco se autorizó un pago. El único sondeo de red de la suite es la entrada pública de STM.

## Evidencia local

- JVM: `app/build/test-results/testDebugUnitTest/`.
- Emulador: `app/build/outputs/androidTest-results/connected/debug/` y `app/build/reports/androidTests/connected/debug/`.
- Lint: `app/build/reports/lint-results-debug.html`.
- Captura de pantalla nativa sin datos personales: `outputs/welcome-test.png`.
- Entrega: `outputs/boletera-prueba-0.1.7.apk` (7918654 bytes), SHA-256 `d8114f93387e34882b78e131d5a238c52a4a058a6bf25ad1911874c8eadc306b`. Firma verificada, igual que las versiones anteriores.

## Criterio para continuar

1. Instalar la APK de prueba en un Android físico y comprobar si la conexión segura a STM funciona.
2. Si funciona, validar ingreso, CAPTCHA y saldo/mínimo reales; ante una pantalla no reconocida, reparar el adaptador sin ampliar la excepción a páginas completas.
3. Probar guardado con huella, reinicio de app, cancelación biométrica y eliminación del acceso. No reemplazar un fallo biométrico por almacenamiento plano.
4. Verificar la recuperación de una solicitud Prex y comprobar en el teléfono la autorización y acreditación de una recarga. No representar el monto preparado, el regreso o un cambio de saldo como confirmación del pago.
