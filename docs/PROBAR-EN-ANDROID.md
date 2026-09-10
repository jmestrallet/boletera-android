# Cómo probar Boletera 0.1.7

Instalá **boletera-prueba-0.1.7.apk** encima de la versión anterior, sin desinstalarla, para conservar tu acceso guardado. Esta candidata inicia solicitudes reales con Prex/eBROU mediante Chrome y conserva Google como proveedor de autocompletado, según tu elección. La autorización y acreditación todavía necesitan una comprobación real.

1. Ingresá a STM. Si ya guardaste el acceso, podés usar la huella.
2. Elegí tu boletera una sola vez. En los siguientes ingresos la app la usa automáticamente si sigue habilitada. **Cambiar boletera** permite elegir otra.
3. Compará el saldo y el mínimo informados por STM y elegí el importe.
4. Elegí **Prex** o **eBROU**. La próxima vez aparece ese medio seleccionado, con la opción de cambiarlo.
5. **Pagar** prepara una solicitud real y abre la pantalla oficial en Chrome. Revisá allí el importe y completá la autenticación que te pida el proveedor. La app no guarda datos bancarios. No está garantizado que se pueda pagar solo con huella.
6. Al volver, podés **Consultar saldo** sin repetir la solicitud. La confirmación del cobro se revisa en el proveedor; un cambio de saldo no identifica por sí solo esa operación.
7. Antes de otra carga, indicá que verificaste que el pago terminó o que saliste sin autorizarlo. Si autorizaste y no conocés el resultado, revisalo antes de repetir. El aviso se conserva aunque reinicies la app.

**Volver al pago de Prex:** después de ingresar nuevamente a STM, permite abrir el enlace de la misma solicitud pendiente. No crea otra solicitud. Si ya autorizaste, revisá el resultado sin volver a autorizar. Si el proveedor indica que el enlace venció, la app no lo reemplaza automáticamente. eBROU no ofrece esta reapertura.

**Capturas:** habilitadas. Para reportar un fallo, alcanza con la versión, el paso, el mensaje y la referencia. No compartas capturas con contraseñas, datos bancarios o enlaces de pago.

**Olvidar acceso guardado:** elimina las credenciales cifradas, las preferencias y la sesión STM local. No cancela pagos. Se conserva el aviso de una operación pendiente para cuando vuelvas a ingresar con esa cuenta.

Los recorridos de acceso y traspaso se prueban en emulador. La autorización dentro de Prex/eBROU, la huella física y la acreditación de una recarga requieren comprobación en tu teléfono. Ver [validación](VALIDACION.md) y [pago](COMPATIBILIDAD-PAGO.md).
