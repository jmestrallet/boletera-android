# Carga Express 0.2.32

## Resultado

En Boletera Beta, un toque en **Carga Express** inicia el recorrido con el mínimo que STM acaba de informar. Reutiliza la boletera operativa habitual, Prex y el titular válido conocido. No muestra una explicación ni exige mantener presionado.

Cuando Sistarbanc llega al formulario de tarjeta, Boletera enfoca el número y solicita inmediatamente el proveedor de autocompletado configurado en Android. El usuario elige su tarjeta y autoriza con huella si ese proveedor lo requiere. Si Android entrega número, vencimiento y CVV válidos, el formulario continúa una sola vez sin tocar **Continuar**.

La confirmación final automática sigue limitada a una tarjeta que ya haya completado una recarga exitosa y cuya identidad local coincida. Una tarjeta nueva o distinta pide confirmación. El CAPTCHA y cualquier desafío adicional permanecen bajo control de la persona.

## Por qué queda limitado a Prex

Prex mantiene el recorrido dentro de Boletera y permite presentar campos Android nativos. eBROU abre una autorización bancaria externa y no ofrece el mismo camino de autocompletado; por eso continúa disponible mediante la recarga común y no aparece como Carga Express.

## Seguridad

Boletera no guarda el número de tarjeta, el vencimiento ni el CVV. Los valores existen sólo mientras el formulario está visible, el CVV se borra al avanzar y todo se limpia al salir o enviar la app al fondo. El identificador local de una tarjeta exitosa no permite reconstruir sus datos.

Android prohíbe que Boletera elija por su cuenta un conjunto privado de otro proveedor o reaproveche la huella usada para gub.uy. La selección, la interfaz y la autenticación pertenecen al servicio de autocompletado elegido por el usuario. Tampoco se incorpora un depósito propio de tarjetas: almacenar CVV después de una autorización no es una alternativa válida.

## Qué puede impedir el recorrido mínimo

- La respuesta de STM, Sistarbanc y la conexión determina cuánto demora llegar a la tarjeta; cinco segundos es un objetivo de experiencia, no un plazo garantizable.
- Google u otro proveedor puede pedir primero elegir una tarjeta, puede no tener CVV guardado o puede exigir su propia autenticación.
- Un CAPTCHA, una pantalla desconocida o una validación del proveedor detiene el avance automático.
- La primera operación con una tarjeta y cualquier tarjeta distinta conservan una confirmación final.

Referencias oficiales: [AutofillManager de Android](https://developer.android.com/reference/android/view/autofill/AutofillManager), [datos de pago y autenticación en Android](https://support.google.com/pixelphone/answer/9215533), [tarjetas virtuales y huella](https://support.google.com/googlepay/answer/11234179), [prohibición de guardar CVV](https://www.pcisecuritystandards.org/faqs/1319/).
