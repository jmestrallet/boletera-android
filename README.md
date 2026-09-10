# Boletera · prueba Android

App independiente y no oficial, de código abierto bajo licencia MIT, con interfaz nativa para el acceso, la boletera y el importe. Maneja el sitio de STM por detrás. **Versión de prueba: puede iniciar una recarga real. Prex se abre dentro de Boletera con perfiles de titular; eBROU conserva Chrome. El dueño reportó tres recargas reales con Prex; la última revisión corrige las esperas, el CVV y el regreso al saldo, y todavía necesita comprobación en su teléfono. No está lista para uso general.**

El código y la documentación se publican para permitir revisión y colaboración. Con 0.1.5 se comprobaron en un emulador Android 16 el ingreso real, saldo/mínimo, preferencias, el inicio real de Prex/eBROU y el regreso al saldo. La integración nueva de 0.1.8 se probó con formularios ficticios; por separado se midió el detector público sin operación. El 10/9/2026 el dueño aportó evidencia de un pago Prex exitoso, confirmación STM y saldo aumentado; esto no valida eBROU ni todas las variantes de CAPTCHA y huella. Consultá [el estado de validación](docs/VALIDACION.md) y [las comprobaciones pendientes de pago](docs/COMPATIBILIDAD-PAGO.md).

La 0.1.9 corrige la conexión segura de Sistarbanc en Android 11; se reprodujo el fallo y se verificó la corrección en un OnePlus 6T. Ver [evidencia TLS](docs/TLS-ANDROID11.md).

Desde **0.2.18**, la home recupera el diseño anterior: título separado, saldo grande y recarga mínima en su propio bloque. Las combinaciones de boletos siguen eliminadas. El icono vuelve al original: un rectángulo redondeado con dos líneas. El aviso de las 72 horas y el acceso a Boletos y tarifas comparten el pie, alineados arriba, para ganar espacio sin achicar los paneles. Se conservan Carga Express, el mínimo de STM, el medio guardado y las pantallas de error.

Desde **0.2.19**, Prex tiene confirmación final y comprobante con el diseño de Boletera. **Confirmar pago** acciona una sola vez el botón original; Carga Express no autoriza este paso. **Volver a mi boletera** sigue el enlace original del proveedor, STM muestra su confirmación y **Ver mi saldo** vuelve a la home con una consulta nueva. Los resultados pendientes o rechazados se muestran como tales. [Cierre del pago](docs/CIERRE-PAGO.md).

En **0.2.20**, se elimina el icono repetido junto a Saldo disponible y el botón Cambiar boletera usa dos flechas de intercambio.

En **0.2.21**, una sesión vencida inicia la recuperación del acceso: intenta aprovechar la sesión web vigente y, si hace falta, pide huella o credenciales. No reenvía recargas. La ayuda de Express respeta No volver a mostrar incluso al soltar antes de tiempo; cambiar de boletera queda exclusivamente en las dos flechas. [Recuperación de sesión](docs/SESION.md).

En **0.2.22**, Siguiente y Continuar llevan al dato de tarjeta faltante, visible sobre el teclado; el CVV se identifica para autocompletado de Android. Las esperas prolongadas permiten revisar la misma solicitud sin reenviarla. Después del comprobante, la confirmación STM se continúa internamente y vuelve la home con saldo consultado de nuevo.

En **0.2.23**, el aviso completo de las 72 horas y Boletos y tarifas usan texto ligeramente menor y un pie más compacto. La vibración usa efectos de toque directos de Android; Probar vibración pide dos pulsos y distingue respuesta táctil apagada, motor no disponible y fallo de petición. Se respeta la preferencia de Android.

En **0.2.24**, el cierre del CAPTCHA deja de exponer la página de Sistarbanc detrás del recorte. Las actualizaciones descargadas muestran un resumen breve de GitHub con **Instalar** y **Ahora no**. Este cartel funciona para las actualizaciones realizadas desde 0.2.24; una app anterior conserva su comportamiento al instalar esta versión.

## Probar en el celular

**Estado actual:** el dueño reportó tres recargas reales con Prex y señaló esperas sin salida, un aviso transitorio, CVV vacío tras autocompletar y un toque adicional para regresar al saldo. La versión 0.1.8 incorpora Prex dentro de la app, apariencia propia y datos del titular guardados por perfil. No es una API de pago nativa: conserva la página y sus verificaciones. La 0.2.19 integra confirmación final, comprobante, resultado STM y regreso a la home. La 0.2.22 aborda esos puntos con pruebas ficticias. El CVV efectivamente entregado por Google depende del proveedor y de los datos guardados; se conserva el ingreso manual visible. Ver [investigación y evidencia](docs/INTEGRACION-NATIVA-PAGO.md).

**[Descargar la versión 0.2.24](https://github.com/jmestrallet/boletera-android/releases/tag/v0.2.24)** · [Todas las versiones y APK históricos](https://github.com/jmestrallet/boletera-android/releases)

El candidato local 0.1.11 reemplaza el resumen y los datos del titular por pantallas nativas. Mantiene la página del proveedor detrás y muestra el CAPTCHA original en un panel. Su validación del pago completo sigue pendiente; no se publicó una nueva versión remota. El archivo local es `outputs/boletera-prueba-0.1.11.apk`.

La **0.2.10** corrige [Express](docs/MODO-EXPRESS.md): un atajo secundario por pulsación sostenida que aparece solo con los datos habituales completos, sin configuración ni encendido/apagado. La recarga común sigue como principal. Atrás desde la selección de boleteras vuelve al saldo conservando la sesión. Conserva respuesta al tacto, pulsación larga sobre la boletera y selecciones animadas. El diseño incluye: saldo protagonista, importe en un panel, tipografía Google Sans, iconos Material Symbols, modo claro/oscuro y transiciones nativas. Conserva Configuración y las actualizaciones desde GitHub. [Diseño, referencias y comprobaciones](docs/DISENO.md). El archivo es `outputs/boletera-prueba-0.2.10.apk`.

Desde 0.1.15, las próximas versiones publicadas en este repositorio se pueden descargar desde Configuración, sin buscar el archivo a mano. Se incluyen las publicaciones de prueba porque esta app todavía está en prueba. Desde 0.2.4, **Actualizar ahora** descarga, verifica y abre automáticamente la confirmación de instalación de Android. La primera vez puede pedir **Permitir desde esta fuente**: al volver con el permiso habilitado, continúa automáticamente. Si cancelás, queda **Instalar actualización** para reintentar. Para llegar desde una versión anterior a 0.2.4 todavía hay que tocar ese botón después de descargarla. Una actualización conserva los datos guardados. Si tu candidato local es más nuevo que GitHub, no ofrece volver a una versión anterior.

En **Assets**, elegí el archivo `.apk`. Las descargas de GitHub son públicas y se pueden compartir por enlace. Las versiones anteriores se conservan con su SHA-256. La 0.1.0 se publica como archivo histórico: es anterior al código subido al repo y su descarga automática de «Source code» no contiene el código de esa versión.

1. Pasá `outputs/boletera-prueba-0.2.24.apk` a tu Android e instalala. Requiere Android 8 o posterior y Android System WebView actualizado. La actualización usa la misma firma que las versiones anteriores; instalala encima para conservar el acceso guardado.
2. Abrí **Boletera · Prueba** e ingresá tu documento y contraseña de Usuario gub.uy.
3. Si querés, activá **Guardar acceso con huella**. El celular debe tener biometría fuerte configurada. La app pedirá autorización para cifrar el acceso; en el siguiente ingreso, pedirá biometría para descifrarlo.
4. Si aparece un CAPTCHA reconocido, se mostrará únicamente su recorte interactivo. Completalo y tocá **Ya completé la verificación**.
5. Elegí la boletera operativa una vez: la app la recuerda por cuenta y la selecciona en los próximos ingresos si sigue habilitada. Podés usar **Cambiar boletera**. Compará saldo y mínimo con STM y elegí el monto.
6. Elegí Prex o eBROU. Si usás Prex por primera vez, **Agregar datos** abre directamente el formulario del titular. Con un solo titular guardado se usan esos datos; con varios podés cambiar la selección. El nombre para guardar es opcional.
7. Tocá **Continuar**: Prex continúa dentro de Boletera y eBROU en Chrome. La app prepara una solicitud real; autorizala solo si querés realizar esa recarga. Al volver, consultá el saldo y revisá la confirmación del proveedor.

Si el acceso, CAPTCHA o certificado falla, la app se detiene. No cambia el servicio de autocompletado de Android; los perfiles propios son solo datos ordinarios del titular. La compatibilidad de Google con el formulario dentro del WebView todavía requiere prueba en el teléfono. Para reportar el problema alcanza con el texto del mensaje y el modelo/versión de Android; no compartas contraseñas ni números bancarios.

Desde 0.2.1, salir de Prex vuelve al saldo y permite iniciar otra recarga sin revisión manual. Al regresar de eBROU también se actualiza el saldo. La app ya no guarda solicitudes como pagos pendientes ni conserva enlaces para reabrirlas. Esto no confirma ni cancela una operación del proveedor. Se mantiene la protección contra toques duplicados mientras se abre el pago.

Desde 0.2.2, al abrir la app con un acceso guardado se solicita la huella automáticamente. Cancelar permite reintentar con el botón o ingresar manualmente; cerrar sesión no vuelve a abrir la huella por sí solo.

Desde 0.2.3, la preparación de la recarga es más compacta y la acción principal queda fija abajo. También quedan visibles las acciones de guardar datos y continuar en las pantallas nativas de Prex. [Auditoría de la experiencia de pago](docs/AUDITORIA-PAGOS-UX.md).

Los avisos, incluido el fallo de desbloqueo, aparecen flotando en la parte inferior sin mover el contenido; se cierran solos o con la cruz.

La **0.2.11** incorpora una explicación de Express antes del primer uso, con opción de no volver a mostrar, y una [guía de boletos y tarifas](docs/BOLETOS-Y-TARIFAS.md) accesible al pie de la boletera y desde Configuración. Precios y ejemplos revisados el 10/9/2026; se leen sin conexión y tienen enlaces a las fuentes.

La **0.2.13** corrige el desafío ampliado del CAPTCHA: la casilla inicial queda detrás, la consigna y el pie se ajustan completos al panel y el teclado se oculta. También habilita capturas durante el pago para reportar errores. Se conserva la página original y sus verificaciones.

## Qué incluye

Desde 0.2.9, el formulario de tarjeta Prex también tiene interfaz nativa: número, vencimiento, CVV y Continuar. Boletera mantiene esos datos solo durante el uso del formulario, borra el CVV al continuar y limpia sus campos al salir o pasar al fondo. El envío sigue a cargo de la página original de Sistarbanc. Si aparecen consentimientos o verificaciones adicionales, se muestra ese paso original. El autocompletado de número/vencimiento depende del servicio de Android y requiere prueba en el teléfono. [Alcance y validación](docs/TARJETA-NATIVA.md).

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
