# Estado de la prueba — 10 de septiembre de 2026

## Versión vigente: 0.1.16

Rediseño nativo de acceso, saldo, importe, configuración y datos del titular. Incluye temas claro/oscuro, Google Sans y Material Symbols. [Decisiones y referencias](DISENO.md).

Pasaron 40 pruebas JavaScript, 7 JVM, lint y compilación release. Cinco comprobaciones de diseño pasaron en tamaño habitual, pantalla de 360 × 640 dp, texto 2× con animaciones desactivadas y orientación horizontal. Las capturas usan datos ficticios. La comprobación del texto se realiza sobre las líneas de glifos; una primera medición confundía el ancho disponible del párrafo con texto recortado y fue corregida.

También pasaron las cuatro pruebas de dispositivo, el recorrido completo interceptado localmente, la conservación de la página y la carga demorada de Prex. La comprobación funcional final usó el gesto real de actualizar y el nuevo selector de importe, y terminó sin repetir una solicitud. La búsqueda de actualizaciones desde el nuevo icono también aprobó. Registros locales: `outputs/design-final-functional-0.1.16.txt` y `outputs/design-*-0.1.16.txt`.

La demostración de movimiento usa los componentes de producción con valores ficticios, sin acceso a STM ni autorización de pagos. No es una medición de rendimiento de todos los dispositivos. La APK está instalada en el OnePlus, versión `0.1.16-prueba`, código 17. SHA-256: `884356F391F71CB379FB93CB96D31B98E6ECDC854649EAD63AE93D2D7E930761`.

La aceptación visual del titular y la autorización/acreditación reales siguen pendientes. Esta entrega no hizo un pago real.

## Versión anterior: 0.1.15

Configuración permite buscar versiones de prueba publicadas en `jmestrallet/boletera-android`, descargar una versión posterior y abrir el instalador de Android. No usa credenciales de GitHub ni consulta la cuenta STM. Verifica tamaño, SHA-256 publicado, paquete, versión y misma firma; no ofrece bajar de versión. Comparte únicamente la APK privada mediante FileProvider y un permiso de lectura temporal.

Pasaron 40 pruebas JavaScript, 7 JVM, lint y compilación. Cuatro pruebas Android comprobaron selección numérica de versiones de prueba, rechazo de borradores/destinos ajenos/digest ausente, controles de paquete/firma/versión, consulta real de GitHub desde Configuración y descarga pública con comprobación de SHA-256. La descarga con hash alterado fue rechazada y eliminada; la versión publicada anterior no se aceptó como actualización. La prueba positiva de identidad usó la APK debug real con metadatos de versión anterior como referencia: no representa una actualización instalada desde GitHub.

Una quinta prueba comprobó el URI privado, el permiso temporal, el rechazo de un archivo fuera del directorio compartido y abrió el instalador real de Android 16. La captura local muestra «Do you want to update this app?»; se salió sin confirmar. Para esa prueba se habilitó la instalación desde esta app únicamente en el emulador. No se modificó ese ajuste en el teléfono.

Evidencia local: `outputs/updates-0.1.15-test.txt`, `outputs/updates-installer-0.1.15-test.txt`, `outputs/update-installer-0.1.15.png`. APK final instalada por cable en el OnePlus y versión comprobada: `0.1.15-prueba`, código 16; SHA-256 `CE4AA79CABCF834371DF7FE6D65B772F42F9F63B555A6A235C27ABB31C7A8E4B`. Todavía no se completó una actualización de extremo a extremo desde GitHub. La 0.1.15 se publicó posteriormente en GitHub y se verificó que el archivo remoto coincidiera con esta APK.

Referencias de Android: [compartir archivos con FileProvider](https://developer.android.com/training/secure-file-sharing/setup-sharing) y [autorización por fuente de instalación](https://android-developers.googleblog.com/2017/08/making-it-safer-to-get-apps-on-android-o.html).

## Candidato anterior: 0.1.14

La pantalla de saldo usa el gesto estándar de arrastrar hacia abajo desde el comienzo para ejecutar la misma consulta de saldo y mínimo. Se retiró el botón inferior. El gesto está deshabilitado en otras pantallas y mientras hay una consulta en curso.

Pasaron 40 pruebas JavaScript, 7 JVM, lint y compilación release. APK instalada y versión comprobada en el OnePlus: `0.1.14-prueba`, código 15. SHA-256: `ED550CC1985D39FE93D98C03F189EDBF587AECFA142BB47DE6E8CA9EAA08528B`. No se realizó un ingreso real ni una prueba del gesto en la cuenta del titular durante este cambio. Candidato local sin publicación remota.

## Candidato anterior: 0.1.13

Se reprodujo con una regresión JavaScript que la ausencia temporal del formulario se clasificaba como página original. Ahora conserva la carga nativa hasta reconocer el siguiente paso. El navegador comienza oculto y vuelve a ocultarse al navegar; las respuestas anteriores no pueden cambiar el estado de la nueva navegación.

Pasaron 40 pruebas JavaScript, 7 JVM, lint y compilación release. En Android, `PrexLoadingTest` montó la pantalla real de la app con una página ficticia magenta que demoró 3,5 segundos en presentar el resumen: seis capturas durante la espera no mostraron píxeles magenta y luego apareció el resumen. También pasó `EmbeddedPrexPaymentTest`, que comprueba la conservación de la página. Ambas pruebas interceptan la red: no iniciaron operaciones reales. Resultado local: `outputs/loading-0.1.13-test.txt`.

APK instalada en el OnePlus y versión comprobada: `0.1.13-prueba`, código 14. SHA-256: `7AB5A9F959FE321DFB4495EDC163923E1A711AD6007693A2C80792A23AF75704`. La corrección del destello todavía no se recorrió contra la pasarela real en el teléfono. El usuario pospuso los pagos reales para continuar el desarrollo. No se publicó una versión remota.

## Candidato anterior: 0.1.12

Se mejoró el panel del CAPTCHA para conservar el componente original en una posición estable y adaptar sus dimensiones a la ventana real de la app. Pasaron 39 pruebas JavaScript, 7 JVM, lint, compilación y la matriz Android del panel con contenido ficticio, más la prueba de retención de la página. La APK está instalada en el OnePlus; el checkbox real se ve completo. [Evidencia y límites](CAPTCHA-RESPONSIVO.md). La verificación humana y el pago completo siguen pendientes.

Actualización física posterior: la misma solicitud alcanzó el paso «Datos Tarjeta» de Sistarbanc, con los pasos de resumen y cliente marcados como completados. Los tres campos de tarjeta estaban vacíos. Al enfocar el número se mostraron sugerencias de tarjetas guardadas en el teclado del teléfono. Esto verifica la disponibilidad de las sugerencias en el formulario real dentro de Boletera; todavía no verifica la selección, el llenado, la huella, la autorización ni la acreditación. No se eligió ninguna tarjeta ni se pulsó Continuar en ese paso. Se dejó la selección abierta para el titular. La captura privada permanece fuera del repositorio.

## Candidato local: 0.1.11

El resumen y los datos del titular se presentan con controles nativos Android. La sesión sigue en el WebView original; el adaptador lee únicamente los pares de texto del resumen y los cinco campos ordinarios del cliente. Las acciones nativas solo avanzan desde los componentes reconocidos de resumen o cliente; no autorizan el pago.

En el OnePlus 6T con Android 11 se reabrió la misma solicitud pendiente y se comprobó el paso de resumen nativo a titular nativo, la lectura de los seis datos del resumen y la apertura/cancelación del editor nativo con cédula y pasaporte. No se creó otra solicitud ni se avanzó a la tarjeta. Se corrigieron dos defectos encontrados físicamente: el resumen usa bloques `b`/`p`, no una tabla; el panel del WebView requiere recorte explícito también en Compose.

La APK final del candidato, SHA-256 `CF6FB2720FBFA247A7F486DC9408E4193BD8293778A243E6C887BD9F43466CDB`, quedó instalada. La inspección visual final confirmó el resumen nativo y la pantalla nativa del titular con el checkbox original completo, centrado y sin franjas de la página alrededor. Se dejó la misma solicitud en ese punto para la verificación humana. Las capturas contienen datos privados y se conservan fuera del repositorio.

Pasaron 37 pruebas JavaScript, 7 JVM, lint y compilación. Las pruebas nuevas cubren pasos ocultos, doble pulsación, controles ambiguos, origen ajeno, ausencia de lectura de tarjeta, geometría del CAPTCHA y edición explícita de datos. El desafío expandido y el envío real de los datos editados no están comprobados físicamente. CAPTCHA resuelto, tarjeta/Google, autorización y acreditación continúan pendientes. Esta versión todavía es un candidato local, no una publicación nueva.

## Versión publicada anterior: 0.1.10

Agrega cédula por defecto y pasaporte en el perfil, y selección del tipo en el control original de Prex. Las pruebas cubren el selector nativo HTML, el selector Material con panel asociado que aparece después y los cambios manuales. Se conservan las limitaciones generales del recorrido de pago.

En el OnePlus 6T con Android 11 se reabrió la misma solicitud de Prex y se avanzó del resumen al formulario del cliente. La app seleccionó «Cédula de Identidad» sin tocar manualmente ese control y completó los cinco campos ordinarios con el perfil guardado antes de incorporar el tipo de documento. Se verificó la pantalla y su jerarquía; las capturas privadas no se publican. El primer intento sobre el formulario oculto no funcionaba: el adaptador ahora espera a que ese paso sea visible. Pasaporte y conservación de cambios manuales se comprobaron con fixtures, no con una operación real de pasaporte. Pasaron 33 pruebas JavaScript, 7 JVM, lint y compilación. No se completó CAPTCHA ni se autorizó un pago.

## Corrección TLS de 0.1.9

Se corrigió la cadena de confianza de Sistarbanc para Android 11. En el OnePlus 6T físico se reprodujeron los rechazos de ambos dominios y se verificó HTTPS y WebView con la configuración corregida, mediante una app de diagnóstico separada sin operación ni cuenta. [Evidencia TLS](TLS-ANDROID11.md). El recorrido de pago completo sigue pendiente.

## Base funcional: 0.1.8

Prex está integrado en una pantalla propia de Boletera que conserva la página original del proveedor. Incluye perfiles cifrados de titular, selección por cuenta, vínculo del perfil a la solicitud y reapertura sin recargar la página retenida. No lee ni guarda datos de tarjeta ni autoriza pagos por código. eBROU conserva Chrome.

Comprobaciones de esta versión:

- 31 pruebas JavaScript aprobadas: 22 del adaptador STM, 4 del candidato de extensión y 5 del adaptador de titular integrado.
- 6 pruebas JVM aprobadas. Lint terminó con 0 errores y 27 advertencias, principalmente recomendaciones de API/estilo y dependencias.
- 14 pruebas Android ejecutadas y aprobadas. El corredor enumera 15 porque incluye el sondeo biométrico, que fue omitido mediante su condición explícita de activación; no se cuenta como aprobado.
- El recorrido Android con contenido ficticio pasó desde ingreso y saldo hasta selección de perfil, nueva solicitud, pantalla de Prex, salida y reapertura. Se comprobó un único ingreso al enlace de pago, los campos ordinarios completados y ausencia de lanzamiento de Chrome para Prex.
- Dos formatos de titular probados; los campos de tarjeta y CAPTCHA permanecen sin lectura ni escritura por el adaptador. Datos distintos preexistentes requieren aplicar explícitamente el perfil elegido.
- Perfiles cifrados recuperados después de recrear su almacenamiento; registros ilegibles no se sobrescriben. Cambiar el perfil vinculado a una solicitud invalida su enlace cifrado y conserva la guardia pendiente.
- La captura del panel se realizó después de que WebView confirmó su dibujo. Una primera captura demasiado temprana aparecía vacía y fue descartada. La evidencia visual corresponde a un formulario ficticio, no a un pago real.
- La APK release usa versionCode 9 y conserva la firma de 0.1.7. No es depurable.

La [prueba pública del detector](PRUEBA-IDENTIDAD-NAVEGADOR.md) comprobó el rechazo del WebView estándar y la ausencia de ese rechazo con la identificación alternativa. Se realizó sin operación ni APIs de pago. **No verifica un formulario con operación, Google en el teléfono, CAPTCHA real, autorización bancaria ni acreditación. El objetivo completo continúa pendiente.**

## Evidencia histórica hasta 0.1.7

## Resultado

La APK release 0.1.7 protege la revisión de pagos hasta completar el ingreso y corrige el encuadre de verificaciones fuera de la vista. Conserva Google y la recuperación de Prex. El inicio real de Prex y eBROU se comprobó con 0.1.5; las comprobaciones nuevas de 0.1.6 usan datos ficticios e interceptan la apertura del navegador. **No se validó una autorización bancaria ni acreditación. No está lista para publicación general.**

| Comprobación | Resultado |
|---|---|
| Adaptador JavaScript: acceso, dinero, deuda, mínimo variable, tarjetas, origen, privacidad, CAPTCHA y límite de pago | 22 pruebas aprobadas; incluye formularios superpuestos, traspaso de identidad, selección de celda, importe mediante el control numérico de STM y medios de pago |
| Modelos JVM: importes exactos, mínimo, política de navegación y origen de respuestas durante transiciones | 6 pruebas aprobadas, incluyendo destinos y formulario de pago |
| Compilación desde clon público limpio de GitHub (`a6724d5`) | APK debug, 3 pruebas JVM, lint y 11 pruebas JS aprobados. Sin `local.properties` ni claves del proyecto; se usaron JDK/SDK instalados y caché de dependencias de la PC |
| Emulador Android 16: arranque nativo, fixture en WebView, ausencia de guardado plano sin biometría, capturas habilitadas y recorrido offline usando StmEngine con login en URL protegida y tabla demorada hasta saldo/mínimo | 10 pruebas locales aprobadas; también preferencias por cuenta, guardia pendiente, traspaso eBROU y recuperación cifrada de Prex |
| Entrada pública real de STM desde WebView | Aprobada en Android 16 tras corregir la cadena TLS incompleta y reconocer la descripción incluida en el botón de identidad. Solo navegación pública; sin enviar documento ni contraseña |
| Login real con credenciales del usuario en la APK | Comprobado en emulador Android 16 con 0.1.5. Las capturas aportadas por el usuario de 0.1.6 muestran saldo, mínimo, confirmación y avance al resumen y formulario del cliente de Prex en su teléfono; no muestran el ingreso completo ni un pago autorizado |
| CAPTCHA real completo dentro del recorte | No validado |
| Guardar y descifrar con biometría | Cifrado, recuperación tras recrear la actividad, cancelación y eliminación aprobados con el sensor simulado del emulador Android 16 y Android Keystore. Huella física pendiente |
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

## Prueba adicional de biometría sobre 0.1.7

`BiometricVaultTest.encryptedRoundTripCancellationAndForget` aprobó en Android 16 en 53,624 segundos. Usa `AccessVault` sin sustituir el diálogo biométrico, el cifrado ni Android Keystore. Se enroló una huella simulada en los ajustes del emulador y se autentificaron dos operaciones con el sensor del emulador: guardar y descifrar. Entre ambas se recreó la actividad. El test comprobó los datos ficticios recuperados y que las preferencias persistidas contienen únicamente IV y texto cifrado. Esto no prueba un reinicio completo del proceso o del teléfono.

En un tercer diálogo se pulsó **Cancelar**: el callback no devolvió credenciales y conservó el acceso guardado. Después se eliminó el acceso y un intento de descifrado devolvió datos nulos. El test no llama a STM ni usa credenciales del usuario. Se quitó el PIN temporal al finalizar y se verificó que el emulador quedó sin huellas enroladas. No hubo cambios al código de la app ni a la APK publicada.

Es una prueba optativa que necesita interacción con un emulador preparado; no se presenta como un test automático aprobado por omisión. Con los APK debug y androidTest instalados, ejecutar:

```powershell
adb -s emulator-5554 shell am instrument -w -r -e class uy.boletera.prueba.BiometricVaultTest -e biometricProbe true uy.boletera.prueba.test/androidx.test.runner.AndroidJUnitRunner
```

Esperar el diálogo y la fase `SAVE_TOUCH`, simular la huella enrolada; repetir en `UNLOCK_TOUCH`; en `CANCEL_PROMPT`, pulsar **Cancelar**. El resultado debe terminar en `OK (1 test)` y fase `COMPLETE`. Sin el argumento `biometricProbe`, se omite. Preparación y comandos del sensor: [documentación oficial del emulador Android](https://developer.android.com/studio/run/emulator-console).

Siguen pendientes la biometría física, el CAPTCHA real, el autocompletado de Google en el formulario bancario, la recuperación de un enlace real de Prex y la autorización/acreditación del pago. Las capturas recibidas de 0.1.6 confirman avance hasta el formulario de cliente; no prueban esas etapas restantes y no se publican.

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
