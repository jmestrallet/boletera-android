# Canales y recuperación de sesión · 0.2.31

## Pública y Beta

La app mantiene el mismo identificador, firma y datos locales en ambos canales. La variante Pública no muestra Carga Express. La Beta conserva el comportamiento de Express publicado en 0.2.30; esta entrega no lo corrige ni lo amplía.

Configuración permite elegir **Pública** o **Beta**. La app consulta solo las publicaciones del canal elegido y exige una APK con el mismo paquete y firma. Cambiar de canal puede instalar una variante con el mismo código de versión cuando ambas corresponden a la misma base; Android sigue mostrando su confirmación de instalación. La descarga y la instalación nunca comienzan solo por tocar el selector.

La Pública 0.2.31 muestra una sola vez un aviso sobre la Beta experimental. **Ver Beta** selecciona ese canal y abre Configuración; **Seguir en Pública** conserva el canal estable. La elección se guarda para las búsquedas futuras. Instalar manualmente la APK del otro canal también actualiza esa elección al abrirla.

Las publicaciones públicas usan etiquetas `vX.Y.Z`. Las Beta usan `vX.Y.Z-beta.N`. La búsqueda pública ignora Beta y la búsqueda Beta ignora Pública. Ambos APK comparten `applicationId`, firma y código base para poder reemplazarse sin borrar los datos de Boletera.

## Sesión vencida sin otra huella

`AccessVault` conserva su diseño: el documento y la contraseña persistentes siguen cifrados con Android Keystore y cada lectura desde disco exige biometría fuerte. Al completar ese primer desbloqueo, 0.2.31 crea además una copia AES-GCM con clave aleatoria que vive únicamente en la memoria del proceso.

Si STM vuelve al formulario de acceso por vencimiento, Boletera intenta primero la sesión web existente. Si ya no sirve, abre la copia en memoria, vuelve a ingresar detrás de la pantalla y limpia inmediatamente los valores de trabajo. El saldo y la boletera ya leídos pueden permanecer visibles mientras se recupera. No se solicita otra huella durante esa ejecución.

La copia en memoria se elimina al cerrar sesión local, olvidar el acceso, cancelar el ingreso, cambiar de cuenta, detectar credenciales rechazadas o destruir el proceso. Después de cerrar la app por completo o de que Android mate el proceso, el próximo acceso vuelve a depender de `AccessVault` y de la huella.

## Límites financieros

La recuperación elimina el importe preparado y todo avance de pago. No vuelve a seleccionar un proveedor, no reenvía una recarga y no confirma un pago. Si existía una operación incierta, mantiene la revisión cifrada y bloquea otra recarga hasta que la persona compruebe el resultado.

La prueba automatizada usa páginas locales y datos ficticios. Comprueba una sesión válida reutilizada, vencimiento con reingreso cifrado en memoria, vencimiento sin copia que solicita acceso otra vez y vencimiento después de entregar una solicitud al proveedor sin reenvío. Falta comprobar el nuevo reingreso silencioso con una cuenta real en el teléfono del dueño.
