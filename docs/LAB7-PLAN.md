# Lab 7 · plan por objetivos

Rediseño completo de Boletera Lab, aprobado en maquetas el 15/10/2026. Se publica **una sola vez** al final (Lab no saca versiones intermedias). Cada objetivo se da por cumplido solo cuando está implementado, probado en el emulador con medidas de Xiaomi 15 (1200×2670, densidad 480, letra 115 %), comparado con su maqueta y con la auditoría de espacios pasada.

Referencias: maquetas en `outputs/home-2026/` (reglas comunes en `reglas.html`), decisiones de flujo en la memoria `decisiones-flujo-lab7`, relevamiento del sitio de STM en `outputs/stm-sitio/2026-10-15/NOTAS.md`.

## Objetivo 1 · Entrada, errores y piezas comunes ✅
- [x] Reglas comunes en código: tarjeta de 360 y 132, títulos de 34/26/24, botón de 64/60, links azul, gris y dentro del texto (`LabDesign.kt`, `LabScreens.kt`).
- [x] Círculo: tilde para «hecho», «+» para reposo.
- [x] Entrada: error con la forma de la idea 1, «Volvé a entrar», sin el texto de pagos.
- [x] Errores con sesión abierta con la misma forma, una sola salida; «Saldo no disponible».
- [x] Corte durante un pago: «Se cortó la conexión», sin hablar de cobros.
- [x] A1: volver en el inicio sale de la app sin cerrar la sesión (solo Lab).
- [x] A2: «Volver a intentar» y la flecha de un error reusan la sesión (solo Lab).
- [x] Pruebas de pantalla (11) y capturas al lado de la maqueta (`outputs/home-2026/lab7-bloque1.html`).
- [x] Regresión contra el STM de prueba: 20/20 (corre con el canal Alpha; lo exclusivo de Lab se cubre con las pruebas de pantalla y se confirma con el STM real).

## Objetivo 2 · Inicio, montos, tarifas y Express (casi completo)
- [x] Inicio según `inicio.html`: pastilla con mini tarjeta, sin Actividad, «Boletos y tarifas ›», línea de 72 h, «Informado por STM · hoy …».
- [x] Una sola boletera: sin flechita ni selector. Dos o más: selector aunque una sola esté operativa.
- [x] Sin rayo antes del primer pago con Prex; Recargar a todo el ancho (el motor ya lo oculta; probado).
- [x] Saldo negativo: «−$» juntos en rosado, «Saldo negativo», recarga mínima de STM.
- [x] Sin internet: último saldo con su hora.
- [x] Panel de montos en lista, el elegido pintado, «Quedás con», «Otro importe» pintado, fila «Pagás con Prex · titular ›», «Continuar con Prex» (B7: el motor va directo a Prex con `prepareWithPrex`, sin «Tu recarga»).
- [x] Guía de tarifas en hoja (Lab).
- [x] Express: mantener 1,2 s con el botón que se carga (sin anillo, tics que se aceleran, giro, temblor, disparo).
- [x] Nunca dejar Recargar ni el rayo bloqueados por un pago anterior (A3, solo Lab).
- [x] La luz del Express por el borde: se hace junto con la espera de Carga Express (objetivo 4), porque aterriza en su tarjeta.
- [x] La ayuda del rayo solo las primeras veces (hasta usar el Express 3 veces; mientras se mantiene apretado siempre se ve).
- [x] Pruebas de pantalla del inicio (8) con capturas en `outputs/home-2026/app/b2/`.

## Objetivo 3 · Conectando y boleteras ✅
- [x] Conectando y «Cambiando de boletera» con la tarjeta de 360 y «Cancelar» gris (al cambiar de boletera no hay a dónde volver, así que no tiene «Cancelar»); sin rueda de ajustes en las esperas.
- [x] «Tus boleteras» en hoja sobre el inicio, con la lista que ya está en memoria (`switchCard`); «No operativa» sin motivo.
- [x] Primera vez con dos o más: «¿Qué boletera usás?».
- [x] Apodos de boleteras (solo en el celular), también en la pastilla del inicio.
- [x] «Falta un paso en STM» en ámbar con los tres pasos.
- [x] Último saldo al instante mientras conecta (cifrado con una clave del celular; «Olvidar este celular» lo borra).
- [x] Arreglo del adaptador: los botones de ícono de STM se buscan también por su etiqueta (Movimientos). Actividad sigue oculta.
- [x] Pruebas de pantalla (7 nuevas) con capturas en `outputs/home-2026/app/b3/`.

## Objetivo 4 · Pago y Prex
- [x] Una sola espera «Abriendo Prex» (B9), con «Cancelar» como única salida.
- [x] Tu tarjeta, error de tarjeta, «Prex está tardando». Si Prex rechaza la tarjeta, el botón es «Ver qué respondió Prex» (la app no puede reenviar hasta que la página de Prex se destrabe).
- [x] Confirmá tu pago → «Pagar $ …»; desde ahí sin flecha ni volver (A4).
- [x] Captcha integrado «Una verificación y seguimos»: la app espera el captcha que aparece después del primer toque en Confirmar; no da el pago por enviado (A5).
- [x] Procesando → Recarga hecha en una sola pantalla, sin «Pago confirmado / Ver mi saldo» (B8).
- [x] Rechazo: «Prex rechazó el pago · Revisá el saldo de tu Prex o los datos de la tarjeta», «Probar de nuevo» vuelve al importe (A6). Reconocer el aviso «Existen datos incorrectos».
- [x] Express cuando pide la tarjeta y cuando aparece el captcha.
- [x] Pantalla encendida durante el pago.
- [ ] Prueba con un pago real (la hace el dueño).

## Objetivo 5 · Configuración y avisos ✅
- [x] Configuración según `configuracion.html`; «Olvidar este celular» en hoja.
- [x] «Antes de entrar» con «Ya entré a STM con mi usuario gub.uy» / «Es mi primera vez: abrir STM»; el botón principal abre directo el panel del documento (B11).
- [x] Novedades y «Lab 7 está lista»; sin Novedades en una instalación nueva (B12); actualización descargando.

## Objetivo 6 · Movimiento (en curso: prototipo v2 a la espera del dueño; base en la app hecha: `LabMotion.kt`, tarjeta que vuela entre pantallas)
Las maquetas eran pantallas fijas: falta diseñar cómo se pasa de una a otra.
- [ ] Inventario de transiciones: la tarjeta como pieza que viaja (entrada → inicio → pago → resultado → inicio, cambiando entre 360 y 132), cambios de estado del círculo (color y dibujo que se transforman, no que saltan), textos que cambian con fundido, hojas que suben, pasos del pago que avanzan, números que cambian, gesto de volver predictivo.
- [ ] Prototipo HTML animado y navegable del recorrido, para aprobar (mismo método: diseño, pulido, crítica).
- [ ] Implementación en Compose; respeta «Efectos visuales: Nunca» y quitar animaciones.

## Objetivo 6b · Modo demo (pedido del dueño, 15/10)
En Configuración, un interruptor «Modo demo» para probar todo sin plata ni STM: saldo inventado, panel de montos, Abriendo Prex, Tu tarjeta, verificación, Confirmá tu pago, Procesando → Recarga hecha, y Carga Express de punta a punta. Nunca abre STM ni Prex ni manda datos.
- [x] Demo aparte del motor real (`LabDemo.kt`): mismas pantallas, tiempos parecidos, sin acceso a STM ni a Prex; tarjeta de muestra sin campos para escribir. Opciones: forzar rechazo y pedir la verificación.
- [x] Marca «Modo demo» fija en pantalla; tocándola se abre la hoja con las opciones y «Salir del modo demo». Se entra desde Configuración → Ayuda → Modo demo.
- [ ] Sirve para probar las animaciones del objetivo 6 en el celular.

## Objetivo 7 · Cierre y publicación
- [ ] Revisión de conjunto en la app (no solo en maquetas) y auditoría de espacios sobre las capturas reales.
- [ ] Regresión completa y pruebas de pantalla.
- [ ] Versión 0.2.40 Lab 7 (código 67), CHANGELOG con «## Novedades en la app».
- [ ] Publicar Lab 7 una sola vez.

## Para después de Lab 7
- **Lo primero en Lab 8 (pedido del dueño):** cuando Prex rechaza la tarjeta, poder corregirla y reenviarla ahí mismo con «Continuar», como en la maqueta (faltantes.html r3). Hoy la app no reenvía mientras la página de Prex marca el error y ofrece «Ver qué respondió Prex». Toca la lógica del pago (`EmbeddedPrexPayment.submitCard` y el guardia `providerError` de `NativeCardForm`): probarlo con un pago real.
- Recarga para otra persona con la «Recarga rápida» de STM (datos temporales).
- Actividad, cuando el dueño tenga nivel 2 (probable: segundo factor de gub.uy).
- eBROU.
