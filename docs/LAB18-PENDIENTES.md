# Lab 18 · todo lo definido desde el 16/10 18:19

## ESTADO ACTUAL (18/10/2026) — leer primero
- **Publicadas:** Lab 17 (code 77), Lab 18 (78), Lab 19 (79), Lab 20 (80, b1a1a49), Lab 21 (81, eec3185, 17/10 21:50), Lab 22 (82, 18/10, revisión por módulos + optimización), Lab 23 (83, 18/10): Google completa la tarjeta entera (la lista se pide cuando los 3 campos terminaron de aparecer; antes, a los 0,12 s, Google veía vencimiento y código transparentes y llenaba solo el número — reporte 10) + red de pruebas `verificar.ps1` (correrla antes de cada APK).
- **18/10 — el dueño dice que con Lab 21 «cada vez la app anda peor, cada vez andan menos cosas».** Respondió que falla todo (pago/tarjeta, Express, entrada/huella/saldo, otras). Se hizo la revisión por módulos (7 agentes Sonnet + coordinador) en el worktree `claude/mystifying-bohr-7a30e8` (basado en lab e568329). **Publicado como Lab 22** (ver «Revisión por módulos 18/10» abajo). Falta que el dueño lo pruebe en uso real con su tarjeta (pago, Express, Google).

- **Lab 24 (84, 19/10) = la última:** «Ver la página» en errores (STM/gub.uy usable; Prex solo mirar), gub.uy a la vista si se traba tras la contraseña o la vuelve a pedir (no se reenvía), forma de página más completa sin datos (solo Lab), «no son válidos» = contraseña incorrecta. Disparador: amigo del dueño trabado en gub.uy (reporte 12, Xiaomi Android 16): la forma mostraba la página de gub.uy con «Continuar» y «Conocé más sobre el doble factor»; no se sabe si pedía un código. Con Lab 24, si le vuelve a pasar, ve la página y el reporte dice qué era.
- **Incluido en Lab 24:** 18/10 la entrada falló porque el servidor de ingreso de la Intendencia (`ih.montevideo.gub.uy/samlsso`, por donde STM manda a entrar con gub.uy) cortaba todas las conexiones, también desde un navegador común: no era Boletera. La app decía «Revisá tu conexión y el estado del pago»; ahora ese servidor cuenta como STM y sale «STM no responde · La página de STM está caída o muy lenta.» (el dueño pidió sacar «No es tu conexión»).

## Revisión por módulos (18/10) — aplicada en el código, sin publicar
Pista del registro real (issue 9, Lab 21): en el paso tarjeta «Continuar» tocado 11 veces sin efecto. El botón sí recibía el toque (sale «toque · Continuar»); sin segundo plano en ese paso. Lo más probable: algún dato incompleto → solo marcaba el campo en rojo y el teclado tapaba el botón, sin explicar (pasaba igual en Lab 20). Ahora explica, vuelve a abrir la lista de Google y el registro dice qué faltaba («tarjeta · …», sin valores). Si vuelve a pasar, el próximo reporte lo dice.
- **Inicio (CONFIRMADOS):** el refresco al volver de Lab 21 nunca corría (era un `else` del sondeo) → «Recarga en camino» no se actualizaba; tras un corte de internet o «El pago no llegó a hacerse» quedaba el mínimo viejo y «Recargar»/rayo no hacían nada para siempre; si STM no aceptaba leer el mínimo, «Recargar» quedaba gris muerto; el rayo no respondía al primer «mantener» tras un Express que no arrancó; la luz de Express salía aunque no arrancara; la hoja de montos podía reaparecer sola.
- **Pago:** «Continuar» que no puede enviar ahora dice por qué; aviso persistente si Prex no toma la tarjeta; Express ya no se apaga solo tras resolver un captcha largo (reloj de 20 s); «recargando al volver» no parpadea 19 ms; reset() limpia más estado entre pagos.
- **Fin del pago:** «Procesando tu pago» en recibo / «Recarga exitosa» sin botón quedaba sin salida → «Volver al inicio» a los 15 s (35 s en confirmación); «Recarga no confirmada» de STM ahora sí cierra el pago como rechazado; sin internet tras pagar se ve «Recarga en camino» si Prex confirmó; Atrás/«Cancelar» en «Conectando con STM» tras pagar ya no cierra la sesión; registro de pagos ilegible ya no bloquea todos los pagos.
- **Entrada:** si Android mataba el proceso, al volver no entraba solo ni pedía huella; Atrás durante el ingreso mostraba «No pudimos entrar» en rojo; llave del celular perdida → huella fallaba en cada apertura sin explicar (ahora dice «Cambiaron las huellas…»); reintento tras abrir sin internet reusa la sesión; «Volver a intentar» sin huella abre el panel; «Olvidar este celular» borra la huella primero.
- **Actualizaciones:** la huella esperaba 1,5 s de más tras «Ahora no»; la huella podía pedirse justo al volver el instalador (= «No se desbloqueó el acceso» al tocar Instalar); la hoja «Lab X está lista» podía salir encima del pago; cerrar el aviso durante la descarga salteaba la versión; un error de búsqueda se pintaba como descarga fallida; el registro ahora anota las actualizaciones.
- **Otros:** avisos tapados por Configuración (ahora se ven) y por el formulario de datos (ahora lo dice el formulario); apodo viejo al editar el nombre; editar una persona la mandaba al final; demo gastaba la pista real del rayo; Atrás en «Conectando» de la demo; botones que cortaban texto con letra grande; **si Android cerraba las páginas por memoria, la app entera se cerraba** → ahora se reabre ordenada.
- **Textos nuevos (elegidos por el dueño 18/10, con el criterio de docs/maquetas/lab20/textos.html):** número vacío «Te faltó completar el número de la tarjeta.» · vencimiento vacío «Falta el vencimiento.» · Prex no recibió la tarjeta «No pudimos mandarla a Prex. Probá de nuevo en un momento.» · STM no tomó un paso en el inicio «STM no respondió como esperábamos. No se hizo nada; deslizá hacia abajo para probar de nuevo.» · búsqueda de versión sin internet «No pudimos buscar actualizaciones. Cuando vuelva la conexión, probá de nuevo.» (otro error: «… Probá de nuevo en un rato.») · Borrar tus datos: siempre «Se borran de este celular. Los podés volver a cargar cuando quieras.» (el de Carga Express no era cierto en todos los casos).
- **Huella (dueño 18/10):** se pide solo cuando hace falta. Con la sesión de STM viva la app entra directo (reusa la sesión de STM, como el navegador; la contraseña nunca se descifra sin huella). NO agregar huella en cada apertura.
- **Revisores (después de los arreglos):** «Recarga no confirmada» de STM como rechazo → REVERTIDO (borraba el aviso que protege del doble pago). «Volver al inicio» en «Procesando» pasa a link discreto. Hoja «Lab X está lista» no tapa otras hojas. Atrás en «Conectando» solo se bloquea mientras STM trabaja. Cambios de AccessVault y del orden de personas solo en Lab. Registro de pagos se borra solo si es ilegible de verdad.
- **Pruebas 18/10:** npm 83/83 · Lab 37/37 en el OnePlus (el celular se apaga solo si no está enchufado: despertarlo cada 15 s durante las pruebas) · Alpha (ExpressMode, ExpressFeedback, CardFlowRegression) 5/5. OnePlus quedó con debug Lab y Alpha: reinstalar los release al terminar.

## Optimización (revisión 18/10, 3 agentes)
- **Aplicado (no cambia nada visible):** la capa del vuelo de la tarjeta redibujaba toda la pantalla 60 veces por segundo siempre (ahora solo mientras vuela); la cuenta regresiva de Express reconstruía toda «Confirmá tu pago» cada cuadro; la apertura reconstruía toda la app cada cuadro (velo); «Último saldo» descifraba con Keystore en cada redibujo; efectos visuales consultaba al sistema en cada redibujo; el reporte retenía la pantalla cerrada en memoria.
- **Para después (decisión del dueño, tocan motor/pago):** el sondeo de STM corre cada 1,1 s en el inicio y reinyecta ~18 KB de JS por vuelta (inyectar una vez / espaciar a 3-4 s en reposo); WebView de Prex creado aunque no se pague; búsqueda de versiones baja 100 releases (medir); R8 y perfil de arranque (tanda propia); rayo de Express recompone por cuadro al mantener (riesgo de cambiar el gesto); OldCardVault.erase en cada apertura; LabLog.dropOld lee todo el archivo.
- **No tocar:** cifrado/commit sincrónico del registro de pagos (es la garantía contra doble pago), sondeo de Prex, máscara del captcha, pausar WebViews, clearCache al cerrar sesión.
- **Calidad del código (veredicto):** seguro en lo importante, pero creció mal: estado en banderas sueltas y etapas como texto, ramas `IsLab` dentro del código compartido (MainActivity 78, StmEngine 39), sin pruebas automáticas del motor ni CI, y versiones Lab que mezclan arreglos con funciones nuevas (15 Labs en 2 días). Plan por pasos: 1) `verificar.ps1` (npm + unit tests) antes de cada APK; 2) todos los tiempos en un solo lugar y una sola definición de «app al frente»; 3) pruebas JVM de «registro de pagos + saldo» y de «volver a la app refresca»; 4) etapas como enum; 5) una Lab = una cosa; 6) un único reset del viaje de pago; 7) partir App() más adelante.
- **No hecho (necesita tarjeta real):** reintento de tarjeta rechazada sin volver al monto. Riesgo conocido sin tocar: si gub.uy rechaza la contraseña con un texto que no reconocemos, se reenvía hasta 3 min.
- **Lab 21 publicada** con toda la revisión. Pendiente para después: reintento de tarjeta rechazada sin volver al monto (sin tarjeta real no se puede probar) y cédula de 7 dígitos en la app no-Lab. Confirmar con el dueño en uso real: Google completa la tarjeta entera tras su huella; la espera ya no salta.
- **Para la próxima Lab (sin publicar, en lab):** ícono corrido 2 a la derecha (opción A del dueño 18/10, en las 3 apps); aviso cuando el pago no arranca = «El pago no llegó a hacerse, probá nuevamente.» (texto final del dueño, 18/10). Lab 21 salió con «No pudimos preparar el pago. Probá de nuevo.». Falta su OK a la hoja «Un pago quedó sin respuesta».
- **Hoja «Un pago quedó sin respuesta»** (aprobada): solo al Recargar/Express si hay pago de esa boletera en «authorizing/pending» de menos de 30 min; «Recargar igual» / «Esperar».
- **Flujo de siempre para publicar:** maqueta/capturas reales → implementar → 2-3 agentes Sonnet → pruebas (npm 83, Lab 35 y Alpha 20 en el OnePlus) → `build.ps1 -Channel lab` → probar APK en el OnePlus → commit + push lab + `gh release create v0.2.40-lab.N --prerelease` (notas en `.tools/release-0.2.40-lab.N.md`, subir versionCode/nombre en app/build.gradle.kts y build.ps1, CHANGELOG, LabWhatsNewItems).
- **OnePlus de pruebas (libre):** por USB se cae; usar Wi-Fi `adb connect 192.168.1.13:5555`. Despertar antes de probar (`input keyevent KEYCODE_WAKEUP`), cerrar diálogos (`am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS`). En ese teléfono las pruebas con animación de entrada necesitan `compose.mainClock.advanceTimeBy(1500)` antes de mirar. Ayudante para tocar por texto: scratchpad `tap.py`.
- **Memoria de la PC:** al terminar, cerrar todo lo propio (`gradle --stop`, procesos java gradle/kotlin, `adb kill-server` si no corre scrcpy, Edge headless, emulador). El dueño lo pide para liberar RAM.
- **Tono de textos:** explicar, «por ahora», no órdenes secas, nunca más largo que antes (lista aprobada en docs/maquetas/lab20/textos.html). Prex: «Por ahora…» en primera vez, monto y tarjeta.

## Revisión de toda la app (17/10 noche) — APLICADA en Lab 21 salvo lo indicado arriba
**Agente Sonnet · pago y Express:**
1. Sin salida después de «Pagar» si Prex muestra una página que la app no reconoce («original»): sin flecha ni «Volver»; el «Prex está tardando» queda debajo. Arreglo: «Volver al inicio» cuando ya se pagó y la página es «original» (o Atrás a los 25 s), y subir la rama `slowStep && paid` arriba de `original` (LabPayment ~333-359).
2. Un pago que quedó «autorizando/pendiente» no deja rastro en el inicio y se puede pagar de nuevo enseguida (sobre todo con Express). Propuesta: una hoja de confirmación al recargar si hay uno de menos de 30 min para esa tarjeta, y sumar «authorizing» a la promoción por saldo (StmEngine ~1045-1083). Ojo con la regla «nada de pagos dudosos»: consultar al dueño la forma.
3. Errores de STM antes de abrir Prex dicen «El pago se interrumpió» (como si se hubiera cobrado): `blockedScreen` no pasa `charged=false` (MainActivity ~411, AppErrorScreen 35/100).
4. Tarjeta rechazada sin reintento: un código mal escrito obliga a empezar todo; permitir «Continuar» de nuevo al editar (LabPayment 472, NativeCardForm 87).
5. Después de un rechazo, al volver, dice 1-3 s «Procesando tu pago»: mostrar «Volviendo al inicio» (LabStatus 100).
6. Los relojes de espera siguen corriendo con la app en segundo plano y pueden pasar a la página de Prex y frenar Express (LabPayment 278-315).
7. «Recargar» no avisa sin internet hasta muy tarde: chequear al tocar Recargar (MainActivity onCharge).
8. «Recarga en camino» no se actualiza al volver a la app y se borra a los 30 min aunque STM no la haya sumado.
9. Estados que quedan colgados tras vencer la sesión (LabRetry.reopenAmount, rechargeShortcut); reset() deja staleAfterResume/payerWhy.
10. Detalles: «Usar Tus datos» (mayúscula), pista de Express «con Prex · •••• 0849» engañosa ahora que elige Google, aviso repetido «El pago fue rechazado» en el inicio, CVV borrado si la verificación aparece justo al enviar.
- **Regla ON_STOP de la tarjeta (Lab 21):** el agente propone una mejor: al salir de pantalla borrar siempre el código de seguridad, NO cancelar a Google, y borrar número y vencimiento si la app no vuelve en 20 s (contados desde la salida, no desde que se abrió la lista). Así no quedan datos de tarjeta guardados si salís de verdad.

**Codex (job task-mvczobm7-r2ms97, terminado):**
- Mi explicación del salto («se arma la WebView de Prex») era INCORRECTA: la WebView ya existe desde que abre la app.
- Página original de Prex sin bloqueo de capturas (SecureScreen solo en el paso tarjeta) → CONFIRMADO en código.
- Si se paga tocando el botón dentro de la página original y se corta la conexión, puede decir «No se cobró nada» sin saberlo (plausible).
- La contraseña se guarda antes de verificarla: un error de tipeo pisa la buena y la huella reintenta la mala (riesgo de bloqueo gub.uy).
- Entrar sin internet con «guardar» apagado: «Entrando…» para siempre.
- «Recarga no confirmada» sin botón habilitado puede quedar sin salida.
- Aviso de actualización que llega tarde puede salir encima de la huella.
- El reporte copiado/compartido no lleva el registro de uso (solo el envío directo).
- Regla de 60 s de Lab 21 guarda la tarjeta aunque bloquees el celular → hay que achicarla.

**Agente Sonnet · entrada/inicio/Configuración (terminado):**
- **Pista fuerte del salto de 10 px (CONFIRMADO en código):** `LabPaymentWait` está dos veces (MainActivity:460 y LabPayment:410). Al pasar de una a otra, el texto de abajo vuelve a entrar subiendo exactamente 10 dp (labEnter dy=10). Arreglo: no repetir la entrada si es continuación.
- Atrás durante «Cambiando de boletera» cierra la sesión (CONFIRMADO: cae en engine.cancel()).
- «Último saldo» de otra cuenta mientras conecta tras cerrar sesión y entrar con otro usuario.
- Errores de actualización invisibles en Configuración; «GitHub limitó…» se muestra como otro error.
- Sin internet al abrir con sesión guardada → pantalla de error en vez de «Sin internet».
- Menores: «Enviar reporte» en contraseña incorrecta; aviso de primera vez no vuelve tras «olvidar celular» hasta reiniciar; hojas abiertas tras sesión vencida; pantalla vieja «paymentBoundary» dice «titular» en Lab; botones sin rol accesible; botón de 64 dp corta textos con letra grande.

## Estado: Lab 18 publicada (17/10/2026)
Todo lo de los puntos 1, 2, 4 y 5 está en Lab 18. Pruebas: lógica de pago 82, pantallas de Lab 35 y Alpha 20 (en el OnePlus), internas y lint; dos agentes Sonnet revisaron y sus hallazgos se arreglaron (registro sin nombres, «Cancelar» después de pagar, otra tarjeta en Prex avisa y no paga sola, cuenta regresiva que espera al aviso de Google, versiones no-Lab sin cambios).

Queda para después (punto 3, pendientes de agentes no hechos): entrada (spinner sin internet, contraseña guardada antes de verificar, etc.), accesibilidad (contraste, alturas fijas), Novedades fijas en el código, «Copiar» sin el registro. Por probar con plata real: que Google ofrezca guardar una tarjeta escrita y qué completa al elegir una.

## Lab 19 (pedidos después de publicar Lab 18)
- Al abrir, la búsqueda de versión nueva va antes de la huella (espera hasta 2 s): con aviso, la huella se pide recién al tocar «Ahora no»; con «Actualizar», no se pide.
- «Datos para Prex»: cambiar entre Cédula y Pasaporte ya no borra el número; cada uno guarda el suyo mientras el formulario está abierto.
- Tarjeta: si Google completa los tres datos, sigue sola a «Confirmá tu pago» también en la recarga común (ahí se ve la tarjeta); si falta el código, espera en el código.

## Para Lab 21 (escrito sin compilar ni probar; el dueño pidió no reabrir nada para liberar RAM)
- Autocompletado a medias (solo el número, la primera vez, cuando Google pidió su huella): la pantalla de huella de Google
  hace que Android avise «salió de pantalla» y la app borraba los campos y cancelaba el pedido de Google. Ahora, si eso pasa
  dentro del minuto de abrir la lista de Google, no se borra ni se cancela (NativeCardForm, autofillAskedAt).
- Salto de ~10 px de la tarjeta al pasar de «Preparando tu pago» a la pantalla de pago: un freno del celular al crear la página
  de Prex rompía la continuidad (tarjeta que reinicia su animación / no se desliza). Márgenes 0,6 s → 1,5 s y 450 ms → 1,5 s
  (LabCardHero, LabMotion). Causa probable, no confirmada: verificar en el OnePlus.

## Lab 20 (publicada 17/10)
- Prex se nombra una sola vez, donde se elige la tarjeta: «Tu tarjeta / Elegí tu tarjeta Prex.» (en Express: «Solo falta la tarjeta / Elegí tu tarjeta Prex.»). Se saca «Se paga con tu tarjeta Prex» del panel (era la fila «Pagás con…» que se había decidido sacar). Espera: «Preparando tu pago · Un momento», pie «Conexión segura»; Express «Cargando el mínimo a tu STM». No se sabe con certeza si la opción «Prex» de STM acepta otras tarjetas; se asume que no.

## 0 · Ya publicado en Lab 17 (16/10 19:13)
Pedido y arreglado entre 18:19 y 19:12; ya está en el celular si se actualizó:
- Cerrar sesión sin «No pudimos entrar» ni «Enviar reporte».
- La tarjeta ya no vuela por encima de Configuración.
- «Borrado ✓» / «Copiado ✓» vuelven solos.
- Express: el desplegable del tipo de documento se reintenta (titular vacío 0/5); plan B que muestra la página de Prex si algo queda quieto antes de pagar; foto de la página en el registro (sin valores); prueba de punta a punta.
- Revisión de 2 agentes: sin doble «Confirmar», nombres fuera del registro, aviso de actualización con errores y reintento, crítica descargada vuelve a pedirse, sin huella tras cancelar Express, reporte parcial avisado, clave de reportes solo en Lab, contraseña mal escrita sin «Enviar reporte».

## 1 · Arreglos ya hechos en el código, sin publicar (revisión de los 5 agentes, 17/10)
- Elegir otro titular en el panel de montos no hacía nada → ya cambia.
- Un rechazo de la tarjeta guardada la dejaba inutilizable hasta cerrar la app → se reinicia en cada pago.
- **Bloqueo de pagos:** una recarga que quedaba «a medio camino» trababa todos los «Pagar» siguientes → la nueva reemplaza a la vieja; la nota vieja se borra a los 30 min o cuando el saldo muestra la recarga.
- Atrás / «Cancelar» después de «Pagar»: ya no abandonan el pago; si el captcha posterior no se resuelve, a los 25 s hay salida.
- «Volver al inicio» / «Probar de nuevo» en los resultados siempre funcionan.
- Error antes de pagar dice «No se cobró nada»; después de pagar queda el texto de siempre («Mirá tu saldo en el inicio…»), sin hablar de pagos dudosos.
- «Cancelar» mientras se prepara la recarga ahora cancela.
- Sin internet: Recargar, Express y cambiar boletera avisan antes de empezar; salir del pago vuelve al último saldo.
- El tiempo con la app en segundo plano ya no cuenta como «STM tarda».
- «Recarga hecha»: Atrás = «Listo», y no se tocan los botones de atrás.
- Express que no arranca explica por qué con palabras («STM todavía está leyendo tu saldo…»).
- Falta: correr todas las pruebas (npm, Lab 32, Alpha 20) antes del APK.

## 2 · Rediseño del pago (maqueta: docs/maquetas/lab18/)
Definido por el dueño (orden en que se decidió):
1. Recargar → «¿Cuánto cargás?» **sin** la fila chica «Pagás con Prex · Titular»; botón «Continuar». (16/10 20:00)
2. Se va la palabra «Titular» de la app. (16/10 22:23)
3. **Boletera no guarda tarjetas. Siempre Google.** «No quiero cosas raras, no quiero responsabilidades» (17/10 00:56):
   - al llegar a «Tu tarjeta», se abre sola la lista de Google; se oculta tocando afuera o escribiendo; a mano siempre se puede (17/10 01:38);
   - tarjeta escrita a mano → Google ofrece «Guardar» (hoy la app lo cancelaba: cambiar cancel() por commit());
   - se van «Guardar para Carga Express», «Usar una tarjeta guardada» y «Usar tu tarjeta ···· 0849»; la tarjeta guardada hoy se borra al actualizar y se avisa en Novedades;
   - lo que completa Google se ve (últimos 4 y vencimiento) antes de seguir; si Google no pone el código, se escribe.
4. **«¿De quién es la tarjeta?»** solo con 2 o más personas guardadas; «Mía» primero. (17/10 01:12)
5. **Tus datos marcados una vez** (17/10 01:21, «pensalo bien»):
   - al cargar datos, arriba «¿Son tus datos?» (viene en «Sí, son míos») / «De otra persona» (+ nombre para guardar);
   - con datos tuyos ya marcados, la pregunta no aparece más; nunca dos «Mía»;
   - si se borran tus datos, vuelve la pregunta; en Configuración cada persona tiene «Son mis datos» (marca uno, desmarca el otro);
   - Express usa tus datos; si no hay, los únicos; si hay varios y ninguno tuyo, pregunta una vez «¿Cuáles son tus datos?»;
   - migración: 1 titular guardado → queda marcado tuyo; varios → se pregunta en la primera recarga.
6. Textos: «Preparando tu pago» (en vez de «Abriendo Prex»), «Código de seguridad» (en vez de «Código»), «Pagar $ 260 ahora». (17/10 00:25)
7. El violeta de Prex «no me parecía mal» → se mantiene donde se muestre el medio de pago. (17/10 00:07)
8. **Carga Express:** mantener el rayo → todo solo hasta la tarjeta → un toque en Google (+ código si hace falta) → **cuenta regresiva** «Cargando en 4… 3… 2… 1» con monto, STM y tarjeta; si no tocás nada paga; se puede cancelar (17/10 05:18). El círculo con «se paga solo en 4 segundos» no gustó (05:20); falta elegir entre la opción 1 (botón que se llena) y la 2 (número en la tarjeta).
9. Las pantallas que ya existen se mantienen como están y solo cambia lo hablado (17/10 05:25): «Confirmá tu pago» con la tarjeta y el monto arriba, «Conexión segura con Prex», «Boletera no guarda tu tarjeta», Cédula/Pasaporte, pantallas de error con la tarjeta y la cruz roja. En «Prex rechazó el pago» el texto pasa a «Revisá el saldo o los datos de la tarjeta».
10. Configuración: la de hoy + sección «Datos para Prex»; se va «Carga Express · Tarjeta guardada».

Descartado en el camino (no implementar): pantalla «¿Con qué pagás?» con medios guardados, guardar tarjetas con huella, «Prex principal» / «último usado», opciones «Pronto», guardar el CVV en Boletera.

Antes de implementar, probar en el emulador: que Google ofrezca guardar una tarjeta escrita; qué completa Google al tocar una (¿código?); que FLAG_SECURE no le corte nada a Google.

## 3 · Pendientes de la revisión de los 5 agentes (sin hacer todavía)
- **Entrada:** spinner infinito al entrar sin internet; la contraseña se guarda antes de verificarla (un error de tipeo pisa la buena); contraseña vieja reintentada hasta bloquear; falta de la llave del celular; orden de «Olvidar este celular»; elegir sola la única boletera; borrar la contraseña al cerrar el panel; Atrás durante el ingreso muestra error rojo; texto de «cambiar contraseña».
- **Configuración / actualizaciones / reporte / demo:** error de descarga que no se limpia al buscar de nuevo; la fila Versión esconde errores; «último intento» guardado antes de lograrlo; la demo usa la tarjeta guardada real; el texto del reporte fuera de protección y sin enmascarar; «Copiar»/compartir sin el registro; Novedades fijas en el código; build.ps1 (nombre del apk y chequeo de la clave).
- **Inicio:** saldo que se sale con letra grande; chip cortado; el rayo desaparece al refrescar; renombrar no se actualiza; texto de «Agregar titular».
- **Textos:** contradicción «Boletera no guarda tu tarjeta» (se resuelve sola con el punto 2), motivos crudos de error, «Olvidar este celular» sin confirmación clara, palabras mezcladas.
- **Accesibilidad:** contraste del texto tenue, alturas fijas, superposiciones sin descripción, saldo leído número por número.

## 4 · Otros pedidos para esta versión
- «Versión» pasa a la 2.ª sección de Configuración, abajo de tu cuenta.

## 5 · Decisiones del dueño (17/10)
1. Cuenta regresiva de Express: primero se eligió el botón que se llena; al verlo grabado no gustó y quedó **el círculo 4-3-2-1** con «Cargando en N…», «Pagar ahora» y «Cancelar».
2. Clave de reportes: **se deja como está** (acepta el riesgo; los reportes no traen tarjeta, cédula ni contraseñas).
3. **«Recarga en camino» sí:** solo cuando Prex confirmó el pago y STM todavía no muestra el saldo nuevo; debajo del saldo, «Recarga de $ 260 en camino · STM la suma en unos minutos»; desaparece cuando el saldo la muestra. Es un pago confirmado, no uno dudoso.

Ya no queda nada por definir. Antes de implementar, las tres pruebas técnicas del punto 2 (Google en el emulador).
