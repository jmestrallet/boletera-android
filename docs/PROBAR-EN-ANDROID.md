# Probar Boletera en Android

## 1. Elegí una versión

- **Estable:** `boletera-0.2.32.apk`. Es la recomendada y no incluye Carga Express.
- **Beta:** `boletera-beta-0.2.32-beta.1.apk`. Incluye Carga Express como función experimental.

Ambas requieren Android 8 o posterior. Instalá el APK encima de una versión anterior para conservar los datos guardados.

## 2. Ingresá a STM

Usá tu documento y contraseña de Usuario gub.uy. Si activás **Guardar acceso con huella**, Boletera cifra esas credenciales con Android Keystore. Al vencer la sesión, puede volver a ingresar sin pedir otra huella mientras el proceso siga abierto.

La primera vez, STM puede pedir que autorices el vínculo con Usuario gub.uy. Completá ese paso en el sitio oficial y volvé a Boletera.

## 3. Revisá saldo y boletera

Elegí tu boletera operativa. Boletera recuerda esa elección para los próximos ingresos. Compará el saldo y el mínimo con la información de STM antes de continuar.

## 4. Prepará la recarga

Elegí el importe y el medio de pago:

- **Prex** continúa dentro de Boletera con la página original de Sistarbanc detrás de la interfaz nativa.
- **eBROU** se abre en Chrome.

La solicitud puede ser real. Revisá el importe, la tarjeta y cualquier verificación antes de confirmar. Boletera no resuelve CAPTCHA ni aprueba pagos por vos.

En la Beta, **Carga Express** empieza con un toque, usa el mínimo vigente, la boletera habitual, Prex y el titular conocido. Al llegar a la tarjeta solicita el autocompletado de Android. Elegí la tarjeta guardada y autorizá con la huella si tu proveedor la pide; si Android entrega número, vencimiento y CVV, Boletera continúa sin otro toque. El tiempo depende de STM, Sistarbanc y la conexión.

## 5. Comprobá el resultado

Después del pago, volvé a tu boletera y consultá el saldo. Si el resultado queda pendiente o incierto, revisalo en Prex o en tu banco antes de iniciar otra recarga. Boletera conserva ese aviso y no repite automáticamente la operación.

## Reportar un problema

Las capturas están habilitadas. Compartí la versión, el paso, el mensaje visible y el modelo de teléfono. Ocultá contraseñas, números de tarjeta, CVV y enlaces de pago.

Las pruebas automatizadas usan datos ficticios y no acreditan una recarga real. Ver [validación](VALIDACION.md) y [compatibilidad de pagos](COMPATIBILIDAD-PAGO.md).
