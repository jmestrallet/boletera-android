# Cambios

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
