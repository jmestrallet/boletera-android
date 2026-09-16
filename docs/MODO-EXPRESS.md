# Carga Express — atajo puntual

**Comportamiento vigente desde 0.2.32:** el alcance y la validación están en [EXPRESS-0.2.32.md](EXPRESS-0.2.32.md). El recorrido anterior de 0.2.30 se conserva como [evidencia histórica](EXPRESS-0.2.30.md).

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
4. Un CAPTCHA visible queda a cargo de la persona. Boletera no lo lee ni lo resuelve.
5. Al llegar a la tarjeta, enfoca el número y solicita inmediatamente el servicio de autocompletado de Android. Android o Google presenta la tarjeta y controla su autenticación.
6. Si el servicio entrega número, vencimiento y CVV válidos, Boletera continúa una vez. Si falta un dato, enfoca únicamente lo que falta.
7. La confirmación final se automatiza sólo si la tarjeta coincide con una identidad ya verificada en una recarga exitosa. Una tarjeta distinta pide confirmación.

Salir, enviar la app al fondo o elegir la página original suspende el avance automático. Volver a abrir Boletera no reinicia una recarga por sí solo.

## Datos y límites

Boletera no guarda número, vencimiento ni CVV. Los campos se limpian al salir y el CVV se borra al continuar. La app tampoco puede elegir una tarjeta de Google ni reutilizar la huella de gub.uy para desbloquear el depósito de otro proveedor.

El objetivo es reducir el recorrido humano al toque inicial y la autorización del proveedor. No hay garantía de cinco segundos: STM, Sistarbanc, la conexión, un CAPTCHA, un CVV no guardado o una validación adicional pueden demorar o detener el proceso.
