# Pago sin navegar por la web: investigación

Fecha: 9 de septiembre de 2026. Estado: viabilidad parcial; integración no implementada.

## Objetivo vigente

El usuario aclaró que quiere elegir el monto y confirmar en Boletera, sin rellenar ni navegar la página de Prex/Sistarbanc. Conservar Google no significa aceptar el formulario web como resultado final. La APK 0.1.7 no cumple ese objetivo. Sus pruebas de acceso y traspaso siguen siendo válidas, pero no acreditan un pago nativo.

## Hallazgo nuevo en el proveedor

Se descargaron, mediante GET sin sesión ni identificador de transacción, el HTML público de la pasarela y el archivo JavaScript enlazado por ese HTML. Ambos respondieron HTTP 200. No se inició ninguna recarga ni se consultaron clientes, tarjetas u operaciones.

El código público contempla:

- Mostrar una opción de tarjeta guardada según `permite_recordar_tarjeta`.
- Mostrar una autorización recurrente según `permite_pagos_recurrentes`.
- Buscar tarjetas previamente asociadas cuando la solicitud incluye un token.
- Un recorrido de asociación separado, ligado a un servicio, cliente y token, que puede precargar el formulario con los datos del cliente.

Esto demuestra que la plataforma tiene recorridos previstos para reutilizar datos y tarjetas. **No demuestra que STM con Prex los tenga habilitados, que estén disponibles para esta app, ni que permitan evitar toda pantalla web.** No se cambió ningún indicador, no se obtuvieron tokens y no se invocaron los servicios de asociación o de pago. Los indicadores provienen del pago devuelto por el proveedor: cambiarlos visualmente no habilita una integración.

El mismo archivo incluye una protección explícita contra WebView e iframe, ejecutada al iniciar el recorrido de confirmación. La denegación observada antes sigue contemplada por el código actual. En la investigación inicial no se modificó la identidad del navegador; la prueba posterior autorizada se documenta debajo.

## Evaluación de las rutas

La revisión adicional del código separa dos requisitos: la referencia de asociación recibida en la solicitud de pago permite buscar tarjetas guardadas; la sesión de autenticación de la pasarela se valida por separado contra el servidor. Tener una tarjeta asociada no acredita una sesión válida ni elimina por sí solo el CAPTCHA. Si la validación de sesión falla, el recorrido vuelve a requerir la verificación.

No hay una configuración estática en el archivo revisado que demuestre que STM habilita esas funciones para Prex: los indicadores llegan con la respuesta de una operación. Las capturas muestran un resumen y el formulario, pero no esos indicadores ni la referencia de asociación. No se usó el número visible de cliente como supuesto identificador de servicio, ni se enumeraron operaciones o clientes para suplir esa información. Por tanto, esta revisión no permite implementar honestamente un pago nativo completo.

| Ruta | Evidencia y límite |
|---|---|
| Conservar el enlace original | Ya implementado. Recupera la solicitud, pero no completa datos ni autorización. |
| Rellenar Chrome desde la app actual | Custom Tabs no permite inyección arbitraria ni acceso completo a los campos. Google conserva sus sugerencias, no ejecuta un perfil propio de Boletera. |
| Copiar el formulario a nuestra interfaz | No resuelve la autenticación de la pasarela. No es una integración validada. |
| Asociación del titular y tarjeta en el proveedor | Existe código público para ese recorrido; faltan confirmación de soporte STM/Prex y documentación de integración. |
| Pago nativo con autorización vinculada al titular | Es la ruta que corresponde investigar para el objetivo. No hay una API/SDK pública localizada que pruebe su disponibilidad para este caso. |

El CAPTCHA no se transforma en un dato guardable por copiar el enlace. La documentación de Google describe respuestas de un solo uso y duración limitada. No se completó ni se automatizó ningún desafío en esta investigación.

## Restricción vigente: investigación por nuestra cuenta

El usuario prohíbe contactar a cualquier persona u organización. No enviar correos, formularios de contacto, consultas de soporte ni solicitudes de habilitación. No se envió la consulta antes propuesta; esa vía queda descartada.

La investigación queda limitada a información pública, análisis local y pruebas dentro del acceso ya autorizado. Hay que demostrar por esos medios si la asociación de titular/tarjeta está disponible para STM con Prex y si permite una interfaz nativa. Su existencia genérica no es prueba de disponibilidad para esta aplicación. Si no puede comprobarse, registrar la limitación sin presentar el pago web como cumplimiento del objetivo.

## Evidencia y fuentes

- [HTML público de la pasarela](https://pasarelaspe.sistarbanc.com.uy/v2/seleccionBanco), consultado sin parámetro de operación.
- [JavaScript público enlazado por la pasarela](https://pasarelaspe.sistarbanc.com.uy/v2/main-es2015.ba3be1cce732d7895caf.js). La copia local de investigación queda excluida de Git; SHA-256 del archivo local guardado: `eebaebf0066a8b789693bb8ff964820727bfd95403e8e7160e3feca7978f7e1b`. Este hash identifica la copia local, no un binario de Boletera.
- [Sistarbanc SPE: aplicaciones participantes y contacto](https://www.e-sistarbanc.com.uy/contenido/ct_13/es/).
- [Chrome: límites de Custom Tabs](https://chromium.googlesource.com/chromium/src/+/refs/heads/main/docs/security/custom-tabs-faq.md).
- [Google: validación de reCAPTCHA](https://developers.google.com/recaptcha/docs/verify).
- [Catálogo público de Montevideo API](https://api.montevideo.gub.uy/docs): transporte expone posiciones y tiempos de arribo; el catálogo consultado no publica recargas.

## Candidato posterior: extensión de navegador

La propuesta de aplicar CSS sobre la página original tiene una vía investigable mediante Firefox Android y un gestor de scripts disponible públicamente. Se preparó el [candidato local](../experiments/pago-apariencia/README.md), con su alcance, pruebas y límites. No se verificó todavía sobre Sistarbanc ni demuestra que se pueda eliminar toda intervención al pagar.

## Prueba posterior autorizada: identificación del navegador

Se compararon tres identificaciones en un WebView Android sin red y dos contra la pantalla pública real, sin operación. La variante alternativa dejó de activar el rechazo inicial por navegador tanto en los predicados locales como en la pantalla pública. El [informe de resultados](PRUEBA-IDENTIDAD-NAVEGADOR.md) delimita la evidencia: no demuestra que se pueda completar una recarga, resolver CAPTCHA ni autorizar el pago.

Tras la investigación, 0.1.8 incorpora Prex dentro de Boletera, con apariencia, perfiles de titular cifrados y conservación de la página al salir y volver. Las pruebas de integración usan datos ficticios. Falta comprobar el formulario con una operación real, Google en el teléfono, CAPTCHA, autorización y acreditación. El objetivo completo sigue pendiente.

## Implementación posterior: formulario nativo 0.2.9

El dueño autorizó incorporar número, vencimiento, CVV y Continuar en la interfaz de la app. La [implementación y sus límites](TARJETA-NATIVA.md) conservan el formulario original como procesador: no son una API/SDK independiente ni prueban aceptación bancaria. El puente se contrastó con el código público actualizado y se probó en WebView con HTML interceptado. Esta implementación reemplaza la situación anterior de mostrar siempre el paso de tarjeta original; autorizaciones, controles desconocidos y consentimientos adicionales siguen apareciendo en la página del proveedor.
