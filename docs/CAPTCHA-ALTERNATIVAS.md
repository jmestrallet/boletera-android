# CAPTCHA: alternativas para Carga Express

Investigación del 26/9/2026, ampliada a pedido del dueño: un éxito parcial también sirve; no es necesario resolver automáticamente todos los desafíos.

## Conclusión

La automatización existe. Que el CAPTCHA siga manual en Boletera describe la implementación actual, no una imposibilidad técnica. Hay tres caminos concretos; ninguno tiene todavía una tasa medida en esta app y en el teléfono del dueño.

| Camino | Evidencia | Aplicación a Boletera |
| --- | --- | --- |
| Activar automáticamente la casilla y dejar las fotos a la persona | Google documenta que la casilla puede aprobar inmediatamente o presentar un desafío. Existen scripts que hacen ese clic. | Primer experimento recomendado: un intento por widget, sin bucles. No garantiza evitar las fotos. |
| Resolver el desafío de audio | Buster es una extensión abierta que usa reconocimiento de voz. Su autor documenta un cliente auxiliar para Windows, Linux y macOS. | Es una referencia real, pero no se instala directamente dentro de nuestro WebView Android. Requiere adaptación y revisar dónde se procesa el audio. |
| Resolver las imágenes con un modelo | El trabajo Breaking reCAPTCHAv2 publica código de clasificación/segmentación con YOLO y resultados experimentales. | Demuestra viabilidad, no una tasa actual para Boletera. El código publicado depende de Python/Firefox y requiere trabajo específico para Android. |

Se consultó también un hilo de r/firefox sobre dificultades de accesibilidad. Varios participantes mencionan Buster; el autor del hilo reporta problemas al probarlo. Son testimonios, no un ensayo controlado ni una garantía. Las conclusiones técnicas de arriba se apoyan en documentación, código de sus autores y el trabajo de investigación.

## Hallazgos en la app

- La verificación de Prex se muestra en el WebView interno. Una extensión de Chrome/Firefox no se incorpora al APK por instalarla en otro navegador.
- `EmbeddedPrexPayment` bloquea cookies de terceros y ajusta la identidad del navegador mediante `compatibilityIdentity`. STM también bloquea cookies de terceros. El posible efecto sobre los desafíos necesita medición; no se cambiaron estas opciones durante esta investigación.
- El CAPTCHA pertenece a un iframe de Google. Ejecutar `.click()` sobre el elemento iframe desde la página principal no equivale a tocar la casilla interna. Un intento desde Android necesita una coordenada validada en el widget visible, o una integración de ejecución dentro del frame; no alcanza con copiar el userscript.
- Sistarbanc valida con su propio servidor después de la respuesta de Google. Su componente emite éxito después de esa validación. Un botón habilitado, una pausa fija o el cierre de las fotos no demuestran ese éxito. Ver evidencia y hash del JavaScript público en [mejora integral](MEJORAS-EXPRESS-2026-09.md).

## Experimento propuesto

1. Probar en laboratorio un solo toque sobre un checkbox visible, de tamaño y origen reconocidos, con la app en primer plano. Si hay un desafío abierto, geometría desconocida o navegación, no tocar.
2. Comprobar que no se repite ante lecturas periódicas, cambios de tamaño, pausa/reanudación o un widget ya resuelto. No usar reintentos para forzar una aprobación.
3. Encontrar una señal verificable de aceptación de Sistarbanc antes de automatizar el avance posterior. No sustituir la validación por una estimación de tiempo.
4. Medir con uso normal cuántos intentos pasan sin fotos, cuántos necesitan intervención y cuánto tiempo se ahorra. El objetivo de 3 de 5 es un criterio deseado del dueño, no un resultado demostrado. Un lote de cinco tampoco establece por sí solo una tasa estable.
5. Evaluar audio o modelo local si la casilla automática ahorra poco. No se contrató un servicio de resolución ni se transfirieron sesiones, capturas de pago o credenciales a un tercero.

## Prueba e implementación de 0.2.37

Se probó un toque nativo dentro del WebView Android configurado con la identidad de Boletera y cookies de terceros bloqueadas. En la demo pública de Google el toque activó la casilla y abrió un desafío de imágenes. El intento no lo aprobó automáticamente: resultado observado `challenge`. Captura e informe locales: `outputs/captcha-checkbox-live.png` y `outputs/captcha-checkbox-live.txt`. No se tocó ninguna imagen ni se envió el formulario de la demo. Este único caso no estima una tasa de éxito para STM/Prex.

La prueba local de un iframe de otro dominio diferencia `.click()` en su contenedor (sin efecto dentro) del evento táctil Android (llega al control interior). La implementación Beta usa ese mecanismo, limitado a Carga Express y a las etapas titular/tarjeta. En tarjeta espera hasta que el formulario muestre la verificación. No intenta tocar frames de geometría desconocida, widgets compactos, desafíos abiertos, elementos tapados, respuestas ya presentes ni páginas fuera del origen esperado. La existencia de respuesta se reduce localmente a un booleano; no sale del script, no se conserva y nunca se usa como autorización de Sistarbanc.

Un toque manual en el WebView cancela la asistencia durante ese recorrido. Pausa, navegación y salida invalidan los callbacks pendientes. No se repite un intento por etapa al reanudar. El control humano de Continuar sigue disponible: un intento de casilla y una autorización del proveedor son hechos distintos.

Pendientes: comprobar el intento en el teléfono del dueño, medir aprobaciones sin imágenes y obtener una señal fiable de aceptación de Sistarbanc para avanzar automáticamente. Audio/modelos siguen como alternativas investigadas, sin integración en el APK.

## Fuentes consultadas

- [Google: variantes y comportamiento de reCAPTCHA](https://developers.google.com/recaptcha/docs/versions).
- [Código del autor de Auto Click Captcha](https://greasyfork.org/en/scripts/439388-auto-click-captcha/code).
- [Buster: documentación del autor](https://github.com/dessant/buster/blob/main/README.md).
- [Breaking reCAPTCHAv2: artículo](https://arxiv.org/abs/2409.08831) y [código de los autores](https://github.com/aplesner/Breaking-reCAPTCHAv2).
- [Foro r/firefox: experiencias de accesibilidad y Buster](https://www.reddit.com/r/firefox/comments/1jrcag0/my_grandpa_cant_solve_captcha_challenges_how_do_i/).
