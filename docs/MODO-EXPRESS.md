# Carga Express — atajo puntual

**0.2.39 Beta 1:** si falta el CVV, escribilo y tocá Listo. Express recuerda esa solicitud mientras resolvés la verificación y continúa después de que Sistarbanc la acepta. Editar los datos, elegir otra tarjeta, un error o salir de la app cancela esa espera. Escribir sin tocar Listo no envía el formulario.

**0.2.38 Beta 1:** después de la verificación, continúa con el titular conocido o la tarjeta completamente autocompletada cuando puede reconocer la aceptación de Sistarbanc. Una versión desconocida del sitio conserva Continuar manual. Sigue intentando una vez la casilla normal, como en 0.2.37; las imágenes/audio quedan a cargo de la persona. No hay una tasa medida de aprobación sin fotos en STM/Prex.

**0.2.36 Beta 1:** Configuración → Información de ayuda permite copiar una medición local del último recorrido, con tiempos por etapa y avances manuales desde la app. No mide los toques dentro de Google ni separa la espera del sitio de la espera de la persona. [Alternativas de automatización del CAPTCHA investigadas](CAPTCHA-ALTERNATIVAS.md).

**Mejoras de 0.2.35 Beta 1:** conserva la preparación mientras STM responde y lleva a la verificación antes de enviar una tarjeta autocompletada. Incluye las mejoras de 0.2.34: solicitud de tarjetas guardadas al campo correcto, reacción al formulario y espera de validación asíncrona. Ver [evidencia y trabajo pendiente](MEJORAS-EXPRESS-2026-09.md).

**Base del recorrido desde 0.2.32:** el alcance y la validación están en [EXPRESS-0.2.32.md](EXPRESS-0.2.32.md). El recorrido anterior de 0.2.30 se conserva como [evidencia histórica](EXPRESS-0.2.30.md).

La recarga común mantiene el botón principal **Recargar boletera**, con elección de importe y medio. Debajo aparece **Carga Express** únicamente en Boletera Beta y cuando la cuenta ya tiene todo lo necesario. No hay configuración, encendido persistente, explicación previa ni pulsación larga.

## Cuándo aparece

- Cuenta verificada, sin otro pago pendiente de revisar.
- Boletera habitual seleccionada y operativa.
- Mínimo vigente leído de STM.
- Prex como medio conocido y un titular válido disponible.

eBROU permanece en la recarga común porque su autorización se abre fuera de Boletera y no puede usar el mismo formulario nativo de tarjeta.

## Recorrido

1. Un toque inicia una sola solicitud con el mínimo vigente. El botón queda ocupado para bloquear duplicados.
2. Boletera vuelve a comprobar boletera, importe y opciones que devuelve STM.
3. Dentro de Prex, avanza por el resumen y el titular sólo si coinciden con la solicitud y el perfil conocido. Una diferencia, un consentimiento o una pantalla desconocida detiene el avance.
4. Boletera Beta puede activar una vez la casilla normal. La persona resuelve las imágenes/audio que aparezcan. El avance posterior requiere reconocer la aceptación del servidor en el componente del proveedor; de lo contrario permanece manual. La respuesta de Google no se exporta ni se trata por sí sola como autorización de Sistarbanc.
5. Al llegar a la tarjeta, enfoca el número y solicita inmediatamente el servicio de autocompletado de Android. Android o Google presenta la tarjeta y controla su autenticación.
6. Si el servicio entrega número, vencimiento y CVV válidos, Boletera continúa una vez. Si falta un dato, enfoca únicamente lo que falta. Después de completarlo y tocar Listo, Express espera la verificación si corresponde y continúa al reconocer la aceptación del proveedor.
7. La confirmación final se automatiza sólo si la tarjeta coincide con una identidad ya verificada en una recarga exitosa. Una tarjeta distinta pide confirmación.

Salir, enviar la app al fondo o elegir la página original suspende el avance automático. Volver a abrir Boletera no reinicia una recarga por sí solo.

## Datos y límites

Boletera no guarda número, vencimiento ni CVV. Los campos se limpian al salir y el CVV se borra al continuar. La app tampoco puede elegir una tarjeta de Google ni reutilizar la huella de gub.uy para desbloquear el depósito de otro proveedor.

El objetivo es reducir el recorrido humano al toque inicial y la autorización del proveedor. No hay garantía de cinco segundos: STM, Sistarbanc, la conexión, un CAPTCHA, un CVV no guardado o una validación adicional pueden demorar o detener el proceso.
