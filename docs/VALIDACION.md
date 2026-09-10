# Estado de la prueba — 10 de septiembre de 2026

## Versión vigente: 0.2.23

El pie conserva el aviso completo de las 72 horas y Boletos y tarifas. Ambos textos pasan a 11 sp con interlineado de 14 sp, la guía reserva 90 dp y la separación desde Recarga mínima baja a 12 dp. Se conserva el área táctil de 48 dp. La consulta de pantalla del teléfono dio 1080 × 2340, 420 dpi y letra 1×; no se cambió su configuración.

La home se probó en emulador con esas medidas: prueba inicial aprobada en 9,883 s (`outputs/footer-fit-0.2.23.txt`). Captura oscura revisada (`outputs/footer-home-0.2.23.png`): aviso en dos renglones, guía completa y home sin desplazamiento, también comprobada con saldo negativo. Con letra mayor el contenido puede desplazarse para preservar su lectura.

El dueño informó que no sentía la vibración. En su teléfono la respuesta táctil estaba activada, intensidad 2; el registro consultado no contenía peticiones de Boletera. No se afirma que eso identifique la causa exacta. Se agregó una vía directa con efectos CLICK y DOUBLE_CLICK, uso TOUCH y permiso normal VIBRATE, respetando HAPTIC_FEEDBACK_ENABLED. La interfaz distingue petición, ajuste apagado, falta de motor y error sin confundir petición con percepción física.

Cinco pruebas Android aprobadas en 14,321 s (`outputs/footer-haptics-final-0.2.23.txt`): botón real de prueba, respeto del ajuste apagado y restauración del ajuste del emulador, home/guía/ayuda y respuesta de controles sin duplicados. El servicio Android del emulador registró DOUBLE_CLICK finalizado en 166 ms y pulsos CLICK (`outputs/haptic-service-0.2.23.txt`). No demuestra todavía que se sienta en el teléfono del dueño.

Ocho pruebas JVM, release y lint aprobados; compilación final en 26 s. No cambió JavaScript ni el recorrido de pago. APK código 41, SHA-256 `c3453da3c4ecd3e5f235287e81531dbcec82450fc475b0e14eadff757e7da315`, 8.612.927 bytes, misma firma. No se instaló ni se ejecutó la corrección en el teléfono físico y no se hicieron operaciones financieras.

## Versión anterior: 0.2.22

El dueño reportó tres recargas reales y describió cuatro molestias: Express sin avance visible, aviso de error seguido por avance normal, CVV vacío tras la huella sin foco visible y otra pantalla antes del saldo. Su relato acredita esas observaciones; no permite atribuir con certeza el aviso original a un único componente. Se corrigieron causas reproducibles y se agregó una salida para esperas prolongadas.

58 pruebas JavaScript, ocho JVM, release/debug/pruebas Android y lint aprobados; compilación final en 33 s. El adaptador no declara rechazo solo porque vuelve a habilitarse un botón: exige un aviso persistente. Las advertencias transitorias conservan el bloqueo de duplicados. Los errores de una navegación anterior se descartan; un fallo persistente sigue visible. La espera de 30 segundos ofrece revisar la misma solicitud o volver al saldo, sin reenviar ni desbloquear una confirmación financiera.

Ocho pruebas Android iniciales aprobadas en 30,086 s (`outputs/polish-android-initial-0.2.22.txt`): validación/limpieza, teclado y campo faltante, envío único, espera de Express con recuperación tardía, página sin contenido reconocido, error transitorio/persistente y comprobante hasta saldo sin otro toque. La tanda de trece pruebas aprobó en 86,939 s (`outputs/polish-android-final-0.2.22.txt`), incluyendo acceso/boleteras/Express y recuperación de sesión. Después del último ajuste que conserva el indicador de regreso durante las navegaciones STM, se repitieron las cuatro de progreso y cierre: aprobadas en 16,237 s (`outputs/polish-payment-published-0.2.22.txt`).

Foco, explicación y CVV visibles con el teclado abierto comprobados en pantalla habitual y en 360 × 640 dp: letra 1×, 4,393 s (`outputs/cvv-small-keyboard-0.2.22.txt`), y letra 1,5×, 4,264 s (`outputs/cvv-small-large-text-0.2.22.txt`). Se revisaron capturas oscuras de los tres tamaños. La prueba completa rellena número/vencimiento ficticios, usa Siguiente, comprueba foco en CVV y ausencia de envío, luego completa CVV y exige un solo Continuar.

Se agrega la indicación de CVV para autocompletado Android; el suministro efectivo por Google no está probado en el teléfono ni se garantiza a partir del soporte de Chrome. El CVV no se guarda en Boletera. El nuevo retorno automático respeta la confirmación original de STM y vuelve a consultar saldo/mínimo; no calcula una acreditación local.

APK código 40, SHA-256 `8f627d96f2a6aa04674e29ed2bd1a8b88c1e5fe94c8a84e3985af89c63a614f9`, 8.612.907 bytes, misma firma. No se modificó el teléfono físico ni se hicieron operaciones financieras en estas pruebas.

## Versión anterior: 0.2.21

La sesión vencida intenta recuperar el acceso con la sesión web todavía disponible; si necesita identificación, vuelve al acceso y solicita huella con credenciales guardadas o permite ingreso manual. Se descartan el importe pendiente y el avance de pago para impedir reenvíos. No volver a mostrar se guarda al marcarlo, incluso al cerrar la ayuda o cancelar la pulsación de Express. Solo las flechas cambian de boletera; el panel de saldo no responde con esa acción.

57 pruebas JavaScript, ocho JVM, release/debug/pruebas Android y lint aprobados; compilación completa en 41 s. Diez pruebas Android finales aprobadas en 71,637 s. Las comprobaciones Android usan solicitudes interceptadas y datos ficticios: recuperación web, ingreso manual, vencimiento tras envío al proveedor sin reenvío, HTTP con expiración frente a una falla real, persistencia de la ayuda tras recrear la actividad, pulsación cancelada, área de saldo inerte, flechas habilitadas y recorrido completo de pago/retorno. Para HTTP se entrega explícitamente el callback de red junto al cuerpo interceptado porque WebView no lo emite de forma fiable en esa simulación. Evidencia final: `outputs/session-gestures-final-0.2.21.txt`.

Pantalla de sesión vencida revisada en tema oscuro, 1080 × 2280, 420 dpi y letra 1× (`outputs/session-expired-manual-0.2.21.png`). La solicitud automática de huella tras vencer la sesión queda pendiente de comprobación en el teléfono con acceso guardado; no se infiere del ingreso manual simulado.

APK código 39, SHA-256 `54835246c82d49fc590837ea45855fff262da4acb3ac4287fda25e9880902d37`, 8.612.879 bytes, misma firma. No se modificó el teléfono físico ni se hicieron operaciones financieras. [Alcance de la recuperación](SESION.md).

## Versión anterior: 0.2.20

Cambio visual acotado: se retira el icono junto a Saldo disponible y se reemplaza el de Cambiar boletera por dos flechas horizontales opuestas. El botón conserva la misma acción, estado habilitado y nombre accesible. No cambia la lógica del pago ni el resto de la home.

54 pruebas JavaScript, ocho JVM, release y lint aprobados en 44 s. Debug y pruebas Android compilados en 16 s. Tres pruebas Android existentes aprobadas en 8,048 s (`outputs/home-icons-0.2.20.txt`): home con saldo positivo/negativo, mínimos variables, ayuda, guía y respuesta al tacto. Captura oscura revisada en 1080 × 2280, 420 dpi y letra 1× (`outputs/home-icons-0.2.20.png`): flechas visibles, saldo sin icono y home sin desplazamiento.

APK código 38, SHA-256 `6194c1ef9a8bdffeadf0dfaa8b7ce01cb30601487e8d60103e311768ef553922`, misma firma. No se modificó el teléfono físico ni se realizaron operaciones financieras.

## Versión anterior: 0.2.19

El dueño aportó el 10/9/2026 la primera evidencia de pago real Prex: confirmación, comprobante exitoso, confirmación STM y saldo aumentado. El cierre quedaba en páginas originales dentro del panel. La nueva integración cubre confirmación final, comprobante, resultado STM y vuelta a la home con una consulta nueva. Las capturas privadas y referencias reales no se publican.

54 pruebas JavaScript, ocho JVM, release/debug/pruebas/lint aprobados; compilación final en 34 s. Las seis pruebas JS nuevas cubren confirmación explícita, bloqueo de duplicados, importe distinto, tarjeta sin enmascarar, controles adicionales, CAPTCHA, éxito/rechazo/pendiente, enlace original de retorno y reconocimiento estricto de STM. La insignia invisible de reCAPTCHA no oculta la confirmación nativa.

El recorrido Android completo con solicitudes interceptadas aprobó en 8,012 s. La prueba de pantalla 360 × 640 dp y letra 2× detectó que el scroll del detalle podía conservarse al cambiar de resultado; se corrigió reiniciándolo por etapa y aprobó en 7,664 s (`outputs/payment-completion-final-large-text-0.2.19.txt`). Con letra ampliada se desplaza el contenido y la acción principal queda accesible.

La tanda normal de cinco pruebas Android aprobó en 60,324 s (`outputs/payment-completion-final-regression-0.2.19.txt`): cierre completo, regresión de acceso/boleteras/Prex y tres casos de Carga Express. Después del último ajuste visual que identifica a STM en el encabezado, el cierre completo volvió a aprobar en 7,562 s (`outputs/payment-completion-published-0.2.19.txt`). Se verifican una sola confirmación y un solo retorno, comprobante desplegable, continuación STM, saldo nuevo leído del sitio y ausencia de otra solicitud de recarga. Pantalla normal: 1080 × 2280, 420 dpi, tema oscuro. Capturas de confirmación, comprobante, éxito STM y home revisadas.

APK código 37, SHA-256 `740aaa1720f49e52ddd7f1d788eeca75c72fff39b54523fcad0fae0962e0eafd`, 8.611.068 bytes, misma firma. No se modificó el teléfono físico ni se hicieron operaciones financieras durante esta corrección. La integración nueva necesita todavía comprobación real en el teléfono; no se infiere su validación del pago anterior. [Alcance y límites](CIERRE-PAGO.md).

## Versión anterior: 0.2.18

Se recuperan el texto original completo de las 72 horas y Boletos y tarifas con icono de información. Los dos grupos se alinean por arriba; el acceso ocupa dos renglones y tiene área táctil mínima de 48 dp. No cambia el resto de la home.

48 pruebas JavaScript, ocho JVM, release/debug/pruebas/lint aprobados; compilación en 48 s. HomeFitTest aprobó en 4,816 s (`outputs/footer-aligned-0.2.18.txt`): pantalla 1080 × 2280 a 420 dpi y letra 1×, saldo positivo/negativo con desplazamiento máximo cero, mínimos variables y apertura de guía. Captura oscura revisada (`outputs/footer-aligned-home-0.2.18.png`): ambas primeras líneas e iconos alineados y todo el pie visible.

APK código 36, SHA-256 `17e30d5617b4ffeea6fdd0ef7195b03b1d8eb8c5ce33c7605137ec534d0a3543`, misma firma. No se modificó el teléfono físico ni se hicieron operaciones financieras.

## Versión anterior: 0.2.17

Ajuste limitado al pie de la home: aviso abreviado de viajes de las últimas 72 horas sin descontar a la izquierda y acceso a Boletos a la derecha. Los tamaños, paneles y separaciones superiores no cambian. Se conserva el icono original.

48 pruebas JavaScript, ocho JVM, release/debug/pruebas/lint aprobados; compilación final en 34 s. HomeFitTest aprobado en 4,665 s (`outputs/footer-fit-0.2.17.txt`) con 1080 × 2280, 420 dpi y letra 1×: saldo positivo y negativo, mínimo actualizado, ayuda y acceso a la guía; desplazamiento máximo cero. Captura oscura revisada: `outputs/footer-home-0.2.17.png`. El espacio útil y la escala de letra varían entre teléfonos; el texto se conserva sin recorte si hace falta desplazarse en otra configuración.

APK código 35, SHA-256 `5b57623bf34dd2b88e3aa10602b15223b8fd43a90d19fbcfc4a902acd6e6afe4`, misma firma. No se instaló nada en el teléfono ni se realizaron operaciones financieras.

## Versión anterior: 0.2.16

Se restaura el icono original: rectángulo redondeado con dos líneas. El recurso de interfaz reproduce las coordenadas, proporciones y grosor del dibujo original de 0.2.13; los recursos del lanzador se recuperan sin diferencias respecto de esa revisión. No cambia la home ni el recorrido de pago.

48 pruebas JavaScript, ocho JVM, release y lint aprobados; compilación en 39 s. Por tratarse de una restauración de recursos gráficos no se repitió el recorrido Android ya validado en 0.2.15. APK código 34, SHA-256 `52d11fec362b590c32378eb0a8d806bf6853f85a0306022c6706d3e1218fdf96`, misma firma. No se instaló nada en el teléfono ni se hicieron operaciones financieras.

## Versión anterior: 0.2.15

Se restaura la home previa a la compactación: título separado, saldo grande y mínimo en su bloque. No vuelve el carrusel de boletos. El icono representa una tarjeta con un ómnibus de perfil, sin ondas, y comparte el dibujo con el lanzador Android.

Se consultaron únicamente resolución, densidad y escala de letra del teléfono conectado: 1080 × 2340, 420 dpi, letra 1×. No se instaló ni modificó nada en ese teléfono. La comprobación se hizo en un emulador con esas medidas, tema oscuro y datos ficticios. El primer restablecimiento exacto necesitaba 16 dp de scroll; se eliminó el espacio extra entre aviso y guía del pie, conservando tamaños y paneles.

Tres pruebas Android finales aprobaron en 11,774 s (`outputs/restored-home-final-0.2.15.txt`): home completa con desplazamiento máximo cero para saldo positivo y negativo, mínimos variables de $564/$731 y ayuda, acceso a guía, pulsación larga, cancelación y bloqueo de duplicados. Capturas revisadas: `outputs/restored-home-dark-0.2.15.png` y `outputs/restored-home-negative-0.2.15.png`. En pantallas menores o con letra ampliada se mantiene el desplazamiento para conservar el diseño y la legibilidad.

48 pruebas JavaScript, ocho JVM, release y lint aprobados; compilación final en 29 s. APK código 33, SHA-256 `a23973c722a83aac3dbe878b60794ebce172501df4bc1678a432c0bad9fc1e03`, misma firma que las versiones anteriores. Se conservan Carga Express y la pantalla de errores. No se hicieron operaciones financieras; la aceptación visual en el teléfono queda pendiente.

## Versión anterior: 0.2.14

Home compacta sin combinaciones de boletos; identificación/saldo/mínimo en un solo panel y símbolo unificado de tarjeta sin contacto, incluido el lanzador Android. Carga Express usa el medio guardado y la ayuda aclara que el mínimo cambia según la cuenta/consulta. Se retiró la referencia a titular habitual. La guía de tarifas permanece aparte.

48 pruebas JavaScript y ocho JVM aprobadas. Release/lint iniciales en 33 s y compilación final release/debug/pruebas/lint en 29 s. El mínimo sigue leyéndose de STM: las pruebas verifican valores de $564 y $731, tanto en home/ayuda como al preparar Express. Carga Express con eBROU funciona sin perfil Prex, consume una sola solicitud y conserva el importe actualizado; la prueba detiene el recorrido antes de navegar al banco. El medio desconocido o el mínimo ausente no habilitan Express.

Android: seis pruebas en 360 × 640 dp con letra normal (12,736 s, `outputs/home-errors-express-small-0.2.14.txt`). Después del ajuste final del error, home y error completos sin desplazamiento (6,233 s, `outputs/home-errors-final-small-0.2.14.txt`). Cinco pruebas con tamaño habitual aprobaron en 56,537 s (`outputs/home-errors-flow-final-normal-0.2.14.txt`): home, error, recorrido de acceso/recarga común/Express interceptado y dos de respuesta táctil. Dos pruebas finales con letra 2× aprobaron en 5,295 s (`outputs/error-express-large-text-0.2.14.txt`): acceso a detalle/salida y confirmación de la pulsación. Con texto ampliado se permite desplazamiento para conservar legibilidad.

La primera comprobación de home, todavía con el carrusel, detectó 29 dp de desplazamiento. La versión final lo elimina junto con el carrusel y el espacio vacío del host; las comprobaciones finales exigen desplazamiento máximo cero en tamaño normal. Se revisaron las capturas de home y error. Las capturas `*-normal-0.2.14.png` son del tema claro; el modo nocturno del emulador no reemplazaba la preferencia explícita de la app.

La pantalla de error cubre fallos controlados del recorrido principal y de Prex. Clasifica según el mensaje conocido de la app, conserva detalle/referencia y nunca reintenta una recarga por sí sola. Enviar error al desarrollador permanece deshabilitado con Próximamente: no envía ni recopila nada adicional. No es recuperación de un cierre abrupto del proceso Android.

APK código 32, SHA-256 `8a57bcf3bb243b764dafc10aed349cbc19677664d23a4e72cbddc089867e9c44`, misma firma de las versiones anteriores. No se realizaron pagos reales ni se modificó el teléfono físico. Autorización/acreditación y experiencia en el teléfono quedan pendientes de comprobación humana.

## Versión anterior: 0.2.13

Express recupera doce líneas radiales y confirma la preparación inmediatamente; el anillo no termina antes de aceptar el gesto, independientemente de la escala de animaciones de Android. El saldo recorre combinaciones comunes de 1 h y 2 h con sobrante, avance manual y automático.

48 pruebas JavaScript y 11 JVM aprobadas. Release, debug, pruebas Android y lint compilados correctamente en 29 s. Las pruebas JVM incluyen saldos insuficientes, negativos, más de 1.400 saldos distintos y Long.MAX_VALUE: cantidades enteras, opciones distintas, costo dentro del saldo y selección acotada.

En Android, la primera tanda pasó cancelación, pulsación completa, exclusión de duplicados y recorrido común/Express con páginas interceptadas; falló la expectativa sobre cuál mezcla equilibrada iba tercera. Se definió el desempate por mayor aprovechamiento del saldo (7 de 1 h + 8 de 2 h para $1.000) y se verificó también en JVM. La prueba del carrusel pausa su avance durante las comprobaciones manuales y lo reanuda para comprobar el avance por tiempo real.

Validación final: dos pruebas Android en pantalla habitual en 12,099 s (`outputs/express-feedback-final-normal-0.2.13.txt`); cuatro en pantalla 360 × 640 dp, letra 2× y animaciones desactivadas en 12,948 s (`outputs/express-feedback-final-small-0.2.13.txt`). Capturas revisadas: señales de preparación legibles, mezclas completas y contenido desplazable. Se comprobaron cancelación antes de completar, un único inicio, señal inmediata, interacción independiente de la boletera, pausa/reanudación y ocultamiento con saldo negativo. El recorrido interceptado anterior pasó en la tanda de 60,445 s (`outputs/express-feedback-normal-0.2.13.txt`), cuyo único fallo era la expectativa de orden ya corregida.

APK código 31, SHA-256 `a99590eff6333099407c40adc6da3c56d669db7fb4b79c252d8d62e368310639`, misma firma que 0.2.12. No se realizó ninguna operación financiera real. La velocidad de respuesta de STM y el resultado en el teléfono quedan para comprobar al actualizar.

## Versión anterior: 0.2.12

Corrección de la superposición del ancla reCAPTCHA: su índice de apilamiento elevado podía cubrir la consigna del iframe de desafío. Ahora queda debajo. El desafío expandido dispone del panel, cierra el teclado y usa un viewport de navegador con alto suficiente antes de ajustar el rectángulo completo. Los controles nativos se ocultan temporalmente y los campos de tarjeta permanecen en memoria para volver. En la vista original el botón de regreso queda fuera del navegador. Se permite capturar durante todo el pago, a pedido del usuario.

48 pruebas JavaScript, ocho JVM, release y lint aprobados. Seis pruebas Android aprobaron en 33,043 s con pantalla 360 × 640 dp, letra 2× y animación desactivada (`outputs/challenge-tests-0.2.12.txt`): desafío en titular/tarjeta, adaptación del widget retenido y tres del formulario nativo. Se comprueban consigna y pie alcanzables por toques, capturas permitidas, un solo iframe de cada tipo, conservación de campos al cerrar el desafío y limpieza habitual al enviar/salir.

Se amplió la prueba a un desafío ficticio centrado de 680 px de alto. La primera ejecución tocaba antes de que el recorte reflejara el nuevo viewport; se corrigió la sincronización del test para esperar coincidencia de geometría. Ambos casos centrados aprobaron en 13,948 s en pantalla chica (`outputs/challenge-centered-small-0.2.12.txt`). Todas las páginas e iframes de estas pruebas se sirven localmente: no se resuelve un CAPTCHA de Google real ni se realiza una operación financiera.

Tres pruebas finales también aprobaron con tamaño habitual y animación habilitada (61,593 s, `outputs/challenge-final-0.2.12.txt`): ambos desafíos centrados y el recorrido completo de acceso, selección, recarga común y Express. Las capturas revisadas muestran consigna y pie completos.

Compilación final release/debug/pruebas/lint: 32 s. APK código 30, SHA-256 `d2afb4176c2b8bd9c0800acd2c2b7d5876217112e50eb710440ec48fa7c33a96`, firma idéntica a la versión anterior. El caso del teléfono reportado por el usuario queda pendiente de comprobar con la actualización.

## Versión anterior: 0.2.11

Ayuda previa de Express con aceptación explícita y preferencia de no mostrar; guía de boletos local accesible desde saldo y Configuración, con fuentes públicas verificadas el 10/9/2026. [Contenido y fuentes](BOLETOS-Y-TARIFAS.md).

48 pruebas JavaScript, ocho JVM, release y lint aprobados. Cinco pruebas Android iniciales aprobaron en 56,337 s (`outputs/guide-tests-0.2.11.txt`): guía, aceptación sin selección implícita, gestos/visibilidad de Express y recorrido completo interceptado. Este último comprueba cancelar el aviso sin abrir pago ni guardar la casilla, aceptar con la casilla, continuidad única hasta tarjeta y siguiente pulsación sin aviso. No se envían datos bancarios reales.

Dos pruebas aprobaron con pantalla 360 × 640 dp, letra 2× y animación desactivada (6,711 s). Tras el ajuste del color del aviso y limpieza de su preferencia al olvidar el acceso, tres pruebas finales aprobaron en ese tamaño (6,539 s, `outputs/guide-final-small-0.2.11.txt`), incluyendo acceso a la guía desde Configuración sin sesión. Se inspeccionaron capturas claras/oscuras, el detalle metropolitano y la pantalla pequeña; el texto se desplaza y las acciones permanecen accesibles.

APK código 29, SHA-256 `5abd5a2d5fa4a80bb6d359be3ef68b2ec83055380df76dbbdd38d35ffdd40321`; firma idéntica a 0.2.10. Build release/JVM/lint: 41 s; compilación final de pruebas Android: 12 s. Teléfono del usuario sin modificar. Pago y acreditación reales siguen pendientes; esta versión no agrega una cuenta regresiva de bloqueo.

## Versión anterior: 0.2.10

Express se rehízo como atajo puntual secundario: se oculta si faltan datos habituales, no tiene configuración ni estado encendido/apagado y no reemplaza Recargar boletera. Usa Prex, mínimo vigente y titular predeterminado. Además, Atrás desde la selección de boleteras regresa al saldo sin cancelar el acceso. [Flujo y condiciones](MODO-EXPRESS.md).

Pasaron 48 pruebas JavaScript, ocho JVM y compilación/lint. En Android pasaron seis pruebas (57,461 s): dos de Express, el recorrido completo con login/selección/recarga común/atajo y tres del formulario nativo. El recorrido verifica ambos botones Atrás sin nuevos accesos, una única solicitud por pulsación sostenida, avance de resumen y titular una vez, llegada al número de tarjeta y cero envíos de tarjeta. Registro: `outputs/express-shortcut-0.2.10.txt`. Cinco pruebas de Express/formulario también pasaron en 360 × 640 dp con texto 2× y animación desactivada (13,488 s): `outputs/express-shortcut-small-0.2.10.txt`.

El usuario informa que al tocar el número de tarjeta el sistema ofrece la tarjeta guardada con huella. Se conserva esa integración y se agrega foco inicial al llegar directamente desde Express. Este comportamiento en el nuevo atajo no se verificó en su teléfono; la prueba interceptada no acredita un pago real ni el funcionamiento de un CAPTCHA real.

Compilación final release/androidTest y lint aprobados (23 s). Se revisó la captura del atajo debajo del botón común y su anillo durante la pulsación; demo con datos ficticios y sin motor financiero, aprobada en 6,348 s. APK código 28, SHA-256 `86097273ffca9ff207a955f5f3f7ce2d4ef04be074c1da52803d348719a7cbb3`; firma idéntica a la versión anterior.

## Versión anterior: 0.2.9

Formulario nativo de tarjeta Prex con datos temporales y continuación mediante el formulario original de Sistarbanc. [Alcance, protección y límites](TARJETA-NATIVA.md).

Pasaron 44 pruebas JavaScript, ocho JVM, compilación release y lint. En Android pasaron nueve pruebas conjuntas (46,166 s): tres del nuevo formulario, recorrido STM/Prex interceptado, dos de Express, dos de gestos y conservación del WebView. Registro: `outputs/native-card-regression-0.2.9.txt`. Las tres pruebas nuevas también pasaron en 360 × 640 dp (10,450 s) y con texto 2×/animación desactivada (13,044 s), antes del ajuste final que separa vencimiento y CVV en filas con texto grande.

Se verifica un solo envío explícito y exacto de datos ficticios, sin valores de tarjeta en las capturas de estado; limpieza del CVV al continuar y de los campos al salir/pasar al fondo; protección y restauración de la captura de pantalla. Se revisan los modos claro y oscuro con el formulario de producción y datos vacíos. No se utilizó una tarjeta real ni se contactó al proveedor en estos tests. Google en el teléfono, CAPTCHA, autorización y acreditación continúan pendientes.

Después del ajuste de letra grande pasaron nuevamente las tres pruebas del formulario (9,788 s), en 360 × 640 dp, texto 2× y animación desactivada. Registro: `outputs/native-card-final-0.2.9.txt`. Compilación final release/debug/androidTest y lint aprobados (28 s). APK código 27, SHA-256 `5c77085cb6c65d4f9edc187beccacacfad496b0109b1111def63a7ea5e62df74`, firma comprobada igual a 0.2.8.

## Versión anterior: 0.2.8

Tras el feedback del usuario, se elimina la inclinación de la boletera, se agrega pulso al toque normal y se amplían las respuestas visuales de botones e importes. Los cambios claro/oscuro interpolan colores y la entrada de pantalla aumenta su desplazamiento. Configuración incorpora Probar vibración con resultado de aceptación de Android; no afirma una sensación física comprobada.

Pasaron diez pruebas Android (48,001 s): cinco de diseño, dos de gestos con pulso diferenciado de toque normal/pulsación larga, dos de Express y el recorrido STM/Prex interceptado. Compilación release y lint aprobados. Registro: `outputs/feedback-0.2.8.txt`. La emisión se comprobó con captura de eventos; la vibración física en Xiaomi continúa pendiente de evaluación por el usuario.

APK código 26, SHA-256 `1561B768577B90B8643B2F1A92440DECCF8CED8F6140E8F435EB85A1C826C20C`. No se cambiaron el acceso ni el mecanismo de pagos y no se realizó una operación real.

## Versión anterior: 0.2.7

Modo Express con activación por pulsación sostenida, respuesta táctil y configuración por cuenta/boletera. El botón de recarga usa el mínimo vigente y evita repetir la selección de importe y medio. La autorización continúa en el proveedor. [Comportamiento y límites](MODO-EXPRESS.md).

Pasaron 40 pruebas del adaptador, siete pruebas JVM, compilación release y lint. En Android pasaron cinco pruebas finales en tamaño habitual (34,946 s): activación y cancelación, aislamiento de preferencias y guardas de Express, dos pruebas de gestos y el recorrido interceptado completo con una tercera recarga Express y control de duplicados. También pasaron tres pruebas en 360 × 640 dp y dos con texto 2× y animación desactivada. Registros: `outputs/express-final-0.2.7.txt`, `outputs/express-small-0.2.7.txt` y `outputs/express-large-text-0.2.7.txt`.

La demostración recorre la interfaz real y activa una preferencia para una cuenta ficticia, sin pulsar el botón de recarga. Se revisó visualmente el anillo durante la pulsación y el estado activado. No se comprobó la vibración física en Xiaomi ni se completó una autorización o acreditación real.

APK código 25, SHA-256 `375B32C8093A94F0F25CD20881CED50E1CD98C10646A87AB060B01B87266D5B1`. La incidencia de ingreso reportada para 0.2.4 dejó de ocurrir según el usuario; su causa no quedó determinada. Se conserva el diagnóstico persistente de 0.2.6.

## Versión anterior: 0.2.6

El usuario reportó un fallo de ingreso en Xiaomi con 0.2.4. La captura sólo muestra la pantalla genérica porque el aviso con la causa desaparecía después de unos segundos. Se corrige ese defecto: la causa de un bloqueo permanece en el contenido y se muestra la referencia de etapas. Los avisos transitorios de acceso conservan su comportamiento flotante.

Tres pruebas Android aprobadas (39,14 s): error persistente más allá del tiempo del aviso y regreso al inicio; aviso de huella flotante que desaparece y puede repetirse; recorrido de ingreso y recarga interceptado. Compilación release y lint aprobados. Registro: `outputs/login-diagnostic-0.2.6.txt`.

La página pública inicial de STM respondió HTTP 200 desde esta PC. Esto no verifica el ingreso del usuario. La comparación entre 0.2.3 y 0.2.4 no muestra cambios en StmEngine, stm-adapter ni AccessVault; no permite descartar otros problemas. Sigue pendiente recibir la referencia del teléfono y reproducir la causa del fallo real. Esta versión mejora el diagnóstico y no se presenta como reparación comprobada del login.

APK código 24, SHA-256 `C0B9083EFD324BD0E99066BBF88A8E7CB3F095B20E7318E0CD95FFC7535470EA`. No se ingresaron credenciales reales, no se inició ningún pago ni se instaló esta entrega en un teléfono físico.

## Versión anterior: 0.2.5

Respuesta al tacto: botones con compresión y retorno, pulsación larga de la boletera y selección animada de importes. Ocho pruebas Android aprobadas en tamaño habitual: dos de gestos y emisión de pulsos, cinco de interfaz y una del recorrido STM/Prex interceptado. Otras siete aprobadas en 360 × 640 dp y con animación desactivada. La pulsación larga selecciona una sola vez, deslizar no selecciona, cancelar no activa y los controles deshabilitados no ejecutan acciones. Compilación release y lint aprobados.

La respuesta táctil se comprobó mediante captura de eventos en el emulador, no mediante valoración física en un teléfono. La grabación usa componentes reales de la app y datos ficticios; no prueba un pago real ni una tasa de cuadros. Registro de pantalla chica: `outputs/touch-small-no-motion-0.2.5.txt`. Demostración: `outputs/motion-demo-0.2.5.txt` (una prueba adicional aprobada).

APK 0.2.5-prueba, código 23, SHA-256 `00970BFFE9917D0EA880F03EBB7ED01560FB70441A62EA0CBCEC5C11BDF4F1B8`. Se entrega para actualizar desde GitHub. La conversión del formulario de tarjeta de Sistarbanc a campos nativos no forma parte de esta versión.

## Versión anterior: 0.2.4

Actualizar ahora conserva la descarga y verificación existentes y solicita abrir el instalador al completarlas. Si la app está en segundo plano, espera al regreso. La solicitud se consume antes de abrir Android para evitar repeticiones. Al habilitar la instalación desde Boletera y volver, se reanuda automáticamente; rechazar el permiso conserva la opción manual sin insistir.

Pasaron seis pruebas Android: apertura automática una sola vez y solo en primer plano, descarga pública con digest correcto, rechazo de alteraciones/retrocesos, validación de identidad y firma, selección de versiones y apertura real del instalador con permiso temporal. Una séptima prueba usó la pantalla real de permisos de Android y comprobó tanto volver sin habilitarlo como habilitarlo y continuar. No se confirmó ninguna instalación.

La prueba del disparo automático usa un estado de descarga verificada simulado y una APK local; las comprobaciones de descarga, integridad e instalador se ejecutan por separado. Esto no representa haber instalado una versión nueva de extremo a extremo. Registros: `outputs/automatic-install-0.2.4-test.txt` y `outputs/automatic-install-permission-0.2.4-test.txt`. Compilación release y lint aprobados.

APK 0.2.4-prueba, código 22, SHA-256 `A7DB8105EBF02E7BFB9C74692F296A0648C1E5A13DD856B14E31503A776A3C03`. Para instalarla desde una versión anterior todavía se toca Instalar actualización manualmente; el automatismo aplica una vez instalada la 0.2.4. Se publica en GitHub sin instalarla en el teléfono. La huella no se modificó: el usuario aclaró que el problema era una descarga que todavía no había instalado.

## Versión anterior: 0.2.3

Revisión de la preparación de recarga y las pantallas de pago: contenido compacto, acción principal fija, alta directa sin titulares, selección automática del único titular y lista aparte si hay varios. Se corrigen los textos que confundían perfiles con tarjetas, uso habitual y confirmación de pago. Formulario con nombre opcional, validación visible y guardado fijo. [Hallazgos y alcance](AUDITORIA-PAGOS-UX.md).

Pasaron siete comprobaciones Android en 360 × 640 dp: cuatro escenarios de interfaz, recorrido STM/Prex interceptado, carga demorada y conservación de la página. Cinco comprobaciones adicionales pasaron en oscuro y cuatro en 411 × 868 dp con texto 2×. En tamaño habitual la pantalla con un titular cabe completa; con texto grande puede desplazarse el contenido, mientras la acción queda visible. También pasaron siete pruebas JVM, compilación release y lint.

APK 0.2.3-prueba, código 21, SHA-256 `FED549641A414A568627795312F9A7D4D5BF2D28BECD5B32A9CD689AFA36CF24`. Se entrega en GitHub para actualizar desde la app. Las comprobaciones usaron datos ficticios; no se instaló esta entrega en el teléfono ni se realizó un pago real.

## Versión anterior: 0.2.2

Al abrir una instancia nueva de la app con acceso guardado, se solicita el desbloqueo al alcanzar el estado activo de Android. La misma función atiende el botón manual. El intento automático queda consumido aunque falle o se cancele, no se repite al cerrar sesión o volver de otra app, y ese estado sobrevive a la recreación de la pantalla. El cifrado y la autorización biométrica de AccessVault no se modificaron.

Pasaron tres pruebas Android: acceso guardado ilegible dispara automáticamente el intento sin credenciales reales, ausencia de acceso conserva el ingreso manual y el aviso sigue flotando sin mover el contenido. También se comprobó que cerrar sesión y recrear la pantalla no repiten el intento, y que una nueva apertura sí lo repite conservando el archivo guardado. La prueba usa datos inválidos a propósito y no representa una lectura de huella física ni un ingreso real. Registro: `outputs/automatic-unlock-0.2.2-test.txt`. Compilación release y lint aprobados.

APK 0.2.2-prueba, código 20, SHA-256 `C8A69C5DFB6CD275A048C588416EB32059B51ECAD7E0444D3C078FD2A87FB6D2`. Se entrega en GitHub para actualizar desde la app. No se instaló en el teléfono durante esta entrega.

## Versión anterior: 0.2.1

Se retiraron el bloqueo persistente, la revisión manual y la recuperación de enlaces de solicitudes anteriores. El contexto de pago existe solo durante la pantalla activa; al salir de Prex se limpia y se consulta el saldo. Regresar de eBROU dispara esa misma consulta. Los marcadores antiguos se eliminan al abrir la app, conservando accesos, perfiles y favoritos. Esto no cancela ni confirma pagos en el proveedor.

Los avisos ahora usan una superposición inferior con cierre automático y manual. La prueba de desbloqueo ficticio comprobó igualdad exacta de la posición del título antes y después, desaparición automática y repetición del aviso. Captura: `outputs/notice-overlay-0.2.1.png`.

Pasaron cinco pruebas Android: aviso flotante, recorrido STM/Prex interceptado, migración de marcadores, perfiles cifrados y transferencia eBROU local interceptada. El recorrido volvió del pago al saldo, abrió una segunda recarga y comprobó una sola carga por acción deliberada pese a repetir el toque. La prueba de transferencia eBROU no representa un regreso desde una sesión bancaria real. Registro: `outputs/flow-notice-0.2.1-test.txt`. También pasaron compilación release, lint y 7 pruebas JVM durante este cambio.

APK 0.2.1-prueba, código 19, SHA-256 `3C42FDB5D308CDD701B00DC3F9A54B3E6759FC41CB2E3EA02E68BCEDAC8268CC`. Se entrega en GitHub para actualizar desde la app. No se instaló en el teléfono ni se hizo un pago real en esta entrega.

## Versión anterior: 0.2.0

La serie 0.2 identifica el rediseño entregado en 0.1.16. Este cambio modifica únicamente la numeración, la documentación y el nombre del APK: versión 0.2.0-prueba, código Android 18. Pasaron nuevamente las 40 pruebas JavaScript, 7 JVM, lint y compilación release. SHA-256: 60A4A17863C9AA45CA579D43C58EBEEE6464607BDFF824E017C6B711FD979007. La revisión visual de 0.1.16 sigue siendo la evidencia de este mismo diseño. La versión instalada en el teléfono sigue siendo 0.1.16; 0.2.0 se entrega para actualizar desde GitHub.

## Versión anterior: 0.1.16

Rediseño nativo de acceso, saldo, importe, configuración y datos del titular. Incluye temas claro/oscuro, Google Sans y Material Symbols. [Decisiones y referencias](DISENO.md).

Pasaron 40 pruebas JavaScript, 7 JVM, lint y compilación release. Cinco comprobaciones de diseño pasaron en tamaño habitual, pantalla de 360 × 640 dp, texto 2× con animaciones desactivadas y orientación horizontal. Las capturas usan datos ficticios. La comprobación del texto se realiza sobre las líneas de glifos; una primera medición confundía el ancho disponible del párrafo con texto recortado y fue corregida.

También pasaron las cuatro pruebas de dispositivo, el recorrido completo interceptado localmente, la conservación de la página y la carga demorada de Prex. La comprobación funcional final usó el gesto real de actualizar y el nuevo selector de importe, y terminó sin repetir una solicitud. La búsqueda de actualizaciones desde el nuevo icono también aprobó. Registros locales: `outputs/design-final-functional-0.1.16.txt` y `outputs/design-*-0.1.16.txt`.

La demostración de movimiento usa los componentes de producción con valores ficticios, sin acceso a STM ni autorización de pagos. No es una medición de rendimiento de todos los dispositivos. La APK está instalada en el OnePlus, versión `0.1.16-prueba`, código 17. SHA-256: `884356F391F71CB379FB93CB96D31B98E6ECDC854649EAD63AE93D2D7E930761`.

La aceptación visual del titular y la autorización/acreditación reales siguen pendientes. Esta entrega no hizo un pago real.

## Versión anterior: 0.1.15

Configuración permite buscar versiones de prueba publicadas en `jmestrallet/boletera-android`, descargar una versión posterior y abrir el instalador de Android. No usa credenciales de GitHub ni consulta la cuenta STM. Verifica tamaño, SHA-256 publicado, paquete, versión y misma firma; no ofrece bajar de versión. Comparte únicamente la APK privada mediante FileProvider y un permiso de lectura temporal.

Pasaron 40 pruebas JavaScript, 7 JVM, lint y compilación. Cuatro pruebas Android comprobaron selección numérica de versiones de prueba, rechazo de borradores/destinos ajenos/digest ausente, controles de paquete/firma/versión, consulta real de GitHub desde Configuración y descarga pública con comprobación de SHA-256. La descarga con hash alterado fue rechazada y eliminada; la versión publicada anterior no se aceptó como actualización. La prueba positiva de identidad usó la APK debug real con metadatos de versión anterior como referencia: no representa una actualización instalada desde GitHub.

Una quinta prueba comprobó el URI privado, el permiso temporal, el rechazo de un archivo fuera del directorio compartido y abrió el instalador real de Android 16. La captura local muestra «Do you want to update this app?»; se salió sin confirmar. Para esa prueba se habilitó la instalación desde esta app únicamente en el emulador. No se modificó ese ajuste en el teléfono.

Evidencia local: `outputs/updates-0.1.15-test.txt`, `outputs/updates-installer-0.1.15-test.txt`, `outputs/update-installer-0.1.15.png`. APK final instalada por cable en el OnePlus y versión comprobada: `0.1.15-prueba`, código 16; SHA-256 `CE4AA79CABCF834371DF7FE6D65B772F42F9F63B555A6A235C27ABB31C7A8E4B`. Todavía no se completó una actualización de extremo a extremo desde GitHub. La 0.1.15 se publicó posteriormente en GitHub y se verificó que el archivo remoto coincidiera con esta APK.

Referencias de Android: [compartir archivos con FileProvider](https://developer.android.com/training/secure-file-sharing/setup-sharing) y [autorización por fuente de instalación](https://android-developers.googleblog.com/2017/08/making-it-safer-to-get-apps-on-android-o.html).

## Candidato anterior: 0.1.14

La pantalla de saldo usa el gesto estándar de arrastrar hacia abajo desde el comienzo para ejecutar la misma consulta de saldo y mínimo. Se retiró el botón inferior. El gesto está deshabilitado en otras pantallas y mientras hay una consulta en curso.

Pasaron 40 pruebas JavaScript, 7 JVM, lint y compilación release. APK instalada y versión comprobada en el OnePlus: `0.1.14-prueba`, código 15. SHA-256: `ED550CC1985D39FE93D98C03F189EDBF587AECFA142BB47DE6E8CA9EAA08528B`. No se realizó un ingreso real ni una prueba del gesto en la cuenta del titular durante este cambio. Candidato local sin publicación remota.

## Candidato anterior: 0.1.13

Se reprodujo con una regresión JavaScript que la ausencia temporal del formulario se clasificaba como página original. Ahora conserva la carga nativa hasta reconocer el siguiente paso. El navegador comienza oculto y vuelve a ocultarse al navegar; las respuestas anteriores no pueden cambiar el estado de la nueva navegación.

Pasaron 40 pruebas JavaScript, 7 JVM, lint y compilación release. En Android, `PrexLoadingTest` montó la pantalla real de la app con una página ficticia magenta que demoró 3,5 segundos en presentar el resumen: seis capturas durante la espera no mostraron píxeles magenta y luego apareció el resumen. También pasó `EmbeddedPrexPaymentTest`, que comprueba la conservación de la página. Ambas pruebas interceptan la red: no iniciaron operaciones reales. Resultado local: `outputs/loading-0.1.13-test.txt`.

APK instalada en el OnePlus y versión comprobada: `0.1.13-prueba`, código 14. SHA-256: `7AB5A9F959FE321DFB4495EDC163923E1A711AD6007693A2C80792A23AF75704`. La corrección del destello todavía no se recorrió contra la pasarela real en el teléfono. El usuario pospuso los pagos reales para continuar el desarrollo. No se publicó una versión remota.

## Candidato anterior: 0.1.12

Se mejoró el panel del CAPTCHA para conservar el componente original en una posición estable y adaptar sus dimensiones a la ventana real de la app. Pasaron 39 pruebas JavaScript, 7 JVM, lint, compilación y la matriz Android del panel con contenido ficticio, más la prueba de retención de la página. La APK está instalada en el OnePlus; el checkbox real se ve completo. [Evidencia y límites](CAPTCHA-RESPONSIVO.md). La verificación humana y el pago completo siguen pendientes.

Actualización física posterior: la misma solicitud alcanzó el paso «Datos Tarjeta» de Sistarbanc, con los pasos de resumen y cliente marcados como completados. Los tres campos de tarjeta estaban vacíos. Al enfocar el número se mostraron sugerencias de tarjetas guardadas en el teclado del teléfono. Esto verifica la disponibilidad de las sugerencias en el formulario real dentro de Boletera; todavía no verifica la selección, el llenado, la huella, la autorización ni la acreditación. No se eligió ninguna tarjeta ni se pulsó Continuar en ese paso. Se dejó la selección abierta para el titular. La captura privada permanece fuera del repositorio.

## Candidato local: 0.1.11

El resumen y los datos del titular se presentan con controles nativos Android. La sesión sigue en el WebView original; el adaptador lee únicamente los pares de texto del resumen y los cinco campos ordinarios del cliente. Las acciones nativas solo avanzan desde los componentes reconocidos de resumen o cliente; no autorizan el pago.

En el OnePlus 6T con Android 11 se reabrió la misma solicitud pendiente y se comprobó el paso de resumen nativo a titular nativo, la lectura de los seis datos del resumen y la apertura/cancelación del editor nativo con cédula y pasaporte. No se creó otra solicitud ni se avanzó a la tarjeta. Se corrigieron dos defectos encontrados físicamente: el resumen usa bloques `b`/`p`, no una tabla; el panel del WebView requiere recorte explícito también en Compose.

La APK final del candidato, SHA-256 `CF6FB2720FBFA247A7F486DC9408E4193BD8293778A243E6C887BD9F43466CDB`, quedó instalada. La inspección visual final confirmó el resumen nativo y la pantalla nativa del titular con el checkbox original completo, centrado y sin franjas de la página alrededor. Se dejó la misma solicitud en ese punto para la verificación humana. Las capturas contienen datos privados y se conservan fuera del repositorio.

Pasaron 37 pruebas JavaScript, 7 JVM, lint y compilación. Las pruebas nuevas cubren pasos ocultos, doble pulsación, controles ambiguos, origen ajeno, ausencia de lectura de tarjeta, geometría del CAPTCHA y edición explícita de datos. El desafío expandido y el envío real de los datos editados no están comprobados físicamente. CAPTCHA resuelto, tarjeta/Google, autorización y acreditación continúan pendientes. Esta versión todavía es un candidato local, no una publicación nueva.

## Versión publicada anterior: 0.1.10

Agrega cédula por defecto y pasaporte en el perfil, y selección del tipo en el control original de Prex. Las pruebas cubren el selector nativo HTML, el selector Material con panel asociado que aparece después y los cambios manuales. Se conservan las limitaciones generales del recorrido de pago.

En el OnePlus 6T con Android 11 se reabrió la misma solicitud de Prex y se avanzó del resumen al formulario del cliente. La app seleccionó «Cédula de Identidad» sin tocar manualmente ese control y completó los cinco campos ordinarios con el perfil guardado antes de incorporar el tipo de documento. Se verificó la pantalla y su jerarquía; las capturas privadas no se publican. El primer intento sobre el formulario oculto no funcionaba: el adaptador ahora espera a que ese paso sea visible. Pasaporte y conservación de cambios manuales se comprobaron con fixtures, no con una operación real de pasaporte. Pasaron 33 pruebas JavaScript, 7 JVM, lint y compilación. No se completó CAPTCHA ni se autorizó un pago.

## Corrección TLS de 0.1.9

Se corrigió la cadena de confianza de Sistarbanc para Android 11. En el OnePlus 6T físico se reprodujeron los rechazos de ambos dominios y se verificó HTTPS y WebView con la configuración corregida, mediante una app de diagnóstico separada sin operación ni cuenta. [Evidencia TLS](TLS-ANDROID11.md). El recorrido de pago completo sigue pendiente.

## Base funcional: 0.1.8

Prex está integrado en una pantalla propia de Boletera que conserva la página original del proveedor. Incluye perfiles cifrados de titular, selección por cuenta, vínculo del perfil a la solicitud y reapertura sin recargar la página retenida. No lee ni guarda datos de tarjeta ni autoriza pagos por código. eBROU conserva Chrome.

Comprobaciones de esta versión:

- 31 pruebas JavaScript aprobadas: 22 del adaptador STM, 4 del candidato de extensión y 5 del adaptador de titular integrado.
- 6 pruebas JVM aprobadas. Lint terminó con 0 errores y 27 advertencias, principalmente recomendaciones de API/estilo y dependencias.
- 14 pruebas Android ejecutadas y aprobadas. El corredor enumera 15 porque incluye el sondeo biométrico, que fue omitido mediante su condición explícita de activación; no se cuenta como aprobado.
- El recorrido Android con contenido ficticio pasó desde ingreso y saldo hasta selección de perfil, nueva solicitud, pantalla de Prex, salida y reapertura. Se comprobó un único ingreso al enlace de pago, los campos ordinarios completados y ausencia de lanzamiento de Chrome para Prex.
- Dos formatos de titular probados; los campos de tarjeta y CAPTCHA permanecen sin lectura ni escritura por el adaptador. Datos distintos preexistentes requieren aplicar explícitamente el perfil elegido.
- Perfiles cifrados recuperados después de recrear su almacenamiento; registros ilegibles no se sobrescriben. Cambiar el perfil vinculado a una solicitud invalida su enlace cifrado y conserva la guardia pendiente.
- La captura del panel se realizó después de que WebView confirmó su dibujo. Una primera captura demasiado temprana aparecía vacía y fue descartada. La evidencia visual corresponde a un formulario ficticio, no a un pago real.
- La APK release usa versionCode 9 y conserva la firma de 0.1.7. No es depurable.

La [prueba pública del detector](PRUEBA-IDENTIDAD-NAVEGADOR.md) comprobó el rechazo del WebView estándar y la ausencia de ese rechazo con la identificación alternativa. Se realizó sin operación ni APIs de pago. **No verifica un formulario con operación, Google en el teléfono, CAPTCHA real, autorización bancaria ni acreditación. El objetivo completo continúa pendiente.**

## Evidencia histórica hasta 0.1.7

## Resultado

La APK release 0.1.7 protege la revisión de pagos hasta completar el ingreso y corrige el encuadre de verificaciones fuera de la vista. Conserva Google y la recuperación de Prex. El inicio real de Prex y eBROU se comprobó con 0.1.5; las comprobaciones nuevas de 0.1.6 usan datos ficticios e interceptan la apertura del navegador. **No se validó una autorización bancaria ni acreditación. No está lista para publicación general.**

| Comprobación | Resultado |
|---|---|
| Adaptador JavaScript: acceso, dinero, deuda, mínimo variable, tarjetas, origen, privacidad, CAPTCHA y límite de pago | 22 pruebas aprobadas; incluye formularios superpuestos, traspaso de identidad, selección de celda, importe mediante el control numérico de STM y medios de pago |
| Modelos JVM: importes exactos, mínimo, política de navegación y origen de respuestas durante transiciones | 6 pruebas aprobadas, incluyendo destinos y formulario de pago |
| Compilación desde clon público limpio de GitHub (`a6724d5`) | APK debug, 3 pruebas JVM, lint y 11 pruebas JS aprobados. Sin `local.properties` ni claves del proyecto; se usaron JDK/SDK instalados y caché de dependencias de la PC |
| Emulador Android 16: arranque nativo, fixture en WebView, ausencia de guardado plano sin biometría, capturas habilitadas y recorrido offline usando StmEngine con login en URL protegida y tabla demorada hasta saldo/mínimo | 10 pruebas locales aprobadas; también preferencias por cuenta, guardia pendiente, traspaso eBROU y recuperación cifrada de Prex |
| Entrada pública real de STM desde WebView | Aprobada en Android 16 tras corregir la cadena TLS incompleta y reconocer la descripción incluida en el botón de identidad. Solo navegación pública; sin enviar documento ni contraseña |
| Login real con credenciales del usuario en la APK | Comprobado en emulador Android 16 con 0.1.5. Las capturas aportadas por el usuario de 0.1.6 muestran saldo, mínimo, confirmación y avance al resumen y formulario del cliente de Prex en su teléfono; no muestran el ingreso completo ni un pago autorizado |
| CAPTCHA real completo dentro del recorte | No validado |
| Guardar y descifrar con biometría | Cifrado, recuperación tras recrear la actividad, cancelación y eliminación aprobados con el sensor simulado del emulador Android 16 y Android Keystore. Huella física pendiente |
| Tarjeta guardada de Google y huella en pago | No validado. Solo hay una prueba local de autocompletado |
| Inicio real Prex y eBROU | Comprobado con la APK release 0.1.5: Prex hasta resumen oficial con el importe elegido y eBROU hasta ingreso oficial del banco. Sin datos bancarios ni autorización |
| Regreso y preferencias reales | Consultar saldo sin reingreso, aviso pendiente persistente, cambio de boletera y medio recordado comprobados |
| Débito y acreditación | No comprobados |

La versión 0.1.0 fallaba con `TLS_REJECTED_3` en Android. El servidor STM enviaba solamente el certificado final y omitía el intermedio. La versión 0.1.1 incluye el certificado público oficial Certum DV TLS G2 R39 como ancla adicional de confianza **solo para el dominio exacto stm.gub.uy dentro de esta aplicación**. No se modifica el almacén de Android. La cadena se verificó contra la raíz oficial; un nombre de servidor incorrecto fue rechazado. Android sigue comprobando TLS y el manejador de errores sigue cancelando, sin `proceed()`.

Fuente del certificado: https://repository.certum.pl/certumdvtlsg2r39ca.pem. SHA-256 del certificado DER: `83C0A5A76844C840DFAF820FFD02ADF6573A26823EF6AF758A3384A0AC044083`.

La ejecución final de 0.1.2 en Android 16 aprobó las seis pruebas (cinco locales y el sondeo público). Los fixtures usan datos ficticios; no acreditan un login completo ni una recarga real. El test de tabla compacta falló con cero boleteras antes de la corrección y luego aprobó con dos; el recorrido Android llegó al saldo y mínimo sin mostrar páginas completas. La corrección de respuestas de un documento anterior está probada con datos controlados, pero no se demostró que sea la única causa del bloqueo informado en el teléfono.

La ejecución de 0.1.3 también aprobó las seis pruebas Android, incluyendo un recorrido ampliado: URL de boleteras con formulario de login, ingreso con contraseña sintética, demora de la tabla y selección de boletera hasta saldo y mínimo. Dos regresiones JS fallaron antes de corregir el reconocimiento por URL y luego aprobaron. Ahora también se exige que el documento termine de cargar antes de clasificarlo. Esto no sustituye confirmar el resultado en el teléfono del usuario. La referencia visible solo incluye etapas y conteos; las capturas recibidas no se publican.

La prueba real de 0.1.4 encontró problemas que los fixtures anteriores no reproducían: formularios de documento y contraseña superpuestos, el traspaso por `ih.montevideo.gub.uy`, selección PrimeFaces que requiere un clic en una celda y el botón de recarga sin etiqueta propia. Corregidos esos pasos, la APK release llegó automáticamente a la consulta real de saldo y mínimo. No se publican datos de la cuenta ni capturas del recorrido autenticado.

Al elegir el importe, apareció además un rechazo porque el campo visible no actualizaba el valor interno de PrimeFaces. Se corrigió usando el método del control numérico observado. Ese adaptador corregido se ejecutó contra la sesión real en una compilación de desarrollo y llegó a `recarga2.xhtml` sin errores, sin seleccionar proveedor ni solicitar pago. La APK final incorpora ese mismo código y pasó 19 pruebas JS, 4 JVM y lint; el recorrido Android de seis pruebas aprobó antes de esta última corrección exclusiva del importe. No se repitió un ingreso completo con el binario final tras esa corrección. Durante el ingreso real no apareció un desafío CAPTCHA; no se lo considera validado.
## Validación final de 0.1.5

Con la APK release final se abrió el ingreso real de eBROU y se regresó a la consulta de saldo sin iniciar sesión otra vez. También se verificaron el diálogo de revisión, la liberación manual tras salir sin autorizar y el cambio de boletera. Prex había llegado a su resumen oficial con la misma implementación de pago; las correcciones posteriores afectaron el regreso y el diálogo. La elección de boletera y Prex sobrevivió a una actualización e ingreso nuevo. Ningún recorrido ingresó credenciales bancarias, autorizó un débito o comprobó acreditación. No se publican datos de la cuenta ni capturas autenticadas.

Después de la última corrección del regreso se ejecutaron las ocho pruebas Android: todas aprobadas, incluyendo el sondeo público. El build final aprobó 21 pruebas JS, 6 JVM y lint. El diálogo se inspeccionó visualmente en el emulador.

## Validación de 0.1.6

Se probó la persistencia cifrada del enlace tras recrear las preferencias, su separación por cuenta y operación, el rechazo de datos modificados, su eliminación y la conservación del aviso pendiente. Un recorrido completo con formularios ficticios verificó que el botón no se habilita antes del ingreso, abrió el diálogo nativo y capturó el enlace exacto enviado a Chrome, sin solicitudes adicionales al motor STM. La apertura de Chrome se interceptó en las pruebas: no hubo conexión al banco ni se comprobó cuánto dura un enlace real.

La suite final comprende 21 pruebas JavaScript, 6 JVM, lint y 10 pruebas Android (9 locales y el sondeo público). No se agregó un servicio de autocompletado ni se cambiaron los ajustes de Google.

## Validación de 0.1.7

Se reprodujo que un ingreso sin verificar podía eliminar el aviso pendiente; la regresión falló antes del cambio y aprobó después. Los avisos no se cargan en la interfaz ni pueden reconocerse hasta leer las boleteras de la sesión autenticada.

Otra regresión Android reprodujo un iframe fuera del área visible del WebView. Se agregó una orden que desplaza la página original antes del recorte, sin leer ni modificar el contenido del desafío. Con un iframe completamente ficticio, servido localmente bajo URLs interceptadas, se verificaron el panel chico, el expandido y los toques a través del contenedor nativo. Se esperó la entrega asíncrona del evento; la comprobación de posición admite un píxel CSS de redondeo. Se inspeccionó la captura del panel expandido: texto y controles sin recortes.

Resultado final: 22 pruebas JavaScript, 6 JVM, lint y 11 Android aprobadas. No se contactó Google para resolver un CAPTCHA ni se validó uno real; tampoco se autorizó un pago. El único sondeo de red de la suite es la entrada pública de STM.

## Prueba adicional de biometría sobre 0.1.7

`BiometricVaultTest.encryptedRoundTripCancellationAndForget` aprobó en Android 16 en 53,624 segundos. Usa `AccessVault` sin sustituir el diálogo biométrico, el cifrado ni Android Keystore. Se enroló una huella simulada en los ajustes del emulador y se autentificaron dos operaciones con el sensor del emulador: guardar y descifrar. Entre ambas se recreó la actividad. El test comprobó los datos ficticios recuperados y que las preferencias persistidas contienen únicamente IV y texto cifrado. Esto no prueba un reinicio completo del proceso o del teléfono.

En un tercer diálogo se pulsó **Cancelar**: el callback no devolvió credenciales y conservó el acceso guardado. Después se eliminó el acceso y un intento de descifrado devolvió datos nulos. El test no llama a STM ni usa credenciales del usuario. Se quitó el PIN temporal al finalizar y se verificó que el emulador quedó sin huellas enroladas. No hubo cambios al código de la app ni a la APK publicada.

Es una prueba optativa que necesita interacción con un emulador preparado; no se presenta como un test automático aprobado por omisión. Con los APK debug y androidTest instalados, ejecutar:

```powershell
adb -s emulator-5554 shell am instrument -w -r -e class uy.boletera.prueba.BiometricVaultTest -e biometricProbe true uy.boletera.prueba.test/androidx.test.runner.AndroidJUnitRunner
```

Esperar el diálogo y la fase `SAVE_TOUCH`, simular la huella enrolada; repetir en `UNLOCK_TOUCH`; en `CANCEL_PROMPT`, pulsar **Cancelar**. El resultado debe terminar en `OK (1 test)` y fase `COMPLETE`. Sin el argumento `biometricProbe`, se omite. Preparación y comandos del sensor: [documentación oficial del emulador Android](https://developer.android.com/studio/run/emulator-console).

Siguen pendientes la biometría física, el CAPTCHA real, el autocompletado de Google en el formulario bancario, la recuperación de un enlace real de Prex y la autorización/acreditación del pago. Las capturas recibidas de 0.1.6 confirman avance hasta el formulario de cliente; no prueban esas etapas restantes y no se publican.

## Evidencia local

- JVM: `app/build/test-results/testDebugUnitTest/`.
- Emulador: `app/build/outputs/androidTest-results/connected/debug/` y `app/build/reports/androidTests/connected/debug/`.
- Lint: `app/build/reports/lint-results-debug.html`.
- Captura de pantalla nativa sin datos personales: `outputs/welcome-test.png`.
- Entrega: `outputs/boletera-prueba-0.1.7.apk` (7918654 bytes), SHA-256 `d8114f93387e34882b78e131d5a238c52a4a058a6bf25ad1911874c8eadc306b`. Firma verificada, igual que las versiones anteriores.

## Criterio para continuar

1. Instalar la APK de prueba en un Android físico y comprobar si la conexión segura a STM funciona.
2. Si funciona, validar ingreso, CAPTCHA y saldo/mínimo reales; ante una pantalla no reconocida, reparar el adaptador sin ampliar la excepción a páginas completas.
3. Probar guardado con huella, reinicio de app, cancelación biométrica y eliminación del acceso. No reemplazar un fallo biométrico por almacenamiento plano.
4. Verificar la recuperación de una solicitud Prex y comprobar en el teléfono la autorización y acreditación de una recarga. No representar el monto preparado, el regreso o un cambio de saldo como confirmación del pago.
