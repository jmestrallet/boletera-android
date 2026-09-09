# Compatibilidad de pago — 9 de septiembre de 2026

## Alcance implementado en 0.1.5

Se implementó el inicio de pagos con **Prex y eBROU**, conservando la boletera y el medio elegidos por cuenta. La app prepara la recarga con STM y abre la autorización del proveedor en una pestaña de Chrome. No captura ni guarda credenciales bancarias, números de tarjeta o CVV.

La excepción de mostrar el pago oficial en Chrome fue presentada al usuario porque la dirección anterior pedía una interfaz completamente propia. La aceptación de esa excepción sigue pendiente; desarrollar y probar el recorrido no equivale a aceptación del usuario ni a un cobro confirmado.

## Evidencia real

- **Prex:** el WebView recibió un bloqueo explícito que exige navegador oficial. El enlace de la misma operación abrió correctamente en Chrome, con el monto preparado en STM. Se observó el resumen y, en la investigación, el formulario de datos del titular y CAPTCHA. No se ingresaron datos financieros ni se completó un débito.
- **eBROU:** STM entrega un formulario de transacción a `ebanking.brou.com.uy/multipagos/billetera`. Se comprobó un traspaso del formulario original a Chrome que llegó al acceso oficial del banco. La continuidad después de ingresar al banco y la autorización necesitan una prueba del usuario; no se dispone de sus credenciales bancarias.
- **Visa:** durante la investigación inicial también se observó el bloqueo de WebView. No está incluida entre las opciones de pago habilitadas de esta versión.

Elegir un proveedor y abrir su pago puede generar una referencia o intento pendiente. Eso no equivale a un débito ni permite afirmar que el servidor haya eliminado el intento al cerrar Chrome.

## Cómo se conserva el recorrido

Prex recibe el enlace original de su operación, limitado al dominio y las rutas observadas de Sistarbanc. No se copian cookies de la app a Chrome.

eBROU requiere POST. La app conserva únicamente los campos originales de ese traspaso y comprueba el destino exacto, el monto en centésimos, la moneda y las direcciones de retorno. Un servidor temporal escucha solo en `127.0.0.1`, usa una dirección aleatoria de un solo uso y entrega a Chrome un formulario que se envía directamente a eBROU. No hay un servidor externo intermediario. El contenido no se escribe en disco, se entrega con `no-store` y una política que permite enviar el formulario únicamente al banco; el servidor se cierra al usarlo o a los 60 segundos.

La app no modifica el agente de usuario ni intenta eludir el bloqueo del proveedor. Chrome conserva su pantalla, identidad de origen y controles de seguridad.

## Resultado, interrupciones y reintentos

Antes de iniciar la solicitud se guarda un aviso local por cuenta. Ese aviso impide iniciar otro pago automáticamente, incluso después de reiniciar la app. Olvidar las credenciales y preferencias no elimina el aviso de una operación pendiente.

Al volver, se puede consultar el saldo sin reenviar la recarga. **Un cambio de saldo no se trata como confirmación de ese pago.** La confirmación se revisa en el proveedor. Para otra carga, el usuario indica que verificó que el pago terminó o que salió antes de autorizarlo; esta declaración solo libera la próxima recarga, no modifica el banco.

No hay conciliación automática de operaciones implementada. La consulta de movimientos de la cuenta de prueba exigió un nivel de identidad superior; no se intentó sortear ese requisito.

## Tarjeta guardada y huella

Recordar Prex o eBROU no guarda una tarjeta bancaria ni su contraseña. La huella de acceso a STM es independiente de la autenticación del pago. Chrome y el proveedor determinan si se ofrece autocompletado, huella, contraseña, llave digital o CAPTCHA; no se promete que todos los pagos se resuelvan con una huella.

Se investigó un servicio opcional de Android para ofrecer perfiles de titular dentro de Chrome. Es técnicamente distinto de controlar una Custom Tab y requiere cambiar el proveedor de autocompletado; no está implementado en la APK ni validado contra Prex. Ver [la propuesta y sus consecuencias](PERFILES-PREX.md).

Referencias oficiales:

- [BROU: recarga STM mediante Multipagos eBROU](https://www.brou.com.uy/personas/servicios/multipagos/stm-en-linea).
- [Prex: recarga de STM](https://www.prexcard.com/beneficiosprex?beneficio=6).
- [Chrome: Custom Tabs y API de lanzamiento](https://developer.chrome.com/docs/android/custom-tabs/howto-custom-tab-low-level-api).
- [Google: información de pago automáticamente en apps](https://support.google.com/googlepay/answer/9215533?hl=en).

## Pendiente

Aceptar la excepción de Chrome; comprobar los dos recorridos en el teléfono con autenticación del usuario; verificar una recarga efectivamente autorizada y acreditada; probar biometría física y CAPTCHA real. No se describe esta app de prueba como lista para publicación general.
