# Auditoría de la experiencia de pago · 0.2.3

Se revisaron la preparación de la recarga, la elección del medio, los datos del titular, los formularios y las pantallas nativas de Prex. El objetivo fue que la siguiente acción se vea y que el texto corresponda a lo que la app sabe y hace.

| Problema observado | Cambio |
| --- | --- |
| El botón quedaba debajo de la pantalla incluso con un titular | Acción fija fuera del contenido desplazable; resumen y selección más compactos |
| «Tu medio habitual» confundía una selección con un uso conocido | Prex y eBROU se muestran directamente, con su selección visible |
| «Elegir titular» llevaba a una lista vacía | Sin datos guardados, «Agregar datos» abre el formulario directamente |
| Un perfil de persona se presentaba como una tarjeta Prex | Textos referidos a datos del titular; no se afirma que haya una tarjeta bancaria guardada |
| Un único titular podía requerir una elección innecesaria | Se selecciona al recuperar la cuenta si no hay otra elección válida |
| Selección, edición y alta estaban mezcladas | Editar abre los datos seleccionados; cambiar abre la lista; agregar crea datos nuevos |
| «Nombre del perfil» era un campo obligatorio adicional | Nombre para guardar opcional; se usa nombre y apellido si queda vacío |
| Guardar deshabilitado no explicaba qué faltaba | Al intentar guardar, se señalan campos y se muestra el dato que falta o requiere corrección |
| Guardar y continuar en Prex también podían quedar fuera de vista | Acciones fijas al pie del formulario y de las pantallas nativas |
| «Pagar» parecía autorizar aunque solo abría el proveedor | «Continuar» conserva el importe y aclara dónde se completa el pago |
| Quedaban textos del antiguo sistema de revisión de pagos | Se retiraron las referencias a conservar solicitudes pendientes |
| El CAPTCHA se describía como comprobación de identidad | Se describe como verificación y solo se pide completarla cuando está presente |

La página original, el CAPTCHA y la autorización del proveedor conservan su funcionamiento. Guardar datos no inicia una operación; avanzar sigue requiriendo una acción explícita. El editor de datos usados en una recarga distingue ese cambio del guardado para próximas recargas.

## Comprobaciones

- Pantalla de 360 × 640 dp: cero titulares, un titular, selección entre doce titulares y eBROU. Con un titular entran los datos y la acción completa sin desplazarse, tanto en claro como en oscuro.
- Pantalla de 411 × 868 dp con tamaño de texto 2×: los cuatro casos anteriores. El contenido puede desplazarse cuando crece; la acción principal queda visible.
- Primera vez: se abre el formulario directamente, se validan campos, se guarda sin nombre adicional y se seleccionan los nuevos datos sin iniciar un pago.
- Regresión del recorrido interceptado STM/Prex: consulta, importe, selección de datos, apertura, regreso al saldo, segunda recarga y protección contra toques duplicados.
- Página de Prex: se conserva la página y no aparece un destello del sitio durante la carga. La acción para avanzar desde el resumen se ve sin desplazarse.
- Compilación release, siete pruebas JVM y lint aprobados.

Registros locales: `outputs/payment-ux-small-0.2.3-test.txt` (7 pruebas), `outputs/payment-ux-dark-0.2.3-test.txt` (5 pruebas) y `outputs/payment-ux-large-text-0.2.3-test.txt` (4 pruebas). Algunos casos se repiten entre configuraciones; esos números no representan pruebas distintas.

Capturas con datos ficticios: `outputs/payment-ux-one-payer-0.2.3.png`, `outputs/payment-ux-one-payer-dark-0.2.3.png`, `outputs/payment-ux-first-use-0.2.3.png` y `outputs/payment-ux-editor-0.2.3.png`.

## Límites

Es una revisión de interfaz y comportamiento local; no acredita un pago real, la compatibilidad de todos los desafíos CAPTCHA ni todos los modelos de teléfono. La autorización y acreditación bancaria siguen pendientes de comprobación. Las páginas bancarias originales pueden tener su propio desplazamiento.
