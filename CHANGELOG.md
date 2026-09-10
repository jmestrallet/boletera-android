# Cambios

## 0.2.17-prueba — ajuste sutil del pie

- Aviso abreviado de viajes pendientes y acceso a Boletos uno al lado del otro.
- Se mantienen los tamaños, paneles y separaciones del resto de la home. El icono original no cambia.

## 0.2.16-prueba — icono original

- Se recupera el icono original de boletera: rectángulo redondeado con dos líneas, tanto dentro de la app como en el lanzador.
- Se conserva la home restaurada de 0.2.15.

## 0.2.15-prueba — vuelve la home anterior

- Se recuperan el título por fuera del panel, el saldo grande y el bloque separado de recarga mínima. Las combinaciones de boletos siguen fuera de la home.
- El pie agrupa su aviso y el acceso a la guía, sin el espacio extra entre ambos.
- Tarjeta con un ómnibus como icono común, también en el lanzador; se retiran las ondas.
- Se conservan Carga Express, el mínimo variable de STM, el medio guardado y las pantallas de error de 0.2.14.

## 0.2.14-prueba — home limpia, Carga Express y errores claros

- Se retiran las combinaciones de boletos del saldo. Identificación, saldo y mínimo vigente se integran en un solo panel; el pie vuelve a entrar en pantalla a tamaño de letra normal.
- Un mismo icono de tarjeta sin contacto representa la boletera en toda la app.
- Modo Express se llama Carga Express. La explicación aclara el mínimo variable por cuenta/consulta y usa el medio guardado; se elimina la expresión titular habitual.
- Carga Express permite el medio guardado Prex o eBROU: Prex necesita los datos requeridos y eBROU necesita Chrome; no se piden perfiles Prex para el banco. Se conserva el recorrido bancario y su autorización humana.
- Pantalla propia para errores controlados de conexión, servicio, datos o verificación, con detalle desplegable y salida. Enviar error al desarrollador queda deshabilitado, rotulado Próximamente.

## 0.2.13-prueba — respuesta de Express y combinaciones de boletos

- Express recupera doce líneas radiales durante la pulsación y la preparación. Al aceptar el gesto muestra que ya se puede soltar, sin esperar la respuesta de STM.
- El anillo mide el tiempo de pulsación y solo se completa al aceptar el gesto, incluso con animaciones de Android desactivadas.
- Combinaciones de boletos comunes de 1 h y 2 h, con sobrante, cambio automático cada cinco segundos y avance al tocar. El cambio se pausa al pasar la app al fondo y respeta los tiempos de accesibilidad.
- Se conservan la ayuda inicial y la protección contra solicitudes duplicadas.

## 0.2.12-prueba — CAPTCHA completo y capturas habilitadas

- La casilla inicial de reCAPTCHA deja de superponerse a la consigna del desafío de imágenes.
- El desafío ampliado ocupa el panel; se ocultan temporalmente los campos y acciones ordinarias, se cierra el teclado y se ajusta el navegador para dibujar el rectángulo completo antes de escalarlo.
- Se conserva el iframe original y los campos temporales de tarjeta al volver. La página original sigue accesible.
- Capturas habilitadas también durante el pago, a pedido del usuario. Se retira el bloqueo de pantalla incorporado en 0.2.9.

## 0.2.11-prueba — ayuda de Express y guía de boletos

- Express muestra una explicación antes del primer uso, con Aceptar y continuar, Ahora no y No volver a mostrar. La preferencia se guarda solo al aceptar. El toque corto vuelve a abrir la ayuda.
- Boletos y tarifas, discreto al pie de la boletera y accesible desde Configuración: precios, explicaciones desplegables, paradas de intercambio y combinación urbana/suburbana.
- Guía disponible sin conexión, con fecha de revisión y enlaces a IM/Cutcsa; distingue precio electrónico, efectivo, categorías y complemento suburbano.
- Cancelar la explicación no inicia solicitudes. Se mantienen la pulsación sostenida y los controles originales del pago.

## 0.2.10-prueba — Express como atajo y regreso al saldo

- Recargar boletera sigue como acción principal. Express es un botón secundario por pulsación sostenida; no tiene configuración ni encendido/apagado.
- Se oculta si falta la boletera habitual operativa, el mínimo vigente, Prex como medio guardado o el titular predeterminado válido.
- Una pulsación completa inicia una sola recarga mínima y avanza por el resumen/titular conocidos hasta el CAPTCHA o los campos de tarjeta. Soltar antes cancela.
- En la llegada directa a la tarjeta se enfoca el número, conservando el autocompletado de Android y su autorización con huella.
- Atrás desde Cambiar boletera regresa al saldo de la habitual conservando la sesión, tanto con la flecha como con Atrás de Android.

## 0.2.9-prueba — tarjeta dentro de la interfaz

- Número, vencimiento y CVV nativos, con teclado numérico, formato, validación y Continuar fijo abajo.
- Transferencia al formulario original solo al tocar Continuar; sus validaciones, solicitud y verificaciones se conservan. Protección contra envíos duplicados.
- Datos transitorios: sin guardado en Boletera, CVV borrado del formulario al enviar y campos limpiados al salir o pasar al fondo. Capturas bloqueadas mientras se muestra el pago.
- Sugerencias de Android para número/vencimiento, sin solicitar guardado ni incluir CVV en el autocompletado. Google en el teléfono sigue pendiente de comprobar.
- Página original disponible; consentimientos adicionales, formularios desconocidos y autorizaciones se muestran allí. Validado con datos ficticios, sin operación bancaria real.

## 0.2.8-prueba — respuesta visible y pulso al tocar

- Se elimina la inclinación de la boletera y se incorpora respuesta táctil en el toque normal, además de la pulsación larga.
- Botones con compresión y desplazamiento de flecha más visibles; selecciones de importe con mayor cambio de forma.
- Entrada de pantalla con desplazamiento y escala; transición gradual entre claro y oscuro.
- Pulsos de interacción en botones, importes, apariencia y medio de pago, en lugar del pulso de selección de texto.
- Probar vibración en Configuración permite comprobar la respuesta del propio teléfono. Respeta los ajustes de Android.

## 0.2.7-prueba — modo Express

- Activación con pulsación sostenida, anillo de energía, pulsos táctiles y confirmación visual. Activar guarda la configuración, sin iniciar una recarga.
- Recarga express muestra y usa el mínimo vigente, la boletera elegida y Prex/eBROU con el titular configurado.
- Saltea la elección repetida de importe y medio; conserva la autorización original del proveedor.
- Configuración por cuenta y boletera, edición/desactivación y recorrido manual para elegir otro importe.
- Revalidación del medio y titular, protección contra duplicados y cancelación del avance automático al interrumpirse.

## 0.2.6-prueba — conservar la causa del error

- Cuando un paso se detiene, la explicación permanece visible en la pantalla junto con la referencia del recorrido.
- Los avisos transitorios, como una huella cancelada, siguen desapareciendo sin desplazar el contenido.
- Corrige la pérdida de información de diagnóstico; no se atribuye todavía una causa al fallo de ingreso reportado en Xiaomi.

## 0.2.5-prueba — respuesta al tacto y movimiento

- Botones con compresión, resorte de retorno, cambio de forma y desplazamiento de la flecha. Un pulso breve acompaña cada activación; mantener presionado no repite la acción.
- La boletera se inclina y comprime al tocarla. Toque o pulsación larga abre la selección de boletera; la pulsación larga usa el pulso nativo de Android. El gesto de desplazamiento se conserva.
- Los importes cambian suavemente de forma y color, con un indicador que aparece con resorte. Importe, apariencia y medio de pago responden al cambiar la selección.
- Se respetan las preferencias de animación y respuesta táctil del sistema.

## 0.2.4-prueba — instalador automático

- Actualizar ahora descarga y verifica la APK; al terminar abre la confirmación de Android sin otro toque.
- Al habilitar la instalación desde Boletera y volver, continúa automáticamente.
- Si se deniega el permiso o se cancela la instalación, no insiste en bucle; conserva la opción manual.
- Una descarga terminada en segundo plano espera a que la app vuelva a estar activa antes de abrir el instalador.

## 0.2.3-prueba — pago más claro y acción siempre visible

- Preparación de recarga compacta con importe, selección directa de Prex/eBROU y acción fija al pie.
- Primera vez: Agregar datos abre directamente el formulario. Un solo titular se selecciona automáticamente; varios se eligen en una lista aparte.
- Se distingue titular de tarjeta. Se retiran textos que asumían uso habitual o una segunda Prex.
- Formulario con nombre para guardar opcional, validación visible y Guardar datos fijo. Guardar no inicia una operación.
- Continuar también queda fijo en las pantallas nativas de Prex. Se corrigen textos obsoletos sobre pagos pendientes y se distingue verificación de identidad.

## 0.2.2-prueba — huella al abrir

- Con acceso guardado, se solicita automáticamente la huella al abrir la app, cuando la pantalla ya está activa.
- Cancelar o fallar conserva el acceso y permite reintentar o ingresar manualmente. No se repite al cerrar sesión, recomponer la pantalla o regresar de otra aplicación.
- Se conserva el mismo desbloqueo cifrado mediante Android.

## 0.2.1-prueba — recarga sin revisión manual

- Se retiran el cartel, la confirmación manual, la recuperación de enlaces y el bloqueo persistente de solicitudes anteriores. Al salir de Prex se consulta el saldo; regresar de eBROU también lo actualiza.
- Se limpian los marcadores antiguos conservando accesos, perfiles y favoritos. El contexto de la operación abierta queda solo en memoria.
- Se mantiene el bloqueo de toques duplicados durante la apertura de una operación.
- Los avisos aparecen flotando, con cierre automático y manual, sin desplazar la pantalla.

## 0.2.0-prueba — nueva etapa visual

El rediseño pasa a la serie 0.2. Código de versión Android 18 para permitir actualizar desde 0.1.16. Misma experiencia y funcionamiento que la entrega anterior.

## 0.1.16-prueba — nueva experiencia

- Rediseño de acceso, saldo, recarga, titulares y configuración; tipografía Google Sans, iconos Material Symbols e identidad adaptable en el inicio de Android.
- Saldo protagonista y selector de importe en un panel. Conserva el gesto de actualizar y la revisión de solicitudes pendientes.
- Apariencia clara, oscura o según el sistema; preferencia guardada en el teléfono.
- Transiciones de pantalla, respuesta al toque y paneles nativos, respetando la duración de animaciones de Android.
- Configuración concentra actualizaciones y acciones de la sesión. No cambia la autorización ni las verificaciones del proveedor de pago.

Ver [criterios de diseño y referencias](docs/DISENO.md).

## 0.1.15-prueba — actualizaciones desde Configuración

- Agrega Configuración con versión instalada, búsqueda en GitHub, descarga e instalación mediante la confirmación de Android.
- Incluye las versiones de prueba publicadas del repositorio oficial; solo ofrece versiones posteriores. Comprueba tamaño, SHA-256, identidad de la app, versión interna y firma antes de ofrecer instalar.
- La APK se descarga en almacenamiento privado y se comparte únicamente con permiso temporal de lectura. Android puede pedir habilitar la instalación desde Boletera.
- No necesita cuenta ni token de GitHub. La consulta y la descarga se inician desde Configuración y conservan su estado al girar la pantalla.

## 0.1.14-prueba — deslizar para actualizar

- En la pantalla de saldo, arrastrar hacia abajo desde el comienzo actualiza saldo y mínimo, con indicador de carga.
- Retira el botón inferior «Actualizar saldo y mínimo». El gesto solo funciona en saldo y no repite consultas mientras hay una en curso.

## 0.1.13-prueba — carga sin destello de la página

- Conserva la pantalla de carga nativa mientras Sistarbanc prepara el formulario, evitando mostrar brevemente la página original.
- Oculta el navegador al comenzar una navegación y descarta respuestas de la navegación anterior.
- Mantiene disponible «Ver pantalla de Sistarbanc» durante la carga como salida manual.

Verificado con una carga ficticia demorada en Android y prueba de conservación de sesión. Instalada en el OnePlus; no se inició ni autorizó un pago. Candidato local sin publicación remota.

## 0.1.12-prueba — panel adaptable

- Presenta el componente original del CAPTCHA en una posición estable sin copiarlo ni recrearlo; restaura su presentación al volver a la página original.
- Calcula el panel con el espacio disponible en la ventana y las medidas CSS del navegador, en lugar del tamaño físico completo de la pantalla.
- Prueba Android de distintos anchos y densidades, desafío expandido ficticio y continuidad del mismo iframe. [Alcance y resultados](docs/CAPTCHA-RESPONSIVO.md).

Continúa pendiente resolver la verificación real y comprobar el resto del pago. Candidato local, todavía no publicado.

## 0.1.11-prueba — candidato nativo

- Resumen y confirmación del titular con controles Android propios, alimentados por la solicitud vigente de Sistarbanc. La página conserva la sesión por detrás.
- Edición nativa de datos ordinarios para el pago en curso, con cédula y pasaporte. No modifica el perfil persistido vinculado a la solicitud.
- Panel con el CAPTCHA original recortado; no lo copia, lee ni resuelve. Las pantallas de tarjeta, confirmación bancaria y diálogos del proveedor conservan su interfaz original.
- Continuación limitada a los dos pasos reconocidos y a una acción por paso; no hay botón automático de autorización del pago.

El candidato requiere completar la verificación humana y comprobar el resto del recorrido en el teléfono. No acredita una recarga exitosa.

## 0.1.10-prueba

- Los perfiles usan cédula uruguaya por defecto y permiten elegir pasaporte. Los perfiles anteriores se interpretan como cédula sin volver a pedir datos.
- Selecciona el tipo de documento en el control original de Prex, además de completar el número. Respeta los cambios manuales posteriores.
- Los pasaportes conservan letras; una cédula sigue requiriendo entre 7 y 8 dígitos.

Pruebas automáticas cubren el valor por defecto, la elección de pasaporte, las opciones del selector original y la conservación de cambios manuales. No se autoriza ni confirma un pago automáticamente.

## 0.1.9-prueba

- Corrige el rechazo TLS de los dominios de Sistarbanc en Android 11. Incluye el certificado público R46 firmado por USERTrust, validado contra el almacén del OnePlus 6T, solamente para los dos dominios exactos de la pasarela.
- Conserva la validación de nombre, fecha y conexión segura de Android. No modifica los certificados del teléfono ni omite errores TLS.

La prueba pública en el OnePlus reprodujo el rechazo de ambos dominios antes del cambio y verificó HTTPS y WebView después. No incluye autorización ni acreditación de un pago. Evidencia: [compatibilidad TLS](docs/TLS-ANDROID11.md).

## 0.1.8-prueba

- Abre Prex dentro de Boletera con una identificación de navegador comprobada contra la pantalla pública de Sistarbanc. eBROU conserva el traspaso a Chrome.
- Agrega perfiles cifrados de titular: nombre, apellido, cédula, correo y celular. Elegir otra Prex permite crear un perfil vacío; la preferencia se recuerda por cuenta.
- Vincula el perfil a la solicitud pendiente. Cambiar el favorito no cambia una solicitud existente; no se permite editar o borrar su perfil mientras esté pendiente.
- Completa los datos ordinarios en los dos formatos identificados del formulario. No lee ni guarda número de tarjeta, vencimiento o CVV, ni cambia el servicio de autocompletado de Android.
- Conserva la página al salir y volver: no recarga el formulario ni repite un envío. Después de reiniciar conserva el enlace original cifrado.
- Advierte si la página ya trae datos distintos del perfil elegido y pide una acción explícita antes de reemplazarlos. Conserva las correcciones manuales posteriores.
- Integra la apariencia de pago y mantiene el CAPTCHA y las confirmaciones originales. No resuelve ni pulsa automáticamente la autorización.

La prueba pública sin operación acredita que el detector inicial no rechaza la variante. El recorrido completo con datos ficticios y la pantalla dibujada se comprobaron en Android. El formulario real con operación, Google en el teléfono, CAPTCHA, autorización y acreditación siguen pendientes: esta versión no acredita un pago completo.

## 0.1.7-prueba

- Exige completar el ingreso antes de mostrar o permitir reconocer un pago pendiente. Un intento de acceso sin verificar ya no puede eliminar ese aviso.
- Desplaza la página original cuando su verificación queda fuera de la vista, antes de mostrar el recorte nativo. No copia el desafío ni automatiza su respuesta.
- Comprueba que el desafío entre completo; si no se puede encuadrar, detiene el paso con una explicación.

Las dos regresiones se reprodujeron antes de corregirlas. Aprobadas 22 pruebas JS, 6 JVM, lint y 11 Android; el panel chico y expandido recibió toques en un fixture local. CAPTCHA real, biometría física, autorización bancaria y acreditación siguen sin validación completa.

## 0.1.6-prueba

- Permite volver al enlace de la solicitud anterior de Prex desde la revisión del pago o el saldo, sin volver a seleccionar proveedor ni crear otra solicitud en STM.
- Conserva el enlace cifrado por cuenta y operación. Después de reiniciar, el botón aparece al completar el ingreso; no basta con escribir un documento.
- Elimina el enlace al reconocer el resultado del pago o al olvidar el acceso. La pérdida del enlace no elimina el aviso de pago pendiente.
- Muestra importe, proveedor y terminación de la boletera del pago por revisar, aunque se esté consultando otra boletera.
- Conserva el autocompletado de Google. No incorpora perfiles bancarios ni un servicio propio de autocompletado.

La reapertura no confirma el estado bancario: el enlace puede vencer y el usuario debe evitar autorizar dos veces. No se agrega reapertura de eBROU porque su inicio usa un formulario POST.

## 0.1.5-prueba

- Recuerda la boletera operativa y el medio de pago por cuenta, con opciones para cambiarlos.
- Habilita Pagar con Prex y eBROU: prepara la solicitud real en STM y abre el proveedor en Chrome. La excepción visual de Chrome queda pendiente de aceptación del usuario.
- Conserva el formulario original de eBROU mediante un traspaso local de un solo uso, validando destino, moneda e importe; las credenciales bancarias se ingresan en el proveedor.
- Guarda un aviso antes de iniciar el pago para evitar otro intento automático al volver o reiniciar. Consultar saldo no reenvía el pago ni declara acreditación.
- Corrige el regreso a saldo y el cambio de boletera sin volver al inicio público de STM ni aceptar respuestas del documento anterior.

APK release comprobada contra STM real en Android 16: acceso, preferencias conservadas entre ingresos, Prex hasta resumen oficial, eBROU hasta ingreso oficial y regreso al saldo. No se ingresaron datos bancarios ni se autorizó un pago. Aprobadas 21 pruebas JS, 6 JVM, lint y las 8 pruebas Android finales. Teléfono, CAPTCHA, biometría y acreditación pendientes.

## 0.1.4-prueba

- Selecciona Continuar dentro del formulario correspondiente al documento o la contraseña.
- Permite el dominio exacto de intermediación de identidad de la Intendencia observado durante el ingreso real.
- Selecciona la boletera mediante su celda y reconoce el botón de recarga con icono.
- Usa el control numérico de STM para que el importe enviado coincida con el elegido.
- Habilita inspección WebView únicamente en compilaciones de desarrollo; la APK de entrega no es depurable.

Ingreso real probado en emulador Android 16 hasta saldo/mínimo. Adaptador del importe comprobado contra STM hasta el paso previo al proveedor, sin pagar. CAPTCHA real, biometría física y confirmación en el teléfono siguen pendientes.
## 0.1.3-prueba

- No clasifica un documento que todavía está cargando como una cuenta autenticada.
- Reconoce el formulario de acceso aunque se muestre bajo la dirección protegida de boleteras.
- Espera filas legibles antes de descartar los datos temporales de ingreso; una tabla sin interpretar ya no se anuncia como ausencia de boleteras.
- Si el acceso vuelve a aparecer, distingue la interrupción por segundo plano del regreso al formulario. No atribuye automáticamente el error a una sesión vencida en STM.
- Muestra una referencia con las últimas etapas y conteos de filas. No incluye documento, contraseña, número de tarjeta, cookies ni direcciones de sesión.
- Excluye las capturas adjuntas del repositorio público.

El recorrido de prueba reproduce un formulario de login bajo la dirección de tarjetas y una tabla que aparece con demora. El resultado real de esta versión debe confirmarse en el teléfono.

## 0.1.2-prueba

- Corrige la lectura de boleteras cuyo número y estado aparecen en elementos HTML contiguos sin espacios. Antes se concatenaban y la lista quedaba vacía.
- Descarta resultados de JavaScript de otro origen durante un cambio de documento y de solicitudes de acceso ya canceladas. No amplía la lista de sitios permitidos.
- Habilita capturas de pantalla para reportar errores, a pedido del usuario, y muestra la versión de la app en el encabezado.
- Agrega regresiones para el formato compacto de las filas y para resultados de un documento anterior.
- Conserva la firma de actualización y el formato del acceso cifrado.

El error de lista vacía se reprodujo con un fixture antes de corregirlo. La hipótesis de transición para el mensaje de recorrido bloqueado necesita confirmación en el teléfono. No se habilitaron pagos.

## 0.1.1-prueba

- Complemento del certificado intermedio público de STM para la conexión TLS en Android.
- Reconocimiento del botón de Usuario gub.uy cuando incluye texto descriptivo.

## 0.1.0-prueba

- Prototipo de interfaz propia, motor WebView local y acceso cifrado con biometría.
