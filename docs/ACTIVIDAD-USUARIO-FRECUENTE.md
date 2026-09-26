# Actividad y Usuario frecuente — desarrollo en curso

26/9/2026. Rama `codex/actividad-usuario-frecuente`, desde Beta 0.2.39.

## Pedido y nombres

El dueño autorizó implementar Movimientos nativos y Progreso de usuario frecuente. La pantalla se llama **Actividad** y el beneficio **Usuario frecuente**. Rechazó expresamente «Tu devolución». Se mantiene la identidad visual de la aplicación.

## Evidencia real y límite actual

Se ingresó a la cuenta autorizada desde Edge y se abrió **Movimientos** de la boletera operativa. STM navegó a `/app/mistm/cuenta/pages/usuarioNoValidado.xhtml` y mostró: «Tu usuario no tiene garantía de identidad nivel 2 o superior.» No se abrió ni envió una recarga.

No se obtuvo una tabla real de viajes, recargas o devoluciones. Tampoco se comprobó un campo oficial de viajes computables ni su correspondencia con el programa de Montevideo. El acceso denegado no significa que no existan movimientos.

**No publicar esta rama como funcionalidad terminada.** El acceso nuevo está habilitado únicamente en compilaciones de desarrollo (`BuildConfig.DEBUG`). La Beta 0.2.39 publicada sigue siendo la versión para uso normal.

## Implementado para preparar y revisar

- Entrada Actividad desde la home de desarrollo. Consulta el botón original Movimientos solo si la página identifica la boletera seleccionada; no pulsa Recargar en esa consulta.
- Estado nativo que explica el requisito de identidad, con enlace a la ayuda oficial; vuelta al saldo y posibilidad de consultar de nuevo.
- Interfaz mensual con filtros, detalle por movimiento, resumen separado de viajes/recargas/devoluciones y tarjeta Usuario frecuente.
- Totales ausentes si no consta cobertura completa del mes. Progreso ausente si no hay conteo respaldado de viajes computables para esa boletera y mes. No se deriva del número de filas.
- Ejemplos ficticios exclusivamente en `androidTest`. No se incorporan datos privados ni ejemplos simulados al historial de producción.
- Datos temporales en memoria. No se añade almacenamiento de movimientos ni transmisión a terceros.

## Falta para conectar y entregar

1. Acceder a Movimientos con una sesión que STM acepte con nivel intermedio o superior.
2. Inspeccionar columnas, paginación, selección de fechas, categorías, ajustes/anulaciones y demoras reales; implementar el lector de esa estructura observada.
3. Determinar si se pueden identificar viajes computables propios, excluyendo pagos a otras personas y sin equiparar trasbordos con nuevos viajes pagos. Si no hay evidencia suficiente, no mostrar progreso estimado.
4. Contrastar mes actual y anterior con el sitio original, comprobar separación de boleteras/cuentas y validar el recorrido real.
5. Habilitar la función en Beta, actualizar versión, publicar APK y comprobar rama/tag/descarga, después de completar esas verificaciones.

## Verificación local

- 76 pruebas JavaScript y 15 pruebas JVM aprobadas.
- Compilación de desarrollo y lint completados: 0 errores y 44 advertencias; no se generó una nueva versión pública.
- Cuatro pruebas Android nuevas cubren el acceso restringido, vuelta al saldo sin iniciar pago, filtros/detalle y ausencia de totales o progreso cuando faltan datos.
- La regresión existente de tarjetas, saldo y Carga Express pasó al ejecutarse con datos de aplicación limpios. La primera ejecución falló al consultar el portapapeles mientras el emulador mostraba «System UI isn't responding»; se repitió sin modificar la implementación.
- Las capturas de revisión se generan exclusivamente con datos ficticios. No equivalen a una conexión validada con el historial real de STM.

## Fuentes verificadas

- [STM en línea: acceso a movimientos](https://montevideo.gub.uy/stm-en-linea).
- [Programa de beneficios STM de Montevideo](https://montevideo.gub.uy/tipo/area-tematica/sistema-de-transporte-metropolitano/programa-de-beneficios-stm): 40 viajes elegibles, devolución de $2 por viaje contabilizado; no incluye los abonados a otras personas.
- [Nivel de seguridad de ID Uruguay](https://www.gub.uy/identificacion-digital/nivel-de-seguridad). El ingreso puede requerir identidad validada y segundo factor; no se modificó la seguridad de la cuenta.

La fórmula preparada corresponde al programa de Montevideo, no a cualquier régimen STM. No se confirma una devolución hasta leer evidencia oficial de su estado.
