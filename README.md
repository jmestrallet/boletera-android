<p align="center">
  <img src="docs/media/boletera-hero.svg" alt="Boletera · Tu STM, más simple" width="100%">
</p>

<p align="center">
  <strong>Consultá tu saldo y recargá tu tarjeta STM desde una app Android clara y nativa.</strong>
</p>

<p align="center">
  <a href="https://github.com/jmestrallet/boletera-android/releases/tag/v0.2.31"><strong>Descargar Boletera</strong></a>
  ·
  <a href="https://github.com/jmestrallet/boletera-android/releases/tag/v0.2.31-beta.1">Probar Boletera Beta</a>
  ·
  <a href="CHANGELOG.md">Ver cambios</a>
</p>

<p align="center">
  <img src="docs/media/boletera-home.png" alt="Pantalla principal de Boletera" width="330">
</p>

> [!NOTE]
> Boletera es un proyecto independiente y de código abierto. No es una aplicación oficial de STM, la Intendencia de Montevideo ni gub.uy.

## Un proyecto personal para hacer la STM más simple

La idea nació para resolver algo que hago seguido: consultar el saldo y cargar la STM. La web oficial cumple su función, pero para el uso diario quería un camino más directo y cómodo desde el celular.

Como yo ingreso con **Usuario gub.uy** y recargo con **Prex**, empecé por esa combinación. Después decidí compartir el proyecto por si también le resulta útil a otra gente, y abrir el código para que su funcionamiento sea transparente.

## Qué funciona hoy

> [!TIP]
> Por ahora, Boletera funciona mejor con **Usuario gub.uy + Prex**, porque es la combinación que uso habitualmente y pude probar de punta a punta.

- **Ingreso:** por ahora funciona con Usuario gub.uy.
- **Prex:** es la opción que mejor probé y ya la usé para completar recargas reales.
- **eBROU:** hay una primera integración disponible, todavía experimental y pendiente de más pruebas de principio a fin.
- **Otras opciones:** todavía no están incorporadas.

El proyecto recién empieza. La idea es sumar otras formas de ingreso y pago a medida que haya interés, feedback y oportunidades de probarlas bien.

## Cómo funciona

Boletera usa como base el mismo sitio oficial de STM y presenta sus pasos conocidos en una interfaz Android más clara, pensada para consultar el saldo y preparar una recarga con menos vueltas.

La app reconoce las pantallas conocidas y automatiza acciones mecánicas, como navegar entre pasos, elegir la boletera y completar campos que ya autorizaste. El ingreso de Usuario gub.uy, los CAPTCHA y la confirmación final del pago siguen bajo tu control. Si STM o un proveedor muestra una pantalla que Boletera no reconoce, el recorrido se detiene.

## Todo lo importante, en un solo lugar

- Saldo disponible y mínimo de recarga informado por STM.
- Selección de boletera, importe y medio de pago.
- Interfaz nativa para el acceso, la recarga y Prex.
- Tema claro, oscuro o automático.
- Actualizaciones firmadas desde la propia app.

## Elegí la versión que querés usar

| | **Estable** | **Beta** |
|---|---|---|
| Para quién | Uso habitual | Pruebas y novedades anticipadas |
| Carga Express | No incluida | Experimental |
| Actualizaciones | Sólo versiones estables | Sólo versiones Beta |
| Descarga | [`boletera-0.2.31.apk`](https://github.com/jmestrallet/boletera-android/releases/tag/v0.2.31) | [`boletera-beta-0.2.31-beta.1.apk`](https://github.com/jmestrallet/boletera-android/releases/tag/v0.2.31-beta.1) |

Podés cambiar entre **Estable** y **Beta** desde Configuración. Ambas usan la misma firma y conservan los datos de Boletera al instalar una encima de la otra.

## Instalar

1. Descargá la [última versión estable](https://github.com/jmestrallet/boletera-android/releases/tag/v0.2.31).
2. Abrí el archivo `.apk` en un teléfono con Android 8 o posterior.
3. Si Android lo solicita, permití la instalación desde la app con la que descargaste el archivo.
4. Instalá la actualización encima de la versión anterior para conservar tus datos.

La app puede preparar una recarga real. Revisá siempre el importe y la tarjeta antes de confirmar un pago. Los CAPTCHA y las autorizaciones finales quedan bajo tu control.

## Privacidad y seguridad

La idea es que tus datos se queden con vos. Boletera no tiene cuentas propias, servidor ni analítica, y la información no me llega a mí: la conexión ocurre entre tu teléfono y los servicios oficiales que intervienen en el recorrido.

- El documento y la contraseña de gub.uy se guardan únicamente si elegís **Guardar acceso con huella**. Quedan cifrados en el teléfono con una clave de Android Keystore y requieren la biometría fuerte del dispositivo para desbloquearse.
- Los perfiles opcionales de titular para Prex también se cifran localmente.
- El número de tarjeta, el vencimiento y el CVV se usan de forma temporal para completar el formulario original. Boletera no los guarda en su almacenamiento y los limpia al salir del formulario.
- Si una recarga queda pendiente, se conserva localmente un registro cifrado del importe y su estado para evitar que la repitas por error. Ese registro no contiene los datos de la tarjeta.
- No hay anuncios, analítica ni seguimiento remoto.
- La navegación se limita a los sitios verificados de STM, gub.uy y los proveedores de pago.
- Una conexión insegura, una pantalla desconocida o un importe ilegible detienen el recorrido.

Todo el código es público. Si te interesa, podés revisar qué se guarda, cómo se cifra y a qué sitios se conecta. La implementación y sus límites están explicados en [Arquitectura](docs/ARQUITECTURA.md), [sesión y versiones](docs/CANALES-Y-SESION-0.2.31.md) e [integración de pagos](docs/INTEGRACION-NATIVA-PAGO.md).

## Feedback y próximos pasos

Me encantaría saber cómo te funciona. Si encontrás un problema, tenés una idea o querés que sume otro método, podés [abrir un issue](https://github.com/jmestrallet/boletera-android/issues) o escribirme por Telegram: [@mestrallet](https://t.me/mestrallet).

Cada cambio en los sitios oficiales puede exigir una actualización. La versión Beta conserva funciones experimentales que todavía no recomiendo para el uso habitual.

- [Validación y evidencia](docs/VALIDACION.md)
- [Compatibilidad de pagos](docs/COMPATIBILIDAD-PAGO.md)
- [Historial de cambios](CHANGELOG.md)
- [Cómo probarla en Android](docs/PROBAR-EN-ANDROID.md)

## Compilar

Necesitás JDK 17 o 21, Node.js y Android SDK 35.

```powershell
git clone https://github.com/jmestrallet/boletera-android.git
cd boletera-android
npm ci --ignore-scripts
npm test
./gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Para generar los APK release con una firma local configurada:

```powershell
./build.ps1                 # Boletera estable
./build.ps1 -Channel beta   # Boletera Beta
```

Las claves de firma y las credenciales locales están excluidas del repositorio.

## Licencia

[MIT](LICENSE). Las marcas y servicios mencionados pertenecen a sus respectivos titulares.
