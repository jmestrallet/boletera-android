# Perfiles para Prex: viabilidad y decisión del usuario

Fecha: 9 de septiembre de 2026. Investigación realizada sobre 0.1.5. El usuario eligió **conservar Google**; el servicio propio no se incorpora a 0.1.6.

**Aclaración posterior:** conservar Google no equivale a aceptar que el usuario complete la página. El objetivo vigente es realizar la recarga desde la interfaz propia. Hay un hallazgo nuevo sobre asociación y tarjetas guardadas en el proveedor, todavía sin habilitación comprobada para STM/Prex: [investigación nativa](INTEGRACION-NATIVA-PAGO.md).

## Necesidad

Recordar los datos del titular para una próxima recarga, distinguiendo cada perfil de Prex de la cuenta STM y de la boletera. Elegir otra Prex debe pedir datos nuevos y no reutilizar silenciosamente los de otra persona. Un perfil de datos no verifica la titularidad de una tarjeta bancaria.

En la investigación previa se observaron datos de documento, titular, correo, teléfono y CAPTCHA. No se completó la autorización posterior y no se afirma que esa lista cubra todo el pago. No se incluyen datos reales del usuario en esta documentación, los fixtures ni el código.

## Ruta actual

La APK abre el pago en una Custom Tab de Chrome. Ese mecanismo no permite a Boletera inyectar JavaScript, acceder libremente al documento o trasladar solamente su CAPTCHA a la interfaz propia. Prex rechazó el WebView en las pruebas anteriores.

Esto no significa que Android carezca de autocompletado: un `AutofillService` elegido expresamente por el usuario puede ofrecer conjuntos de datos a Chrome. Es una integración distinta, con consecuencias fuera de Boletera.

## Consecuencia para el usuario

Chrome permite elegir Google o un servicio externo como fuente del autocompletado. Si el usuario activa otro servicio, los datos de autocompletado pasan a provenir de ese servicio en lugar de Google. Implementar un servicio que solo cubra Prex puede quitarle las sugerencias habituales de Google en otras páginas.

El usuario eligió conservar Google. La propuesta de servicio propio que sigue queda descartada para este tramo; no se activará en su teléfono como parte de una actualización ordinaria.

## Propuesta concreta si se elige el servicio opcional

- Crear, editar y eliminar perfiles con nombre visible; mantenerlos separados por cuenta STM.
- Guardar los datos cifrados localmente. No incorporar los datos de la conversación como valores iniciales de una APK pública.
- Recordar el perfil seleccionado; «Usar otra Prex» comienza con campos vacíos.
- Mostrar claramente el perfil y el destino al ofrecer el relleno. La sugerencia necesita selección del usuario y no envía ni confirma el formulario.
- Limitar el servicio a Chrome auténtico y a los orígenes de pago verificados. Rechazar dominios, campos y formularios desconocidos; no extraer credenciales de otras apps.
- No guardar CVV ni resolver automáticamente CAPTCHA. El desafío y la autorización permanecen en el proveedor.
- Explicar el cambio de autocompletado antes de abrir los ajustes de Android/Chrome; permitir desactivarlo y conservar el recorrido manual.

## Prueba técnica de esta investigación

Se construyó un APK de laboratorio separado de Boletera, con dos perfiles ficticios y sin persistencia de datos. El servicio solo reconoce un formulario local de cuatro campos, no un sitio bancario. Compiló correctamente con SDK 35.

El emulador disponible tiene Android 16 y Chrome 133.0.6943.137. La ruta de ajustes anunciada por Google para Chrome 135 no se resolvió en esa versión. Además, la revisión automática bloqueó abrir el formulario local mediante el comando de lanzamiento de Chrome, sin aportar un motivo específico. Por tanto, **no se comprobó el rellenado en Chrome, el cambio entre perfiles, ni el funcionamiento con Prex real**. El prototipo no se incluye en la APK distribuida.

Para validar una implementación futura hace falta Chrome compatible, comprobar la selección de ambos perfiles con datos ficticios y después verificar los campos reales de Prex sin autorizar pagos automáticamente. La compatibilidad de un fixture no acredita compatibilidad bancaria.

## Fuentes oficiales consultadas

- [Chrome: límites de acceso de Custom Tabs](https://chromium.googlesource.com/chromium/src/+/main/docs/security/custom-tabs-faq.md).
- [Android: servicio de autocompletado y elección del usuario](https://developer.android.com/identity/autofill/autofill-services).
- [Android: verificación del navegador y dominio de destino](https://developer.android.com/reference/android/service/autofill/AutofillService).
- [Google: soporte de servicios externos desde Chrome 135 y acceso a ajustes](https://android-developers.googleblog.com/2025/02/chrome-3p-autofill-services-update.html).
- [Ayuda de Chrome: elegir Google u otro proveedor de autocompletado](https://support.google.com/chrome/answer/142893?co=GENIE.Platform%3DAndroid&hl=es-419).
