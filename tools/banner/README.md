# Banner del README (diseño de Boletera Lab)

Genera `docs/media/boletera-banner.svg` (README, animado) y `docs/media/boletera-social.svg` (vista previa al compartir el link, quieta).

El banner es el inicio de la app puesto de costado: la luz de fondo, el saldo como lo muestra la app, «Te alcanza para…» y la barra de Recargar con el rayo de Carga Express. Los textos se convierten en trazos con las fuentes Google Sans de la app (`app/src/main/res/font`), así se ve igual en GitHub y en cualquier navegador. La tarjeta, el rayo y los colores salen del código de la app (`LabCardHero.kt`, `LabDesign.kt`).

```sh
pip install fonttools uharfbuzz
python tools/banner/banner.py
```

La animación (`motion.py`) cuenta Carga Express en 10 segundos: el rayo se carga y se dispara, la luz recorre el borde, se junta arriba del saldo y cae en él, y el saldo sube de $ 980 a $ 1.240 (solo rueda el número de viajes); después vuelve al principio con la misma subida al revés. Usa las curvas de la app (`LabExpressButton`, `LabExpressLight.kt`). Con «reducir movimiento» queda quieta en el estado final.

`docs/media/boletera-social.png` es `boletera-social.svg` dibujada a 1280 × 640.

Se pasa a la página principal (rama `main`) cuando el diseño nuevo llegue a la versión estable.
