# Recuperación de sesión · 0.2.21

La sesión de STM puede vencer mientras Boletera permanece abierta. La app no conserva una contraseña descifrada para renovar el acceso: el almacén de credenciales exige una autorización biométrica por uso.

## Detección y recuperación

Se reconocen el regreso al login oficial después de haber verificado la cuenta y los mensajes explícitos de sesión expirada, vencida o caducada, incluido ViewExpiredException. La comprobación ocurre al observar la página y ante un paso rechazado; no se considera vencida una sesión solo porque transcurrió un plazo arbitrario. Un error HTTP se inspecciona al completar la página para distinguir su cuerpo de expiración de una falla real del servicio. Un aviso oculto, contraseña incorrecta o caída del servicio no se convierte en expiración.

Primero se navega nuevamente a las boleteras. Si queda una sesión web utilizable, las acciones de entrada de STM/Usuario gub.uy pueden recuperar el acceso sin pedir datos. Este intento de navegación está acotado: no se repiten sus pasos en bucle. Si aparece el formulario de identificación, se presenta Tu sesión venció y se solicita automáticamente la huella cuando existe un acceso guardado. Cancelar deja disponibles reintento e ingreso manual, sin abrir el diálogo repetidamente al volver del fondo. Sin acceso guardado se muestra el formulario.

La vuelta al login durante el retorno de Prex también sale del panel original e inicia la consulta y recuperación de STM. Las verificaciones del proveedor siguen perteneciendo a su recorrido.

## Límites que se conservan

La recuperación descarta el importe preparado y el avance pendiente de Express. Nunca reejecuta la selección/envío de proveedor ni una confirmación financiera. Si hubo una solicitud activa, al entrar de nuevo se consulta el saldo y se recuerda revisar el estado del pago antes de otra recarga. No se interpreta el cierre del panel como cancelación o éxito del proveedor.

Se conservan las elecciones por cuenta y el acceso cifrado. La cuenta y el saldo se vuelven a leer; no se muestra un monto calculado localmente como acreditación. Los errores no reconocidos como expiración siguen siendo errores reales, con su explicación habitual.

## Comprobación

Los casos Android usan sitios interceptados y datos ficticios: sesión web reutilizable, identificación manual tras vencimiento, vencimiento después de enviar al proveedor sin reenvío, expiración HTTP frente a falla HTTP real. Las pruebas del adaptador cubren avisos ocultos, orígenes ajenos y no lectura de credenciales. Se reutiliza AccessVault sin cambiar su cifrado ni su exigencia biométrica. La nueva solicitud automática de huella necesita comprobación en un teléfono con acceso guardado; no se afirma validación física por las pruebas ficticias. Ver [VALIDACION.md](VALIDACION.md).
