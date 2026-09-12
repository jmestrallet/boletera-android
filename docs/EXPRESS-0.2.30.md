# Express y reutilización de sesión — 0.2.30

## Recorrido

La pulsación de **1,2 segundos** se conserva. Autoriza una sola recarga con la boletera y el importe mostrados. La ayuda ya no interrumpe una pulsación completa; un toque corto puede abrirla y **Entendido** solo la cierra.

Express conserva su identidad durante la operación: continuar después de una verificación no lo convierte en recarga común. Pasar al fondo suspende el avance; volver retoma la misma solicitud en memoria. Reiniciar el proceso no reconstruye ni reenvía una operación financiera.

El medio y el titular de la última recarga Prex confirmada tienen prioridad sobre selecciones abandonadas. Para instalaciones anteriores sin ese registro se conserva la selección existente como inicio compatible. El reconocimiento de la tarjeta de pago se aprende con una recarga confirmada en esta versión; puede necesitar una confirmación manual la primera vez.

Si Google entrega todos los datos válidos, el formulario continúa una sola vez. Si solo entrega número y vencimiento, Express enfoca CVV. **Listo** del teclado valida y continúa. Google conserva su propia selección y autenticación: no se elige una tarjeta del gestor sin intervención.

La confirmación financiera avanza automáticamente solo en Express cuando el identificador de la tarjeta ingresada coincide con el de la última recarga confirmada y los últimos cuatro dígitos coinciden con la tarjeta enmascarada mostrada por el proveedor. Se mantienen las comprobaciones originales de comercio, importe, estructura, controles adicionales y desafíos. Una tarjeta distinta, desconocida, una revisión manual o una pantalla no reconocida requieren intervención.

Con pago exitoso, Express acciona una sola vez el retorno original del proveedor. STM continúa su confirmación y la app consulta nuevamente la boletera. No requiere tocar **Volver a mi boletera**. Los resultados pendientes o rechazados siguen visibles y no se convierten en éxito.

## Sesión y datos guardados

Al iniciar, si existen acceso guardado, cuenta local identificada y cookies STM, primero se intenta recuperar esa sesión. Si sigue vigente, se vuelve al saldo sin descifrar la contraseña. Si requiere identificación, se solicita la huella con el mecanismo existente. No se eliminó ni debilitó la protección biométrica de `AccessVault`.

Se rechazó la alternativa de guardar número y vencimiento. Boletera no persiste PAN, vencimiento ni CVV. Para reconocer una tarjeta conserva los últimos cuatro dígitos y un identificador HMAC por cuenta, calculado con una clave de Android Keystore; ese identificador no permite autocompletar la tarjeta. Google y el proveedor siguen controlando sus autenticaciones.

Antes de enviar la confirmación, se guarda de forma cifrada y síncrona un seguimiento por cuenta: boletera, proveedor, importe, fecha, saldo anterior y referencia de transacción, cuando está disponible. La revisión de la página original y el traspaso a eBROU también conservan seguimiento. No se guarda una URL de pago reutilizable ni credenciales bancarias.

Una consulta de saldo no borra un pago incierto. Si la app se cierra o se vuelve a ingresar, el seguimiento se recupera y evita otra recarga inadvertida. **Ya revisé el pago** permite cerrar esa excepción de forma explícita después de comprobarla en el proveedor; no anula, devuelve ni reenvía el pago.

Cuando STM confirmó la recarga pero la consulta devuelve exactamente el saldo anterior, se programan hasta tres consultas adicionales. Si sigue igual se informa y se conserva la revisión. El saldo mostrado siempre proviene de STM; no se suma el importe localmente para simular acreditación.

## Velocidad y límites

Durante pasos en curso, las comprobaciones pasan a intervalos de 200 ms; las pantallas esperando intervención se revisan con menor frecuencia. La carga terminada de una página también dispara una inspección. No se afirma una reducción medida del tiempo total de un pago real: la latencia del proveedor y sus verificaciones siguen influyendo.

El saldo de la página de retorno no se reutiliza directamente: su adaptador actual no demuestra qué boletera está seleccionada. Se conserva la consulta que identifica la boletera correcta.

El CAPTCHA visible sigue requiriendo la continuación humana. El 12/9/2026 se verificó mediante GET público, sin operación ni sesión, que el botón del titular se controla por `disableButton` y que la autenticación se comprueba al pulsarlo. Por tanto, botón habilitado y formulario válido no demuestran CAPTCHA aprobado. No se leen respuestas, tokens ni contenido de sus iframes.

Fuente pública inspeccionada: [código de la pasarela](https://pasarelaspe.sistarbanc.com.uy/v2/main-es2015.ba3be1cce732d7895caf.js), SHA-256 `23f1080350a236fd6eddfca561a2a7cc672a7fef2b9b10f806e92cfaffbb0660`.
La selección/autenticación del gestor está descrita por [Android: Dataset](https://developer.android.com/reference/android/service/autofill/Dataset). La protección de la contraseña conserva las [claves con autenticación por uso](https://developer.android.com/identity/sign-in/biometric-auth).

## Validación

Los casos de laboratorio interceptan todas las páginas financieras y usan identidades, tarjetas y operaciones ficticias. Cubren tarjeta habitual con confirmación y retorno automático, tarjeta distinta con confirmación manual, resultado pendiente conservado, cifrado y aislamiento del seguimiento, suspensión, autocompletado y recuperación de sesión. El resultado final y las cantidades ejecutadas se registran en `VALIDACION.md`.

La percepción de velocidad, el autocompletado real de Google, los desafíos y una recarga completa en el teléfono del dueño siguen requiriendo comprobación física. Esta versión no garantiza una recarga de un solo gesto.
