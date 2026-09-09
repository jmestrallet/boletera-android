# Boletera · prueba Android

App independiente y no oficial, de código abierto bajo licencia MIT, con interfaz nativa para el acceso, la boletera y el importe. Maneja el sitio de STM por detrás. **Versión de prueba: puede iniciar una recarga real con Prex o eBROU. La autorización y el resultado se completan en el proveedor, mediante Chrome. No está lista para uso general.**

El código y la documentación se publican para permitir revisión y colaboración. En un emulador Android 16 se comprobaron el ingreso real, saldo/mínimo, preferencias, el inicio real de Prex y eBROU y el regreso a la consulta de saldo. La autorización bancaria, la acreditación, el CAPTCHA real y la huella física siguen pendientes. Consultá [el estado de validación](docs/VALIDACION.md) y [las comprobaciones pendientes de pago](docs/COMPATIBILIDAD-PAGO.md).

## Probar en el celular

**[Descargar la versión 0.1.6](https://github.com/jmestrallet/boletera-android/releases/tag/v0.1.6)** · [Todas las versiones y APK históricos](https://github.com/jmestrallet/boletera-android/releases)

En **Assets**, elegí el archivo `.apk`. Las descargas de GitHub son públicas y se pueden compartir por enlace. Están conservados los siete APK, de 0.1.0 a 0.1.6, con su SHA-256. La 0.1.0 se publica como archivo histórico: es anterior al código subido al repo y su descarga automática de «Source code» no contiene el código de esa versión.

1. Pasá `outputs/boletera-prueba-0.1.6.apk` a tu Android e instalala. Requiere Android 8 o posterior y Android System WebView actualizado. La actualización usa la misma firma que las versiones anteriores; instalala encima para conservar el acceso guardado.
2. Abrí **Boletera · Prueba** e ingresá tu documento y contraseña de Usuario gub.uy.
3. Si querés, activá **Guardar acceso con huella**. El celular debe tener biometría fuerte configurada. La app pedirá autorización para cifrar el acceso; en el siguiente ingreso, pedirá biometría para descifrarlo.
4. Si aparece un CAPTCHA reconocido, se mostrará únicamente su recorte interactivo. Completalo y tocá **Ya completé la verificación**.
5. Elegí la boletera operativa una vez: la app la recuerda por cuenta y la selecciona en los próximos ingresos si sigue habilitada. Podés usar **Cambiar boletera**. Compará saldo y mínimo con STM y elegí el monto.
6. Elegí tu medio habitual. Prex y BROU aparecen primero; la app recuerda tu elección y permite cambiarla. Tocá **Pagar** para abrir el pago oficial en Chrome. La app prepara una solicitud real; autorizala solo si querés realizar esa recarga. Al volver, consultá el saldo y revisá la confirmación del proveedor.

Si el acceso, CAPTCHA o certificado falla, la app se detiene. El acceso a STM mantiene la interfaz propia. El pago con Prex o eBROU usa expresamente Chrome y conserva el autocompletado de Google. Para reportar el problema alcanza con el texto del mensaje y el modelo/versión de Android; no compartas contraseñas ni números bancarios.

Desde 0.1.6, **Volver al pago de Prex** abre el enlace de la misma solicitud, incluso después de un nuevo ingreso a la app. No crea otra solicitud en STM. Si ya autorizaste, revisá el resultado sin volver a autorizar; si el enlace venció, no se reemplaza automáticamente. El enlace queda cifrado localmente y se elimina al cerrar la revisión o al olvidar el acceso. Esta recuperación solo se ofrece para Prex.

## Qué incluye

- Kotlin + Compose, Android WebView local, ingreso por Usuario gub.uy, selección de boletera, saldo, mínimo del sitio, selección de importe y traspaso del pago a Prex o eBROU mediante Chrome.
- Adaptador de lectura/navegación independiente de las pantallas, basado en controles y texto observados en STM. Los cambios del sitio pueden requerir una actualización de la APK.
- CAPTCHA original: se conserva el documento y el iframe. El contenedor nativo recorta y escala su rectángulo; no se copia HTML a otro origen ni se resuelve el desafío por código. **Su funcionamiento con desafíos reales aún necesita una prueba en el teléfono.**
- WebView limitado a los orígenes de STM e ID Uruguay observados y, solo al iniciar un pago, a sus pasarelas verificadas. SSL inválido, redirecciones desconocidas, montos ilegibles y pantallas nuevas detienen el recorrido.
- Sin backend, SDK de anuncios, analítica, gestor de contraseñas remoto ni API de pago.

## Credenciales y sesión

`AccessVault` usa AES-256-GCM y una clave no exportable de Android Keystore. Cada operación de cifrado/descifrado está vinculada a un `BiometricPrompt.CryptoObject` con autenticación biométrica fuerte por uso. En disco solo quedan el texto cifrado y el IV aleatorio; las contraseñas de esta conversación no están incluidas.

La protección concreta de la clave depende del hardware del teléfono; no se afirma que todos los dispositivos tengan StrongBox. No hay alternativa que guarde texto plano. Si falta biometría compatible, queda el ingreso manual. Agregar/quitar biometría o cambiar la seguridad del dispositivo puede invalidar la clave; en ese caso hay que **Olvidar acceso guardado** y configurarlo de nuevo.

Para enviar las credenciales al sitio, necesariamente existen brevemente descifradas en memoria. El motor descarta sus referencias al completar el ingreso, cancelar, pasar a segundo plano o superar tres minutos. No se registran en consola ni se guardan en estado restaurable. Desde 0.1.2 las capturas están habilitadas a pedido del usuario para reportar problemas. Antes de compartir una captura, revisá que no incluya datos personales o de tarjeta.

Un nuevo ingreso explícito limpia primero la sesión web local para no mostrar accidentalmente otra cuenta. **Cerrar sesión local** borra cookies/almacenamiento WebView pero conserva el acceso cifrado. **Olvidar acceso guardado** elimina ambos y las preferencias locales. La boletera y el medio elegidos se guardan separados por cuenta; el identificador de cuenta se deriva con una clave HMAC local no exportable de Android Keystore, sin guardar el documento en las preferencias. Si no se puede acceder a esa clave, la app mantiene la elección manual. El respaldo en la nube y la transferencia de datos de la aplicación están excluidos.

## Compilar

Para compilar el código público sin claves privadas, instalá JDK 17/21, Node.js y Android SDK con plataforma 35 y build-tools 35.0.0. Indicá el SDK con `ANDROID_HOME` o con un `local.properties` propio. Desde PowerShell:

```powershell
git clone https://github.com/jmestrallet/boletera-android.git
cd boletera-android
npm ci --ignore-scripts
npm test
./gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

En Linux/macOS usá `./gradlew` en lugar de `./gradlew.bat`. El APK de desarrollo queda en `app/build/outputs/apk/debug/app-debug.apk`. Tiene firma de desarrollo y no reemplaza la APK release instalada con otra firma. Esta compilación no necesita cuenta STM ni credenciales de GitHub.

Entorno usado: JDK 21 (compila bytecode Java 17), Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, compile/target SDK 35. Versiones fijadas. `gradlew` contiene verificación SHA-256 de la distribución.

En esta PC las herramientas, SDK, emuladores y firma de prueba están en `.tools/` (excluido del código). La clave de firma local **debe conservarse** para instalar actualizaciones sin desinstalar la app. No es la clave que protege las credenciales: esa se genera dentro de cada teléfono.

```powershell
npm ci --ignore-scripts
./build.ps1
```

El script ejecuta pruebas JS, pruebas JVM y lint, compila la APK release **sin depurador**, firma con la clave local de prueba y la copia a `outputs/`. No publica ni instala en un teléfono.

En otra máquina: instalá JDK 17/21 y SDK Android (plataforma 35, build-tools 35.0.0), configurá `local.properties` con `sdk.dir=...` y usá `./gradlew.bat :app:assembleDebug`. La variante debug es para desarrollo. Para la variante release, el archivo local `.tools/signing.properties` debe contener `password=...`, correspondiente al almacén `.tools/boletera-test.jks`, alias `boletera-test`. Ninguno se distribuye con el código fuente.

## Comprobaciones

```powershell
npm test
./gradlew.bat :app:testDebugUnitTest :app:lintDebug
# Pruebas locales en emulador/dispositivo dedicado sin datos personales:
./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=uy.boletera.prueba.DeviceSmokeTest
# Sondeo público en vivo, SIN ingresar documento ni contraseña:
./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=uy.boletera.prueba.PublicSiteProbe
```

Los tests JS usan fixtures sintéticos y WebView se prueba también dentro de Android. El sondeo público en vivo es una comprobación distinta: una falla de red/certificado no se oculta como éxito. Ver `docs/VALIDACION.md` para resultados y limitaciones.

## Pendiente para una app pública

- Validar el ingreso completo y el CAPTCHA real en un Android físico.
- Validar guardado/desbloqueo biométrico, cancelación, contraseña incorrecta y cambio de huellas en hardware real.
- Aceptar y validar el pago oficial en Chrome con Prex/eBROU, incluyendo su autorización y acreditación reales. No se simula una recarga exitosa.
- Otros métodos de ID Uruguay, diferentes tipos de boletera y revisión de las condiciones aplicables antes de publicar.

No se infiere el mínimo a partir de una tarifa fija. Pago confirmado y saldo acreditado deberán ser estados diferentes cuando se implemente el cobro real.
