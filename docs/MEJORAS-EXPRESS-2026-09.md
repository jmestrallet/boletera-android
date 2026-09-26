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

La ampliación pedida por el dueño revisa foros, clic automático, Buster y modelos de imágenes: [alternativas concretas y experimento propuesto](CAPTCHA-ALTERNATIVAS.md). Que la versión actual requiera intervención no significa que automatizar sea imposible. No hay todavía una tasa de éxito medida en Boletera.

Google documenta reCAPTCHA v2, verificación invisible y callbacks de resolución/expiración. La selección y configuración pertenecen al sitio que verifica la operación. No hay evidencia en esta revisión de que Boletera pueda cambiar la configuración de gub.uy o Sistarbanc. Tampoco se identificó una API pública de recarga STM con tokenización delegada que permita reemplazar el recorrido de Prex.

No se presenta un botón habilitado, `ng-valid` o el cierre de un desafío como prueba de resolución: el sitio puede habilitar Continuar antes de verificarlo. La quinta entrega incorpora la lectura de la aceptación del proveedor en la versión inspeccionada; falta comprobarla en una recarga real. No se han integrado proveedores externos de resolución ni transferido sesiones o datos de pago a terceros.

Fuentes consultadas el 26/9/2026:

- [Autocompletado en Compose](https://developer.android.com/develop/ui/compose/text/autofill).
- [AutofillManager y solicitudes a campos virtuales](https://developer.android.com/reference/android/view/autofill/AutofillManager).
- [reCAPTCHA v2: callbacks y configuración](https://developers.google.com/recaptcha/docs/display).
- [Tipos de reCAPTCHA](https://developers.google.com/recaptcha/docs/versions).
- [STM en línea](https://montevideo.gub.uy/stm-en-linea).

## Criterios y estado del objetivo

1. Medir tiempos y cantidad de intervenciones en cada etapa; distinguir demoras introducidas por Boletera de red/proveedor. Desde 0.2.36 hay medición local por etapas y recuento de avances manuales en la app. Falta recoger un recorrido real y separar su espera de red de la de la persona; el informe no pretende esa atribución. No prometer cinco segundos a partir de pruebas sintéticas.
2. Revisar el acceso y la recuperación de sesión para evitar autenticaciones repetidas cuando existe una sesión válida, conservando la separación de cuentas. Revisado con ocho pruebas Android de sesión; se reutiliza la sesión válida y la recuperación no repite una recarga.
3. Mejorar la presentación y recuperación del CAPTCHA y comprobar qué señales ofrece el flujo real después de resolverlo.
4. Revisar con capturas los estados de espera, errores, reanudación y el formulario de tarjeta en pantalla chica, teclado abierto y letra grande. La verificación posterior al autocompletado se comprobó a 360 × 640 dp y fuente 1,3 en 0.2.35. Falta contrastar el desafío real del proveedor con esa presentación.
5. Validar el autocompletado y el recorrido real en el teléfono del dueño. Las pruebas de laboratorio no sustituyen esa comprobación.
6. Publicar cada entrega Beta con rama, tag, APK y hash remoto verificados. Mantener explícito qué está probado y qué queda pendiente.

## Segunda entrega: 0.2.35 Beta 1

- Se reprodujo en WebView una preparación de recarga pendiente: tres lecturas de la pantalla anterior bastaban para perder `busy`. Se conserva ahora hasta la respuesta o el error. Se cubren Express y recarga común; un intento de preparar otro importe mientras espera tampoco borra la selección Express.
- Un CAPTCHA visible o un componente de verificación todavía cargando impide el envío automático de la tarjeta. El autocompletado completo oculta el teclado y desplaza la verificación a la vista. El botón Continuar queda disponible para después de la intervención humana.
- El error de tarjeta ofrece «Revisar respuesta», incluso cuando el proveedor no habilita Continuar. Antes podía mostrar un botón sin efecto.

### Evidencia nueva sobre CAPTCHA (26/9/2026)

Se leyó el [JavaScript público actual de Sistarbanc](https://pasarelaspe.sistarbanc.com.uy/v2/main-es2015.c4dd4374250f3678bcc8.js), sin abrir una solicitud de pago. SHA-256 de la copia local: `bbc0e8291f932f91f1f3dbbea3bdb7adae4332b6af990027823089b0ca3f497a`.

El componente `angular-recaptcha` recibe el resultado de Google y realiza después `autenticationService.login("recaptcha", respuesta)`. Solo tras esa respuesta establece la autenticación y emite éxito. En `alta-cliente` el método `resolvedCaptcha` está vacío; el botón usa `disableButton`, sin exponer allí un resultado verificable de CAPTCHA. Esto refuerza que leer una respuesta de Google o ver el botón habilitado no bastaría para continuar con confianza. No se incorporaron lecturas de tokens ni sustituciones de callbacks. La continuación automática después de resolverlo sigue pendiente de una señal fiable del proveedor.

## Tercera entrega: 0.2.36 Beta 1

El informe de Configuración usa el reloj monotónico del dispositivo. Agrupa preparación STM/Prex, tarjeta, verificación del titular, tarjeta con verificación, confirmación, revisión manual, recuperación y resultado/regreso. El segundo plano se contabiliza aparte y no se atribuye a una pantalla interactiva. Finaliza al consultar saldo, cancelar o detenerse por error; «Regreso al saldo» no afirma que se pagó.

El recuento suma el inicio de Express y los avances manuales aceptados desde los controles nativos. Excluye la elección de tarjeta o biometría de Android/Google, escritura, gestos del CAPTCHA y controles de la página original: no es un contador universal de toques. La suma de los tiempos conserva el total. Los datos viven en memoria y se copian únicamente si la persona toca Copiar información.

También se corrige la hora de consulta: una lectura repetida de la misma página con idéntico saldo/mínimo no mueve la hora de «Actualizado».

## Cuarta entrega: 0.2.37 Beta 1

Primer intento de la casilla de Google desde Android, una vez por etapa de Express. Las imágenes/audio y la continuación posterior siguen manuales. El prototipo activó la demo pública real y Google pidió imágenes; no se obtuvo aprobación sin intervención en ese caso. Se conservan configuración de cookies e identidad de navegador. Ver [alcance, evidencia y pendientes](CAPTCHA-ALTERNATIVAS.md).

## Quinta entrega: 0.2.38 Beta 1

El titular conocido y la tarjeta completamente autocompletada pueden continuar después de reconocer la aceptación del servidor en el componente visible del proveedor. La estructura se comprobó contra su runtime público en Android y la lógica de aceptación se ejecutó con servicios ficticios. Ante cambios del sitio o falta de evidencia, el avance sigue manual. No se equipara una respuesta de Google con una autorización.

Se comprobaron en Android espera, pausa, reanudación y envío único. No se resolvieron imágenes/audio ni se realizó una recarga real. El objetivo de experiencia final sigue abierto hasta comprobar el teléfono del dueño y medir el tiempo y las aprobaciones sin fotos durante su uso normal.

Esto avanza el punto 3, pero no lo cierra: la señal está implementada para la versión inspeccionada del proveedor; falta comprobar su comportamiento en el teléfono del dueño. No se reemplaza ese requisito por el éxito de un clic sintético.

## Sexta entrega: 0.2.39 Beta 1

Se encontró otra intervención redundante: un CVV escrito a mano seguido de Listo mostraba la verificación, pero olvidaba la intención de continuar. La prueba nueva reprodujo la falta de avance en 0.2.38 aun después de informar aceptación.

Express conserva ahora ese pedido explícito mientras espera la aceptación. El envío diferido usa nuevamente la comprobación del proveedor. Cambiar datos, solicitar otra tarjeta, un error o salir cancela el pedido. El texto de ayuda explica que después de verificar seguirá solo. La escritura sin Listo sigue sin enviar y la recarga común mantiene su comportamiento.

La prueba del teléfono y las mediciones de uso real siguen pendientes. Esta mejora elimina una interacción identificada; no demuestra un tiempo total ni una tasa de CAPTCHA sin fotos.
