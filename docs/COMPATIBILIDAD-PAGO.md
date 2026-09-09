# Compatibilidad de pago — 9 de septiembre de 2026

## Conclusión actual

La APK 0.1.1 entregada permite probar el acceso nativo y el autocompletado local. No está demostrado el pago de una recarga STM con formulario propio. No se habilitará una solicitud de pago a partir de un autocompletado exitoso: son comprobaciones distintas.

## Evidencia consultada

Google documenta el autocompletado de información de pago dentro de apps Android y una preferencia para exigir biometría antes de completar tarjetas. Eso respalda el mecanismo general que usa la prueba local. No garantiza que una tarjeta guardada solo en Chrome esté disponible en el servicio configurado en un teléfono concreto, ni que la huella esté activada allí.

- [Google: información de pago automáticamente en apps](https://support.google.com/googlepay/answer/9215533?hl=en).
- [Android: preparar una app para autocompletado](https://developer.android.com/identity/autofill/autofill-optimize).

La documentación pública de Fenicio para su propia integración con Sistarbanc requiere conexión del comercio y un ID Organismo. Es evidencia de una integración comercial de Fenicio, no de una API disponible para una app personal que recargue STM. La búsqueda realizada no encontró un contrato público de API/SDK que permita afirmar esa compatibilidad para esta app. Ausencia de documentación encontrada no demuestra imposibilidad.

- [Fenicio: integración con Sistarbanc](https://guia.fenicio.help/integraciones/integradores/sistarbanc).

## Verificación pendiente y orden acordado

1. En el Android físico del usuario: ingreso completo, CAPTCHA original dentro del panel y lectura del saldo/mínimo. La prueba automática actual solo llega al acceso público.
2. Guardar y recuperar el acceso con biometría real, cancelar y olvidar el acceso. El emulador sin biometría solo verificó que no se guarde en texto plano como alternativa.
3. En la prueba local de autocompletado: observar si se ofrece la tarjeta y qué autenticación pide Android. No enviar números de tarjeta ni capturas con datos al desarrollador.
4. Con el acceso anterior validado: comprobar el recorrido real del proveedor sin pagar automáticamente. Se necesita evidencia de los campos, verificaciones, sesión e identificación de operación antes de programar su envío. Una página completa o un navegador externo que resulte obligatorio debe reportarse antes de cambiar la experiencia acordada.
5. Solo entonces implementar los estados de pago pendiente, confirmado y recarga acreditada, con reconciliación antes de reintentar y confirmación del usuario para el pago.

## Auditoría del objetivo

| Requisito | Evidencia actual | Estado |
|---|---|---|
| Interfaz propia y motor web separado | Código Compose/WebView y prueba de arranque Android | Implementado |
| Login completo sin páginas completas | Sondeo público Android aprobado; no login completo en APK | Pendiente |
| CAPTCHA original acotado e interactivo | Detector y recorte implementados; pruebas de geometría | Pendiente de desafío real |
| Saldo, boletera operativa y mínimo de STM | Adaptador probado con fixtures; sin lectura autenticada en APK | Pendiente de validación real |
| Credenciales cifradas con huella | Android Keystore implementado; alternativa plana rechazada en emulador | Pendiente de hardware |
| Tarjeta guardada y huella en formulario propio | Mecanismo documentado por Google; prueba local incluida | Pendiente del teléfono |
| Pago propio y estados sin duplicados | Motor se detiene antes de generar solicitud | Sin implementar |
| APK privada para probar | Versión 0.1.1, 7.900.750 bytes, entrega privada verificada | Entregada |

La entrega del archivo no cierra el objetivo. El siguiente dato decisivo es el resultado del ingreso y CAPTCHA en el teléfono, tal como establece el plan aprobado antes de validar el pago.
