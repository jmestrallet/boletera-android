# Carga Express — atajo puntual

**Actualización 0.2.30:** el comportamiento vigente de Express, sesión y seguimiento de pagos está en [EXPRESS-0.2.30.md](EXPRESS-0.2.30.md). Las secciones siguientes describen las versiones indicadas y sus antecedentes.

La recarga común conserva el botón principal **Recargar boletera**, con elección de importe y medio. Debajo aparece un control secundario **Carga Express** solo cuando la cuenta ya tiene lo necesario. No hay encendido/apagado ni activación persistente.

Desde 0.2.11, antes de iniciar se muestra una explicación con el mínimo actual variable por cuenta y consulta, medio guardado, gesto y verificaciones humanas. **Aceptar y continuar** inicia esa recarga; **Ahora no**, Atrás o cerrar no inician nada. Desde 0.2.21, **No volver a mostrar** se guarda al marcar la casilla, incluso si luego se cierra con Ahora no. La preferencia se respeta tanto en pulsaciones completas como en toques cortos o cancelados. Si se marca, las siguientes pulsaciones completas inician directamente y soltar antes no abre la ayuda. Olvidar el acceso restablece también esta preferencia de ayuda del teléfono.

## Cuándo aparece

- Cuenta verificada, boletera habitual seleccionada y operativa.
- Mínimo vigente leído de STM.
- Medio guardado compatible: Prex o eBROU.
- Para Prex, almacenamiento disponible y datos válidos guardados; para eBROU, Chrome disponible. No se pide un perfil de Prex para usar eBROU.

Si falta cualquiera de esos datos, Express se oculta. Los datos se guardan mediante el recorrido común que ya existía. La presencia de tarjetas en Google/Android no es consultable por Boletera y no se utiliza como condición: Boletera conserva los campos para el autocompletado del sistema.

## Una recarga Express

1. Mantené apretado **Carga Express** durante 1,2 segundos. El control muestra el medio guardado y el importe mínimo antes del gesto, llena su anillo, extiende doce líneas radiales y emite pulsos. El tiempo del gesto no depende de la velocidad de animación configurada en Android. Soltar antes cancela; un toque corto solo abre la ayuda si no elegiste ocultarla, sin iniciar una solicitud.
2. Al completar el gesto, se muestra la explicación salvo que hayas elegido no volver a mostrarla. Después de aceptar, o directamente si la omitiste, comienza esa única solicitud con el mínimo vigente, la boletera habitual y el medio guardado. La app vuelve a comprobar las condiciones y las opciones devueltas por STM.
Desde 0.2.13, al comenzar la solicitud aparece **Preparando tu recarga · Ya podés soltar**, con las líneas animadas durante la espera. El anillo no se completa antes de que el gesto sea aceptado. Esta señal confirma el inicio de la preparación, no el pago.

3. Con eBROU se prepara el recorrido bancario ya existente y se abre Chrome; no se automatiza la autorización bancaria. En Prex se avanza por el resumen únicamente si moneda e importe coinciden. Se avanza por el titular si coincide con el perfil elegido, el formulario original es válido y no aparece una verificación o dato adicional.
4. El recorrido se detiene en el CAPTCHA original, si lo pide, o en el número de tarjeta. Con CAPTCHA se conserva el botón para continuar después de resolverlo: no se lee su respuesta ni se afirma poder detectar su resolución desde otro origen. En el formulario de tarjeta se enfoca el número para facilitar la sugerencia del sistema. La tarjeta guardada y su autorización con huella pertenecen a Android/Google; Boletera no almacena esos datos ni los envía automáticamente.

No se leen tokens ni se usan APIs financieras alternativas. Los pasos conocidos se ejecutan una sola vez; un error, cambio de importe/titular, formulario desconocido, consentimiento adicional o navegación estancada devuelve el control al usuario sin reintentar. Salir, pasar al fondo o elegir la página original detiene el avance automático. Volver a abrir la app no reanuda una recarga Express por sí solo.

## Cambio desde 0.2.7–0.2.9

Se eliminó la configuración separada de Express y su sustitución del botón principal. La preferencia antigua deja de leerse o escribirse; no modifica las elecciones ordinarias de boletera, medio y titular. El gesto ahora inicia una recarga puntual, en vez de activar un modo para más adelante.

## Validación

Se comprueban visibilidad condicionada, permanencia del botón común, toque corto/cancelación/pulsación completa, interrupción, aislamiento de las preferencias por cuenta y exclusión de duplicados. El recorrido Android interceptado comprueba que una sola pulsación llega al formulario de tarjeta pasando una vez por resumen/titular y sin pulsar Continuar en la tarjeta. CAPTCHA, autocompletado físico y acreditación no quedan demostrados por ese laboratorio.

Desde 0.2.14, la ayuda no usa la expresión titular habitual. El mínimo nunca es un monto fijo: se lee de STM, se borra al actualizar/cambiar de cuenta y se vuelve a validar antes de avanzar. Sin un mínimo reconocido, la recarga no se habilita.

## Esperas — 0.2.22

El panel conserva el estado de un paso ya enviado aunque Express deje de avanzar. Tras 30 segundos sin progreso, fuera de un CAPTCHA, ofrece Revisar esta solicitud o Volver al saldo. La primera opción muestra la página ya abierta, sin cargar una solicitud nueva; no desbloquea un envío pendiente ni confirma un pago. Si el proveedor finalmente avanza, desaparece el aviso. También cubre una página cargada cuyo contenido todavía no se reconoce, para evitar un indicador sin salida.
