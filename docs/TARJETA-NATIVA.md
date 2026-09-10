# Formulario nativo de tarjeta — 0.2.9

Implementado por pedido del dueño, priorizando seguridad y practicidad. El paso normal muestra número, vencimiento y CVV en Compose, dentro de Boletera. Los datos se transfieren al formulario ya abierto de Sistarbanc únicamente después de un toque explícito en Continuar. No se utiliza una API de pagos propia ni un SDK bancario.

## Procesamiento y compatibilidad

El adaptador exige el origen HTTPS de Sistarbanc, ruta `/v2/`, documento principal y un único componente `stepper-pago alta-tarjeta` visible. Debe tener exactamente los tres campos conocidos y un solo Continuar. Se conservan los controles Angular originales y se emiten sus eventos; se comprueba la validación antes de hacer un único clic original. No hay acceso a tokens ni HTTP financiero escrito por Boletera.

La estructura se contrastó mediante GET público sin cuenta ni operación el 10 de septiembre de 2026: [archivo enlazado por la pasarela](https://pasarelaspe.sistarbanc.com.uy/v2/main-es2015.ba3be1cce732d7895caf.js), SHA-256 de la copia analizada `23f1080350a236fd6eddfca561a2a7cc672a7fef2b9b10f806e92cfaffbb0660`. Sus límites actuales son 13–16 dígitos de número, vencimiento MM/AA vigente y CVV de 3–4 dígitos. El formulario nativo también comprueba Luhn.

Si aparecen otras entradas o casillas de consentimiento, un formulario diferente o un diálogo de autorización, se muestra la página original. Nunca se aceptan esas opciones por omisión. El CAPTCHA original puede presentarse recortado, sin copiar ni resolver su contenido. Ver página original permanece accesible. Si el resultado de un envío no se reconoce, no hay reintento por tiempo: se conserva la página para que el usuario la revise.

## Tratamiento de los datos

- Estados locales `remember`, sin `rememberSaveable`, preferencias, archivos, ViewModel ni registro de valores. Las capturas de estado del adaptador devuelven indicadores y geometría, nunca PAN, vencimiento o CVV.
- El CVV se oculta visualmente, no tiene indicación de autocompletado y se vacía del formulario nativo tras enviar. Número/vencimiento quedan temporalmente para una corrección si el proveedor rechaza; todos los campos nativos se vacían al salir, desmontarse o recibir ON_STOP.
- El autocompletado usa indicaciones de Android para número y vencimiento. Se cancela la sesión antes de enviar, salir o vaciar; no se solicita guardar. No se cambia el proveedor de autocompletado configurado por el usuario. [Comportamiento de Compose](https://developer.android.com/develop/ui/compose/text/autofill).
- `FLAG_SECURE` protege el panel de pago frente a capturas y miniaturas; al cerrarlo se restaura la política anterior. WebView no guarda estado del formulario; sus mensajes de consola no se publican en registros de Boletera.
- La página de Sistarbanc conserva su propio estado durante el procesamiento y una eventual autorización adicional. Borrarlo prematuramente rompería esa operación. La limpieza descrita es de las copias nativas; no promete borrado forense de cadenas inmutables ni control sobre sistemas de terceros.

## Evidencia y límites

Pruebas locales del adaptador cubren validación sin envío, transferencia por eventos, un solo clic, rechazo de origen ajeno/controles extra, errores Angular y preservación del bloqueo durante autorización adicional. Una prueba Android utiliza el WebView y la pantalla reales con HTML interceptado y una tarjeta ficticia; comprueba entrega exacta, un solo envío y ausencia de valores bancarios en el estado leído. Otras comprueban limpieza al enviar, salir y pasar al fondo, y activación/restauración de protección de pantalla.

Esto prueba el puente y la interfaz contra una representación del formulario observado. No demuestra aceptación de una tarjeta real, autocompletado de Google en el teléfono, CAPTCHA real, autorización bancaria o acreditación. No se hizo ninguna operación financiera para esta versión.
