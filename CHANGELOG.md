# Cambios

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
