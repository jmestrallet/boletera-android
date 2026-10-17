# Lab 18 · todo lo definido desde el 16/10 18:19

## Estado: Lab 18 publicada (17/10/2026)
Todo lo de los puntos 1, 2, 4 y 5 está en Lab 18. Pruebas: lógica de pago 82, pantallas de Lab 35 y Alpha 20 (en el OnePlus), internas y lint; dos agentes Sonnet revisaron y sus hallazgos se arreglaron (registro sin nombres, «Cancelar» después de pagar, otra tarjeta en Prex avisa y no paga sola, cuenta regresiva que espera al aviso de Google, versiones no-Lab sin cambios).

Queda para después (punto 3, pendientes de agentes no hechos): entrada (spinner sin internet, contraseña guardada antes de verificar, etc.), accesibilidad (contraste, alturas fijas), Novedades fijas en el código, «Copiar» sin el registro. Por probar con plata real: que Google ofrezca guardar una tarjeta escrita y qué completa al elegir una.

## Lab 19 (pedidos después de publicar Lab 18)
- Al abrir, la búsqueda de versión nueva va antes de la huella (espera hasta 2 s): con aviso, la huella se pide recién al tocar «Ahora no»; con «Actualizar», no se pide.
- «Datos para Prex»: cambiar entre Cédula y Pasaporte ya no borra el número; cada uno guarda el suyo mientras el formulario está abierto.
- Tarjeta: si Google completa los tres datos, sigue sola a «Confirmá tu pago» también en la recarga común (ahí se ve la tarjeta); si falta el código, espera en el código.

## Para Lab 20 (sin publicar)
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
