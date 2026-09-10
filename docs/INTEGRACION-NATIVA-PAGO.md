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

El mismo archivo incluye una protección explícita contra WebView e iframe, ejecutada al iniciar el recorrido de confirmación. La denegación observada antes sigue contemplada por el código actual. No se intentó desactivar esa protección ni sustituir la identidad del navegador.

## Evaluación de las rutas

| Ruta | Evidencia y límite |
|---|---|
| Conservar el enlace original | Ya implementado. Recupera la solicitud, pero no completa datos ni autorización. |
| Rellenar Chrome desde la app actual | Custom Tabs no permite inyección arbitraria ni acceso completo a los campos. Google conserva sus sugerencias, no ejecuta un perfil propio de Boletera. |
| Copiar el formulario a nuestra interfaz | No resuelve la autenticación de la pasarela. No es una integración validada. |
| Asociación del titular y tarjeta en el proveedor | Existe código público para ese recorrido; faltan confirmación de soporte STM/Prex y documentación de integración. |
| Pago nativo con autorización vinculada al titular | Es la ruta que corresponde investigar para el objetivo. No hay una API/SDK pública localizada que pruebe su disponibilidad para este caso. |

El CAPTCHA no se transforma en un dato guardable por copiar el enlace. La documentación de Google describe respuestas de un solo uso y duración limitada. No se completó ni se automatizó ningún desafío en esta investigación.

## Consulta técnica preparada, no enviada

Destino publicado por Sistarbanc para consultas SPE: `spe@sistarbanc.com.uy`.

> Estamos evaluando una aplicación Android independiente para recargar STM con Prex. Queremos que el titular registre sus datos una vez y confirme cada recarga en nuestra interfaz, sin navegar el formulario web, manteniendo la autenticación que exija el proveedor. La pasarela pública contempla asociación de cliente/tarjeta, tarjetas recordadas y autorización recurrente. ¿Están disponibles esas capacidades para STM con Prex y para una aplicación de terceros? ¿Existe API o SDK para el alta inicial, autorización de cada pago y consulta de su resultado? Necesitamos saber qué habilitación requiere STM, quién emite las credenciales/tokens, si alguna etapa exige obligatoriamente su web y si hay un entorno de pruebas. No buscamos dar de alta otro comercio para cobrar ventas propias ni activar recargas periódicas.

No se envió un mensaje ni se solicitó un alta externa. Un convenio de cobranza para un comercio nuevo no acredita permiso para liquidar solicitudes existentes de STM.

## Evidencia y fuentes

- [HTML público de la pasarela](https://pasarelaspe.sistarbanc.com.uy/v2/seleccionBanco), consultado sin parámetro de operación.
- [JavaScript público enlazado por la pasarela](https://pasarelaspe.sistarbanc.com.uy/v2/main-es2015.ba3be1cce732d7895caf.js). La copia local de investigación queda excluida de Git; SHA-256 del archivo local guardado: `eebaebf0066a8b789693bb8ff964820727bfd95403e8e7160e3feca7978f7e1b`. Este hash identifica la copia local, no un binario de Boletera.
- [Sistarbanc SPE: aplicaciones participantes y contacto](https://www.e-sistarbanc.com.uy/contenido/ct_13/es/).
- [Chrome: límites de Custom Tabs](https://chromium.googlesource.com/chromium/src/+/refs/heads/main/docs/security/custom-tabs-faq.md).
- [Google: validación de reCAPTCHA](https://developers.google.com/recaptcha/docs/verify).
- [Catálogo público de Montevideo API](https://api.montevideo.gub.uy/docs): transporte expone posiciones y tiempos de arribo; el catálogo consultado no publica recargas.

No se modificó la APK durante esta investigación. No hay una nueva versión que cumpla el objetivo nativo.
