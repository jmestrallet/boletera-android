# Cierre de Prex · 0.2.19

## Problema observado

El dueño completó por primera vez una recarga real el 10/9/2026. Sus cuatro capturas muestran confirmación final de Sistarbanc, comprobante exitoso, confirmación de recarga en STM y saldo posterior aumentado. El pago funcionó, pero esas pantallas aparecieron como web dentro del panel y el recorrido no devolvió la home nativa. Las capturas privadas y sus referencias no se incorporan al repositorio.

## Recorrido corregido

1. **Confirmá tu pago** muestra importe, comercio y tarjeta ya enmascarada; el detalle conserva la referencia de la solicitud. **Confirmar pago** ejecuta el botón original una vez, únicamente tras el toque del usuario.

2. **Pago confirmado** muestra el importe y permite desplegar el comprobante visible del proveedor. Rechazo y pendiente tienen estados separados; un texto desconocido conserva la página original.

3. **Volver a mi boletera** acciona el enlace original de Sistarbanc, incluido su retorno al comercio. No se construye una URL ni se salta el intercambio del proveedor.

4. **Recarga exitosa** requiere que STM muestre tanto esa declaración como «Recarga Confirmada.». **Ver mi saldo** acciona su Continuar original.

5. Al aparecer la página principal de STM con saldo y control Recargar, se cierra el panel de pago y se consultan nuevamente las boleteras, saldo y mínimo. No se suma el importe de manera local ni se vuelve a enviar la recarga.

## Límites del adaptador

Se utiliza la estructura de los componentes públicos `finalizar-pago` y `resultado-pago`, contrastada con las capturas del dueño. Solo se leen filas visibles de resumen/comprobante, nunca valores de inputs financieros, tokens ni estado interno de Angular. El medio de pago solo se copia si ya está enmascarado y termina en cuatro cifras.

El comercio debe ser STM Recargas y el total visible debe coincidir exactamente con el importe de la solicitud. Formularios adicionales, CAPTCHA interactivo, diálogos, datos inesperados y acciones ambiguas conservan la página original. La insignia invisible de reCAPTCHA no se confunde con un desafío interactivo. Carga Express conserva su límite anterior y no confirma el pago.

La confirmación enviada desde la pantalla propia queda bloqueada durante toda esa sesión de pago, incluso al navegar o reabrir el panel. Si la respuesta se interrumpe, no hay reintento automático. No hay archivo permanente del comprobante; puede consultarse en el paso correspondiente y abrirse su página original.

## Evidencia y alcance

El pago real anterior fue realizado por el dueño; no valida automáticamente el adaptador nuevo. Las pruebas nuevas usan datos ficticios, interceptan todas las solicitudes y verifican envío único, resultado, retorno del proveedor, continuación STM y saldo consultado de nuevo. Se conservan aparte las comprobaciones históricas de eBROU, que no se modificó en esta entrega. Detalle de ejecuciones en [VALIDACION.md](VALIDACION.md).
