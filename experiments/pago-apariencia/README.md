# Apariencia y autocompletado: candidato experimental

Este archivo prueba la idea de presentar la página de Sistarbanc con una apariencia propia y completar datos del titular. **No es una integración de pago ni una APK nueva. No está validado con una operación STM/Prex.**

## Ruta de instalación propuesta

1. Firefox Android y la extensión [Violentmonkey del catálogo de Mozilla](https://addons.mozilla.org/en-GB/firefox/addon/violentmonkey/), que figura disponible para Android.
2. Importar `boletera.user.js` en el editor de scripts de esa extensión y guardarlo. No requiere publicar nuestro código ni pedir habilitación al proveedor.
3. Abrir el enlace original de pago en ese navegador. La propuesta mantiene la página y los controles originales; modifica su apariencia y agrega el perfil del titular.

La instalación permanente de **esta combinación** y su funcionamiento sobre Sistarbanc siguen pendientes de comprobación. No se cambió el navegador predeterminado ni se instaló nada en el teléfono del usuario. Chrome Custom Tabs, usado por la APK actual, no ejecuta este script.

## Qué hace el candidato

- Se ejecuta exclusivamente sobre el origen HTTPS de la pasarela, ruta `/v2/`, fuera de marcos.
- Agrega controles para guardar, borrar y completar nombre, apellido, documento, email y celular. El perfil se guarda en el almacenamiento local de Violentmonkey; no está cifrado por Boletera.
- Exige un único formulario con los cinco controles identificados en el JavaScript público. Un formulario distinto o ambiguo no se completa.
- Conserva valores existentes y correcciones manuales. No completa número de tarjeta, vencimiento, CVV, CAPTCHA ni contraseñas.
- No envía formularios, no pulsa botones de pago, no llama APIs, no cambia tokens, indicadores del proveedor ni identificación del navegador.
- Aplica CSS a la página original; ofrece volver al aspecto original. No oculta la identidad de Sistarbanc ni las verificaciones.

## Evidencia del 9 de septiembre de 2026

- Un complemento mínimo separado, limitado a localhost y datos ficticios, se instaló temporalmente con `web-ext` en Firefox 155.0.1 de un emulador Android. Firefox confirmó su instalación. Esto acredita la instalación de un complemento de laboratorio, **no la ejecución de este userscript**.
- La revisión automática rechazó abrir el formulario local mediante ADB, indicando solamente bloqueo por política. No se completó la comprobación visual Android ni se intentó eludir el rechazo.
- Las pruebas de `tests/payment-userscript.test.cjs` ejercitan el candidato en un DOM simulado. Verifican selección de campos, conservación de valores, navegación dinámica y ausencia de envío automático. No acreditan compatibilidad con Angular real, pago, apariencia final ni ejecución en Violentmonkey.

## Pendiente material

Comprobar instalación persistente, ejecución en el navegador móvil, apariencia sobre el formulario real y reconocimiento de datos por la página. Después hay que evaluar el recorrido de autorización que exija el proveedor. Esta vía por sí sola no resuelve pagar sin ninguna interacción: el CAPTCHA y las confirmaciones originales siguen presentes.

Fuentes: [extensiones Firefox Android](https://extensionworkshop.com/documentation/develop/developing-extensions-for-firefox-for-android/), [metadatos y permisos de Violentmonkey](https://violentmonkey.github.io/api/metadata-block/). No se contactó a personas u organizaciones.
