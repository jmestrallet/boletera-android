# Diseño de Boletera · 0.1.16

La pantalla principal responde primero cuánto saldo hay y cuál es la próxima acción. El importe se elige en un panel; no compite con la consulta del saldo. Configuración reúne apariencia, actualizaciones y acciones de la sesión. Los pasos bancarios conservan su funcionamiento y las verificaciones originales.

## Identidad

- Verde profundo para acciones, lima para el saldo y superficies suaves. Colores semánticos distintos para claro y oscuro; también puede seguir el sistema.
- Google Sans para títulos y Google Sans Text para lectura. Las fuentes se incluyen localmente, con su licencia OFL, sin llamadas a servidores de fuentes.
- Material Symbols oficiales para controles y un símbolo propio de boletera. Icono de inicio adaptable y variante monocromática.
- Márgenes de 24 dp, controles de al menos 48 dp, contenido principal limitado a 600 dp en ventanas anchas. Los paneles y formularios se desplazan cuando el espacio o el teclado lo requieren.

## Movimiento y respuesta

- Entrada de pantalla de 280 ms, con desplazamiento de 16 dp y opacidad. Ocurre al dibujar; no duplica la composición ni recrea el navegador autenticado.
- Botones con respuesta nativa al toque y cambio de curvatura mediante un resorte de amortiguación 0,85 y rigidez 900. El tamaño del área táctil permanece estable.
- Paneles y gesto de actualización de Material 3. La selección de importe da una respuesta táctil breve; no vibra cada control.
- Las preferencias de duración de animación de Android siguen vigentes. El progreso representa espera real, sin porcentajes inventados para el acceso o el pago.

## Comprobaciones

Capturas y pruebas usan valores ficticios: boletera DEMO1234, saldo $ 1.240 y mínimo $ 260. No se presentan como información de una cuenta.

Se revisaron saldo, acceso, importe, medio de pago y configuración en claro y oscuro. Se comprobó la selección de importe, el desplazamiento de paneles y la ausencia de texto truncado mediante los límites de las líneas dibujadas. Matrices: 360 × 640 dp, 411 × 868 dp con fuente 2× y animación desactivada, y 1280 × 720 dp en horizontal. Esto no equivale a haber probado todos los teléfonos ni constituye una medición de 60/120 fps.

Los ocho pares principales de color medidos superan 4,5:1. El menor contraste de texto secundario fue 5,91:1 en claro; en oscuro fue 10,24:1. No se atribuye esta comprobación de colores a una auditoría completa con lector de pantalla.

## Referencias originales

- [Investigación de Material 3 Expressive, Google Design](https://design.google/library/expressive-material-design-google-research).
- [Animaciones de Compose y trabajo en la fase de dibujo](https://developer.android.com/develop/ui/compose/animation/quick-guide).
- [Sistema de movimiento de Material](https://github.com/material-components/material-components-android/blob/master/docs/theming/Motion.md).
- [Áreas táctiles de Android](https://support.google.com/accessibility/android/answer/7101858).
- [Google Sans v14.000](https://github.com/googlefonts/googlesans/releases/tag/v14.000) y [Material Symbols](https://github.com/google/material-design-icons). Sus licencias se distribuyen en `app/src/main/assets/licenses/`.

La dirección visual es propia; Boletera no está diseñada, avalada ni publicada por Google o STM.

## Respuesta al tacto · 0.2.5

Los botones se comprimen al 97% durante la pulsación y vuelven con resorte; mantienen su espacio en el diseño. La flecha acompaña el gesto con 4 dp de desplazamiento. El pulso táctil se produce al activar, no al empezar a tocar: cancelar o deslizar fuera no confirma una acción. Una pulsación sostenida no introduce repetición ni dispara un pago antes de soltar.

La boletera se comprime al 97,5% y gira -1,2 grados al tocarla; toque o pulsación larga abre el selector existente. La pulsación larga utiliza la respuesta nativa de Compose/Android, una vez por reconocimiento del gesto. La alternativa visible Cambiar boletera sigue disponible. El gesto se deshabilita durante una consulta.

El importe seleccionado cambia de superficie y curvatura; la marca aparece con resorte dentro de un espacio fijo. Cambiar importe, apariencia o medio de pago emite un pulso de selección. Volver a elegir la misma opción no genera otro pulso. La configuración de Android controla la respuesta táctil y la escala de duración de Compose; no se pide permiso de vibración ni se usan patrones permanentes.

Referencias consultadas el 10/09/2026:

- [Material 3 Expressive: sistema de movimiento](https://m3.material.io/blog/m3-expressive-motion-theming).
- [Apple: movimiento](https://developer.apple.com/design/human-interface-guidelines/motion).
- [Android: respuesta táctil semántica y ajustes del usuario](https://developer.android.com/develop/ui/views/haptics/haptic-feedback).

Es una implementación propia inspirada en esos criterios. No acredita equivalencia de calidad con productos de Google o Apple, medición de fluidez a 120 Hz ni validación física en Xiaomi.

## Corrección tras feedback · 0.2.8

El usuario no percibía las mejoras de 0.2.5 y rechazó la inclinación de la boletera. Se elimina ese giro; se mantiene una compresión plana al 98,5%. El toque normal y el icono Cambiar boletera solicitan un pulso de interacción ContextClick; la pulsación larga conserva su pulso propio. Los botones e importes también usan ContextClick en lugar de TextHandleMove. La diferencia física depende del dispositivo; no se atribuye una intensidad medida.

Los botones se comprimen al 94%, pasan de radio 32 a 16 dp y desplazan su flecha 10 dp. Los importes alternan radio 12/32 dp. La entrada de contenido dura 380 ms con 40 dp de recorrido y escala 97–100%; sigue existiendo una única composición del navegador. Los colores del tema interpolan durante 360 ms. Las preferencias de duración de Android siguen vigentes.

Configuración incorpora Probar vibración con el pulso largo nativo. Se informa si Android no acepta la petición; una respuesta positiva no se confunde con una comprobación física de que el usuario la sintió. No se fuerza ni modifica la configuración del sistema.
