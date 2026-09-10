# Notas de implementación

## Separación

- `MainActivity.kt`: interfaz Compose, formularios nativos, aviso previo al pago, prueba local de autofill y panel original del CAPTCHA.
- `AccessVault.kt`: cifrado de documento/contraseña ligado a biometría, sin exportar claves de Android Keystore.
- `StmEngine.kt`: un solo WebView, acciones seriales, caducidad de credenciales temporales, navegación restringida, cancelación y limpieza local. No hay puente JS → Android accesible al sitio.
- `stm-adapter.js`: `snapshot()` devuelve exclusivamente etapa, boleteras, saldo/mínimo en centésimos, indicador de error y geometría del CAPTCHA; `command(action,value)` ejecuta una acción permitida en la etapa esperada. Nunca devuelve HTML, cookies, tokens o valores de contraseña/tarjeta.
- `Models.kt`: importes exactos en centésimos y política de navegación por esquema/host/puerto, sin coincidencias por sufijo.

## Recorrido implementado

Inicio propio → autorización biométrica opcional → entrada oficial → elección interna de Usuario gub.uy → documento → contraseña → selección propia de boletera → saldo → lectura de mínimo en el formulario de recarga → selección de monto → elección de proveedor → Prex integrado o eBROU en Chrome → resultado original del proveedor → consulta nueva de saldo.

Los identificadores temporales del login se obtienen siguiendo el sitio; no se reutiliza el enlace `process_state` compartido en el chat. Los importes se vuelven a validar desde el DOM al enviar el formulario de monto. El envío del proveedor está separado del adaptador general y requiere una solicitud activa y una elección explícita. Prex conserva su página original; la confirmación final tiene un comando acotado al componente visible, importe coincidente y toque explícito del usuario.

## CAPTCHA

El detector inspecciona únicamente geometría y visibilidad de iframes oficiales de reCAPTCHA; da prioridad al desafío expandido sobre el checkbox y omite anchors invisibles. `CaptchaHost` mantiene el WebView con su viewport, desplaza su dibujo mediante layout nativo y recorta/escala la región. Las interacciones las recibe el iframe original.

Esto es experimental: cambios de proveedor, orígenes nuevos, scroll, visibilidad y detección de WebView pueden impedirlo. No se puede afirmar que Google permita o que todos los desafíos admitan este encuadre. Una prueba de geometría no acredita que un CAPTCHA real se resuelva. El usuario debe probar los desafíos en su dispositivo.

## Firma, registros y errores

La APK entregada se compila como release sin depuración y se firma con una clave local de prueba fuera del código fuente. La clave de firma no se usa para cifrar credenciales. No compartir `.tools`, `local.properties`, claves ni perfiles de emulador.

Los mensajes de error al usuario son genéricos y no incluyen URL con parámetros. El logging de consola WebView está deshabilitado. Nunca se continúa ante un error SSL. Cambios de huellas pueden invalidar el acceso guardado; existe borrado explícito.

La versión actual puede realizar un pago. No reintenta automáticamente una confirmación interrumpida ni infiere éxito al salir. La lectura de resultado y el retorno se documentan en [CIERRE-PAGO.md](CIERRE-PAGO.md).
