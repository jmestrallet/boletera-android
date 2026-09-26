# Mejora integral de Boletera y Carga Express

Objetivo del dueño: mejorar la aplicación en general y conseguir un Express realmente rápido, investigando también las alternativas para el CAPTCHA. Este objetivo sigue abierto; la primera entrega no equivale a terminarlo.

## Primera entrega: 0.2.34 Beta 1

- Express reacciona a los cambios del formulario original sin esperar cada consulta periódica de Android. Conserva el bloqueo de un solo envío por paso.
- La validación asíncrona de los datos del titular puede terminar sin expulsar prematuramente al usuario al modo manual. Un error, dato distinto, consentimiento desconocido o demora excesiva sigue deteniendo el avance.
- El autocompletado se solicita al campo virtual de número de tarjeta mediante la API de nodos de Compose. La versión anterior lo solicitaba al contenedor de la pantalla.
- El botón **Tarjetas guardadas** permite volver a solicitar el servicio Android desde la pantalla nativa.
- Al ocultar el pago o enviar la app al fondo se suspende también el observador del formulario. Reabrir conserva la misma solicitud.

## Evidencia

- 65 pruebas JavaScript aprobadas, incluidas navegación por cambios del documento, validación demorada, suspensión, errores y prevención de duplicados.
- 10 pruebas JVM y lint aprobados para los cambios de producción.
- 11 casos Android dirigidos aprobados: autocompletado completo/parcial, pausa, cambio de tarjeta, confirmación y vuelta al saldo, disponibilidad de Express y validación demorada con el panel oculto.
- Un caso adicional usa un servicio Android de autocompletado real instalado exclusivamente en el APK de pruebas. Recibe los tres tipos de tarjeta y confirma que las solicitudes explícitas de llegada y reintento apuntan al número de tarjeta. No entrega, lee ni guarda datos de tarjeta.
- Esto no demuestra que Google ofrezca las tarjetas guardadas en todos los dispositivos: la oferta y autenticación dependen del proveedor del usuario.

## Investigación CAPTCHA

Google documenta reCAPTCHA v2, verificación invisible y callbacks de resolución/expiración. La selección y configuración pertenecen al sitio que verifica la operación. No hay evidencia en esta revisión de que Boletera pueda cambiar la configuración de gub.uy o Sistarbanc. Tampoco se identificó una API pública de recarga STM con tokenización delegada que permita reemplazar el recorrido de Prex.

No se presenta un botón habilitado, `ng-valid` o el cierre de un desafío como prueba de resolución: el sitio puede habilitar Continuar antes de verificarlo. La investigación de una continuación confiable después de la intervención humana sigue pendiente. No se han integrado proveedores externos de resolución ni transferido sesiones o datos de pago a terceros.

Fuentes consultadas el 26/9/2026:

- [Autocompletado en Compose](https://developer.android.com/develop/ui/compose/text/autofill).
- [AutofillManager y solicitudes a campos virtuales](https://developer.android.com/reference/android/view/autofill/AutofillManager).
- [reCAPTCHA v2: callbacks y configuración](https://developers.google.com/recaptcha/docs/display).
- [Tipos de reCAPTCHA](https://developers.google.com/recaptcha/docs/versions).
- [STM en línea](https://montevideo.gub.uy/stm-en-linea).

## Trabajo pendiente para cumplir el objetivo completo

1. Medir tiempos y cantidad de intervenciones en cada etapa; distinguir demoras introducidas por Boletera de red/proveedor. No prometer cinco segundos a partir de pruebas sintéticas.
2. Revisar el acceso y la recuperación de sesión para evitar autenticaciones repetidas cuando existe una sesión válida, conservando la separación de cuentas.
3. Mejorar la presentación y recuperación del CAPTCHA y comprobar qué señales ofrece el flujo real después de resolverlo.
4. Revisar con capturas los estados de espera, errores, reanudación y el formulario de tarjeta en pantalla chica, teclado abierto y letra grande.
5. Validar el autocompletado y el recorrido real en el teléfono del dueño. Las pruebas de laboratorio no sustituyen esa comprobación.
6. Publicar cada entrega Beta con rama, tag, APK y hash remoto verificados. Mantener explícito qué está probado y qué queda pendiente.

## Segunda entrega: 0.2.35 Beta 1

- Se reprodujo en WebView una preparación de recarga pendiente: tres lecturas de la pantalla anterior bastaban para perder `busy`. Se conserva ahora hasta la respuesta o el error. Se cubren Express y recarga común; un intento de preparar otro importe mientras espera tampoco borra la selección Express.
- Un CAPTCHA visible o un componente de verificación todavía cargando impide el envío automático de la tarjeta. El autocompletado completo oculta el teclado y desplaza la verificación a la vista. El botón Continuar queda disponible para después de la intervención humana.
- El error de tarjeta ofrece «Revisar respuesta», incluso cuando el proveedor no habilita Continuar. Antes podía mostrar un botón sin efecto.

### Evidencia nueva sobre CAPTCHA (26/9/2026)

Se leyó el [JavaScript público actual de Sistarbanc](https://pasarelaspe.sistarbanc.com.uy/v2/main-es2015.c4dd4374250f3678bcc8.js), sin abrir una solicitud de pago. SHA-256 de la copia local: `bbc0e8291f932f91f1f3dbbea3bdb7adae4332b6af990027823089b0ca3f497a`.

El componente `angular-recaptcha` recibe el resultado de Google y realiza después `autenticationService.login("recaptcha", respuesta)`. Solo tras esa respuesta establece la autenticación y emite éxito. En `alta-cliente` el método `resolvedCaptcha` está vacío; el botón usa `disableButton`, sin exponer allí un resultado verificable de CAPTCHA. Esto refuerza que leer una respuesta de Google o ver el botón habilitado no bastaría para continuar con confianza. No se incorporaron lecturas de tokens ni sustituciones de callbacks. La continuación automática después de resolverlo sigue pendiente de una señal fiable del proveedor.
