# Cambios

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
