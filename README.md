# Boletera · prueba Android

App independiente y no oficial, de código abierto bajo licencia MIT, con interfaz nativa para el acceso, la boletera y el importe. Maneja el sitio de STM por detrás. **Versión de prueba: puede iniciar una recarga real. Prex se abre dentro de Boletera con perfiles de titular; eBROU conserva Chrome. La autorización y acreditación reales siguen pendientes de validación. No está lista para uso general.**

El código y la documentación se publican para permitir revisión y colaboración. Con 0.1.5 se comprobaron en un emulador Android 16 el ingreso real, saldo/mínimo, preferencias, el inicio real de Prex/eBROU y el regreso al saldo. La integración nueva de 0.1.8 se probó con formularios ficticios; por separado se midió el detector público sin operación. La autorización bancaria, la acreditación, el CAPTCHA real y la huella física siguen pendientes. Consultá [el estado de validación](docs/VALIDACION.md) y [las comprobaciones pendientes de pago](docs/COMPATIBILIDAD-PAGO.md).

La 0.1.9 corrige la conexión segura de Sistarbanc en Android 11; se reprodujo el fallo y se verificó la corrección en un OnePlus 6T. Ver [evidencia TLS](docs/TLS-ANDROID11.md).

## Probar en el celular

**Objetivo pendiente:** completar y verificar el recorrido real de pago desde Boletera. La versión 0.1.8 incorpora Prex dentro de la app, apariencia propia y datos del titular guardados por perfil. No es una API de pago nativa: conserva la página y sus verificaciones. El CAPTCHA, Google en el teléfono, la autorización y la acreditación requieren comprobación real. Ver [investigación y evidencia](docs/INTEGRACION-NATIVA-PAGO.md).

**[Descargar la versión publicada 0.1.10](https://github.com/jmestrallet/boletera-android/releases/tag/v0.1.10)** · [Todas las versiones y APK históricos](https://github.com/jmestrallet/boletera-android/releases)

El candidato local 0.1.11 reemplaza el resumen y los datos del titular por pantallas nativas. Mantiene la página del proveedor detrás y muestra el CAPTCHA original en un panel. Su validación del pago completo sigue pendiente; no se publicó una nueva versión remota. El archivo local es `outputs/boletera-prueba-0.1.11.apk`.

El candidato vigente es **0.1.14**, en `outputs/boletera-prueba-0.1.14.apk`: permite actualizar el saldo arrastrando hacia abajo desde el comienzo de la pantalla, en lugar del botón inferior. Conserva la corrección del destello de Sistarbanc de 0.1.13 y la adaptación del CAPTCHA incorporada en 0.1.12; ver [pruebas de tamaños y densidades](docs/CAPTCHA-RESPONSIVO.md). Todavía no se publicó una versión remota nueva.

En **Assets**, elegí el archivo `.apk`. Las descargas de GitHub son públicas y se pueden compartir por enlace. Las versiones anteriores se conservan con su SHA-256. La 0.1.0 se publica como archivo histórico: es anterior al código subido al repo y su descarga automática de «Source code» no contiene el código de esa versión.

1. Pasá `outputs/boletera-prueba-0.1.10.apk` a tu Android e instalala. Requiere Android 8 o posterior y Android System WebView actualizado. La actualización usa la misma firma que las versiones anteriores; instalala encima para conservar el acceso guardado.
2. Abrí **Boletera · Prueba** e ingresá tu documento y contraseña de Usuario gub.uy.
3. Si querés, activá **Guardar acceso con huella**. El celular debe tener biometría fuerte configurada. La app pedirá autorización para cifrar el acceso; en el siguiente ingreso, pedirá biometría para descifrarlo.
4. Si aparece un CAPTCHA reconocido, se mostrará únicamente su recorte interactivo. Completalo y tocá **Ya completé la verificación**.
5. Elegí la boletera operativa una vez: la app la recuerda por cuenta y la selecciona en los próximos ingresos si sigue habilitada. Podés usar **Cambiar boletera**. Compará saldo y mínimo con STM y elegí el monto.
6. Elegí tu medio habitual. Para Prex, elegí o agregá un perfil con los datos del titular. **Agregar otra Prex** comienza con campos vacíos. No se guarda número de tarjeta, vencimiento ni CVV en esos perfiles.
7. Tocá **Pagar**: Prex continúa dentro de Boletera y eBROU en Chrome. La app prepara una solicitud real; autorizala solo si querés realizar esa recarga. Al volver, consultá el saldo y revisá la confirmación del proveedor.

Si el acceso, CAPTCHA o certificado falla, la app se detiene. No cambia el servicio de autocompletado de Android; los perfiles propios son solo datos ordinarios del titular. La compatibilidad de Google con el formulario dentro del WebView todavía requiere prueba en el teléfono. Para reportar el problema alcanza con el texto del mensaje y el modelo/versión de Android; no compartas contraseñas ni números bancarios.

Desde 0.1.6, **Volver al pago de Prex** abre el enlace de la misma solicitud, incluso después de un nuevo ingreso a la app. No crea otra solicitud en STM. Si ya autorizaste, revisá el resultado sin volver a autorizar; si el enlace venció, no se reemplaza automáticamente. El enlace queda cifrado localmente y se elimina al cerrar la revisión o al olvidar el acceso. Esta recuperación solo se ofrece para Prex.

Desde 0.1.8, mientras la app conserva la pantalla, volver muestra esa misma página sin cargar el enlace de nuevo. La solicitud también conserva el perfil con el que se inició; cambiar el favorito no altera un pago pendiente. Si el formulario trae datos distintos, la app los señala antes de reemplazarlos por el perfil elegido.

## Qué incluye

- Kotlin + Compose, Android WebView local, ingreso por Usuario gub.uy, selección de boletera, saldo, mínimo del sitio, selección de importe, Prex integrado y traspaso de eBROU a Chrome.
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
- Validar Prex dentro de Boletera y eBROU en Chrome, incluyendo tarjeta, autorización y acreditación reales. No se simula una recarga exitosa.
- Otros métodos de ID Uruguay, diferentes tipos de boletera y revisión de las condiciones aplicables antes de publicar.

No se infiere el mínimo a partir de una tarifa fija. Pago confirmado y saldo acreditado deberán ser estados diferentes cuando se implemente el cobro real.
