# Express — atajo puntual desde 0.2.10

La recarga común conserva el botón principal **Recargar boletera**, con elección de importe y medio. Debajo aparece un control secundario **Modo Express** solo cuando la cuenta ya tiene lo necesario. No hay encendido/apagado ni activación persistente.

Desde 0.2.11, antes de iniciar se muestra una explicación con el mínimo actual, Prex, datos habituales, gesto y verificaciones humanas. **Aceptar y continuar** inicia esa recarga; **Ahora no**, Atrás o cerrar no inician nada. **No volver a mostrar** está desmarcado inicialmente y solo se guarda al aceptar. Si se marca, las siguientes pulsaciones completas inician directamente. Un toque corto siempre permite volver a consultar la explicación. Olvidar el acceso restablece también esta preferencia de ayuda del teléfono.

## Cuándo aparece

- Cuenta verificada, boletera habitual seleccionada y operativa.
- Mínimo vigente leído de STM.
- Prex guardado como medio habitual y perfil de titular predeterminado válido; un único titular válido puede usarse si no hay otra selección.
- Almacenamiento de perfiles disponible.

Si falta cualquiera de esos datos, Express se oculta. Los datos se guardan mediante el recorrido común que ya existía. La presencia de tarjetas en Google/Android no es consultable por Boletera y no se utiliza como condición: Boletera conserva los campos para el autocompletado del sistema.

## Una recarga Express

1. Mantené apretado **Modo Express** durante 1,2 segundos. El control muestra Prex y el importe mínimo antes del gesto, llena su anillo, extiende doce líneas radiales y emite pulsos. El tiempo del gesto no depende de la velocidad de animación configurada en Android. Soltar antes cancela; un toque corto abre la ayuda sin iniciar una solicitud.
2. Al completar el gesto, se muestra la explicación salvo que hayas elegido no volver a mostrarla. Después de aceptar, o directamente si la omitiste, comienza esa única solicitud con el mínimo vigente, la boletera habitual y el titular predeterminado. La app vuelve a comprobar las condiciones y las opciones devueltas por STM.
Desde 0.2.13, al comenzar la solicitud aparece **Preparando tu recarga · Ya podés soltar**, con las líneas animadas durante la espera. El anillo no se completa antes de que el gesto sea aceptado. Esta señal confirma el inicio de la preparación, no el pago.

3. En Prex se avanza por el resumen únicamente si moneda e importe coinciden. Se avanza por el titular si coincide con el perfil elegido, el formulario original es válido y no aparece una verificación o dato adicional.
4. El recorrido se detiene en el CAPTCHA original, si lo pide, o en el número de tarjeta. Con CAPTCHA se conserva el botón para continuar después de resolverlo: no se lee su respuesta ni se afirma poder detectar su resolución desde otro origen. En el formulario de tarjeta se enfoca el número para facilitar la sugerencia del sistema. La tarjeta guardada y su autorización con huella pertenecen a Android/Google; Boletera no almacena esos datos ni los envía automáticamente.

No se leen tokens ni se usan APIs financieras alternativas. Los pasos conocidos se ejecutan una sola vez; un error, cambio de importe/titular, formulario desconocido, consentimiento adicional o navegación estancada devuelve el control al usuario sin reintentar. Salir, pasar al fondo o elegir la página original detiene el avance automático. Volver a abrir la app no reanuda una recarga Express por sí solo.

## Cambio desde 0.2.7–0.2.9

Se eliminó la configuración separada de Express y su sustitución del botón principal. La preferencia antigua deja de leerse o escribirse; no modifica las elecciones ordinarias de boletera, medio y titular. El gesto ahora inicia una recarga puntual, en vez de activar un modo para más adelante.

## Validación

Se comprueban visibilidad condicionada, permanencia del botón común, toque corto/cancelación/pulsación completa, interrupción, aislamiento de las preferencias por cuenta y exclusión de duplicados. El recorrido Android interceptado comprueba que una sola pulsación llega al formulario de tarjeta pasando una vez por resumen/titular y sin pulsar Continuar en la tarjeta. CAPTCHA, autocompletado físico y acreditación no quedan demostrados por ese laboratorio.
