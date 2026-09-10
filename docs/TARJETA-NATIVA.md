# Formulario nativo de tarjeta — revisión 0.2.22

Implementado por pedido del dueño, priorizando seguridad y practicidad. El paso normal muestra número, vencimiento y CVV en Compose, dentro de Boletera. Los datos se transfieren al formulario ya abierto de Sistarbanc únicamente después de un toque explícito en Continuar. No se utiliza una API de pagos propia ni un SDK bancario.

## Procesamiento y compatibilidad

El adaptador exige el origen HTTPS de Sistarbanc, ruta `/v2/`, documento principal y un único componente `stepper-pago alta-tarjeta` visible. Debe tener exactamente los tres campos conocidos y un solo Continuar. Se conservan los controles Angular originales y se emiten sus eventos; se comprueba la validación antes de hacer un único clic original. No hay acceso a tokens ni HTTP financiero escrito por Boletera.

La estructura se contrastó mediante GET público sin cuenta ni operación el 10 de septiembre de 2026: [archivo enlazado por la pasarela](https://pasarelaspe.sistarbanc.com.uy/v2/main-es2015.ba3be1cce732d7895caf.js), SHA-256 de la copia analizada `23f1080350a236fd6eddfca561a2a7cc672a7fef2b9b10f806e92cfaffbb0660`. Sus límites actuales son 13–16 dígitos de número, vencimiento MM/AA vigente y CVV de 3–4 dígitos. El formulario nativo también comprueba Luhn.

Si aparecen otras entradas o casillas de consentimiento, un formulario diferente o un diálogo de autorización, se muestra la página original. Nunca se aceptan esas opciones por omisión. El CAPTCHA original puede presentarse recortado, sin copiar ni resolver su contenido. Ver página original permanece accesible. Si el resultado de un envío no se reconoce, no hay reintento por tiempo: se conserva la página para que el usuario la revise.

## Tratamiento de los datos

- Estados locales `remember`, sin `rememberSaveable`, preferencias, archivos, ViewModel ni registro de valores. Las capturas de estado del adaptador devuelven indicadores y geometría, nunca PAN, vencimiento o CVV.
- El CVV se oculta visualmente, desde 0.2.22 tiene indicación de autocompletado CreditCardSecurityCode y se vacía del formulario nativo tras enviar. Número/vencimiento quedan temporalmente para una corrección si el proveedor rechaza; todos los campos nativos se vacían al salir, desmontarse o recibir ON_STOP.
- El autocompletado usa indicaciones de Android para número, vencimiento y código de seguridad. Se cancela la sesión antes de enviar, salir o vaciar; no se solicita guardar. No se cambia el proveedor de autocompletado configurado por el usuario. [Comportamiento de Compose](https://developer.android.com/develop/ui/compose/text/autofill).
- Desde 0.2.12 se permiten capturas también durante el pago, por pedido del usuario para reportar errores. Se retiró el bloqueo que se había agregado en 0.2.9. WebView no guarda estado del formulario; sus mensajes de consola no se publican en registros de Boletera.
- La página de Sistarbanc conserva su propio estado durante el procesamiento y una eventual autorización adicional. Borrarlo prematuramente rompería esa operación. La limpieza descrita es de las copias nativas; no promete borrado forense de cadenas inmutables ni control sobre sistemas de terceros.

## Evidencia y límites

Pruebas locales del adaptador cubren validación sin envío, transferencia por eventos, un solo clic, rechazo de origen ajeno/controles extra, errores Angular y preservación del bloqueo durante autorización adicional. Una prueba Android utiliza el WebView y la pantalla reales con HTML interceptado y una tarjeta ficticia; comprueba entrega exacta, un solo envío y ausencia de valores bancarios en el estado leído. Otras comprueban limpieza al enviar, salir y pasar al fondo. Desde 0.2.12 se comprueba además que el pago permite capturas y que los desafíos altos conservan visibles y utilizables consigna y pie, sin recrear sus iframes.

Esto prueba el puente y la interfaz contra una representación del formulario observado. No demuestra aceptación de una tarjeta real, autocompletado de Google en el teléfono, CAPTCHA real, autorización bancaria o acreditación. No se hizo ninguna operación financiera para esta versión.

## Pulido de 0.2.22

Siguiente y Continuar buscan el primer campo inválido, lo enfocan, muestran la explicación y desplazan el formulario para conservarlo visible sobre el teclado. Si número y vencimiento ya están completos, el destino es CVV. Siguiente no envía los datos; Continuar conserva el envío explícito y único.

Un botón reactivado durante el procesamiento no basta para inferir rechazo. El adaptador exige un aviso de error visible y persistente por dos segundos; una advertencia que desaparece durante la transición no desbloquea otro envío. Una espera larga permite revisar la página existente, sin reintentar.

Google documenta el guardado opcional de códigos de seguridad en Chrome: [ayuda Android](https://support.google.com/chrome/answer/142893?co=GENIE.Platform%3DAndroid&hl=en). Eso no garantiza que el servicio de autocompletado los entregue en una app nativa. Se usa la [indicación oficial de Android](https://developer.android.com/reference/kotlin/androidx/compose/ui/autofill/ContentType#CreditCardSecurityCode()) y se mantiene ingreso manual. No se cambió la configuración de Google ni se guardó un CVV desde Boletera. Las pruebas comprueban datos parciales ficticios, foco y teclado; la entrega real de Google queda por verificar en el teléfono.
