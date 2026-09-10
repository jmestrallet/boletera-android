# Panel de verificación — candidato 0.1.12

El componente `angular-recaptcha` existente recibe una posición estable mediante una regla de presentación reversible. No se copia su HTML interno, no se mueve ni recrea el iframe y no se cambia la conexión con el formulario de Sistarbanc. Al mostrar la pantalla original, se retira esa regla.

El navegador se mide con el espacio disponible en la ventana de la app. El panel convierte las medidas CSS usando el ancho real del navegador y el ancho de su viewport; ya no supone que la pantalla física completa coincide con la ventana. Sigue mostrando el componente web original mediante un panel nativo acotado: no es un CAPTCHA nativo ni una integración independiente de Google.

## Pruebas Android con contenido ficticio

Todas las direcciones se interceptaron y se respondieron localmente. El supuesto iframe de Google contenía un botón de color que notificaba los toques; no se contactó con Google, STM o Sistarbanc durante estas pruebas.

| Resolución del emulador | Densidad configurada | Resultado |
| --- | --- | --- |
| 1080 × 1920 | 240 dpi | Aprobado |
| 1080 × 1920 | 420 dpi | Aprobado |
| 1920 × 1080 | 320 dpi | Aprobado |

En cada configuración se probaron ventanas de 340, 280 y 380 dp y un desafío expandido de 304 × 320 píxeles CSS. Se comprobó que los toques llegaran al botón original, que el iframe mantuviera su identidad y una sola carga, y que la presentación se pudiera restaurar. La prueba de retención del pago también pasó: reabrir conserva la página sin repetir su envío ficticio.

## Teléfono real

Se instaló la APK `61D96D03D8B93CC53F1AFAA1E2D95148819310D05FB13CBB9A5933C7A3155237` en el OnePlus 6T con Android 11. Se abrió la misma solicitud pendiente y se comprobó visualmente el checkbox original completo dentro de la pantalla nativa. No se inició otra solicitud.

El CAPTCHA real no se resolvió. El desafío expandido real, los demás modelos físicos, el envío del formulario, Google/tarjeta, la autorización y la acreditación siguen pendientes. La matriz demuestra el comportamiento del panel con contenido controlado; no certifica una operación bancaria ni compatibilidad universal.
