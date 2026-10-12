"""Text -> SVG path helper using the app's bundled Google Sans fonts (HarfBuzz shaped)."""
from pathlib import Path

import uharfbuzz as hb
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

FONT_DIR = str(Path(__file__).resolve().parents[2] / "app/src/main/res/font") + "/"
_cache = {}


def _load(name):
    if name not in _cache:
        path = FONT_DIR + name
        blob = hb.Blob.from_file_path(path)
        _cache[name] = (TTFont(path), hb.Font(hb.Face(blob)))
    return _cache[name]


def _fmt(v):
    s = "%.1f" % v
    return s[:-2] if s.endswith(".0") else s


def text_path(s, fname, size, x=0.0, y=0.0, tracking=0.0, anchor="start"):
    """Return (d, width). tracking in thousandths of an em."""
    tt, hbfont = _load(fname)
    upm = tt["head"].unitsPerEm
    buf = hb.Buffer()
    buf.add_str(s)
    buf.guess_segment_properties()
    hb.shape(hbfont, buf, {"kern": True, "liga": True})
    order = tt.getGlyphOrder()
    gs = tt.getGlyphSet()
    scale = size / upm
    track = tracking / 1000 * upm
    infos, poss = buf.glyph_infos, buf.glyph_positions
    total = (sum(p.x_advance for p in poss) + track * (len(poss) - 1)) * scale
    if anchor == "middle":
        x -= total / 2
    elif anchor == "end":
        x -= total
    pen = SVGPathPen(gs, ntos=_fmt)
    cur = 0.0
    for info, pos in zip(infos, poss):
        gx = x + (cur + pos.x_offset) * scale
        gy = y - pos.y_offset * scale
        gs[order[info.codepoint]].draw(TransformPen(pen, (scale, 0, 0, -scale, gx, gy)))
        cur += pos.x_advance + track
    return pen.getCommands(), total
