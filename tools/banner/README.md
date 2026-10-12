# Banner del README

Genera `docs/media/boletera-hero-dark.svg` y `docs/media/boletera-hero-light.svg`.

Los textos se convierten en trazos con las fuentes Google Sans de la app (`app/src/main/res/font`), así el banner se ve igual en GitHub, en cualquier navegador y sin depender de fuentes instaladas. Los iconos son los mismos vectores de `res/drawable` y los colores siguen el tema claro y oscuro de Boletera.

```sh
pip install fonttools uharfbuzz
python tools/banner/banner.py
```

La animación es decorativa (el teléfono flota y un punto recorre la ruta) y se desactiva cuando el sistema pide reducir el movimiento.
