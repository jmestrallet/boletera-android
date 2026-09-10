# Modo Express

Express es un atajo configurable para la recarga mínima vigente. La activación no inicia una operación: guarda una boletera, un medio y, para Prex, un perfil de titular. El botón posterior muestra el importe que se va a usar.

## Activación

Desde el saldo, Activar abre la configuración. La persona revisa boletera y mínimo vigente, elige Prex/eBROU y el titular cuando corresponde. Mantener la superficie de activación durante 1,2 segundos llena el anillo de energía; dos pulsos breves anticipan el pulso nativo de reconocimiento. Soltar antes cancela. Una acción de accesibilidad permite activar sin sostener el gesto. El estado final dice Express activado y confirma que todavía no empezó una recarga.

En el saldo, Recarga express muestra el mínimo actual. Ajustar permite cambiar la configuración o desactivarla. Elegir otro importe conserva el recorrido manual. La interfaz usa el sistema gráfico de la app; no incorpora marcas o recursos de Tesla.

## Recorrido

- La configuración se separa por cuenta mediante el identificador local existente y queda ligada a una boletera concreta. Las elecciones de una recarga manual no reescriben Express.
- Cada recarga comienza por una acción explícita. Usa el mínimo obtenido de STM, que el adaptador vuelve a comprobar antes del envío.
- Al llegar a los medios, se comprueba que el elegido esté disponible y que el titular continúe existiendo. Se consume la intención de Express antes de abrir el proveedor.
- Un toque duplicado durante la preparación no inicia otra solicitud. Cancelar, refrescar, volver a ingresar, fallar o pasar a segundo plano descarta la intención automática pendiente antes de empezar la apertura del proveedor.
- Si algo dejó de estar disponible, el flujo queda en la preparación manual con explicación. No sustituye de forma silenciosa un medio o titular.
- Se conserva el paso de autorización del proveedor, sus campos de tarjeta y verificaciones. No guarda PAN, vencimiento ni CVV; no da por acreditado un pago.

## Alcance de la comprobación

Las pruebas usan perfiles ficticios y respuestas de red interceptadas. Cubren la activación sin pago, cancelación del gesto, persistencia aislada por cuenta, cambio de boletera, interrupción en segundo plano, proveedor ausente y un recorrido completo hasta Prex con mínimo de $ 564 y un único envío ante doble toque. No equivalen a completar ni acreditar una recarga real. La vibración física depende del teléfono y de los ajustes del usuario.
