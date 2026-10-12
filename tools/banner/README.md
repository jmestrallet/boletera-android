# Banner del README

Genera `docs/media/boletera-banner-dark.svg` y `docs/media/boletera-banner-light.svg`.

Los textos se convierten en trazos con las fuentes Google Sans de la app (`app/src/main/res/font`), así el banner se ve igual en GitHub, en cualquier navegador y sin depender de fuentes instaladas. Los iconos son los mismos vectores de `res/drawable`. El estilo toma la señalética del transporte público: azul, una franja de cartel de parada con el aviso de proyecto independiente y un mapa de líneas.

```sh
pip install fonttools uharfbuzz
python tools/banner/banner.py
```

La animación es decorativa (el teléfono flota y un ómnibus recorre la línea azul) y se desactiva cuando el sistema pide reducir el movimiento.
