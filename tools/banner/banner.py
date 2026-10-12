"""Generates docs/media/boletera-hero-{dark,light}.svg.

Every string is converted to outlines with the app's own Google Sans files, so
the banner renders identically on GitHub, in any browser and without fonts.
"""
import sys
from pathlib import Path

from glyphs import text_path

W, H = 1280, 640

# Icons taken from app/src/main/res/drawable (Material Symbols + own symbol).
CARD_ICON = "M6,5 H18 A3,3 0,0 1,21 8 V16 A3,3 0,0 1,18 19 H6 A3,3 0,0 1,3 16 V8 A3,3 0,0 1,6 5 Z M7,10 H17 M7,14 H13"
SWAP = "M4,8 L20,8 M16,4 L20,8 L16,12 M20,16 L4,16 M8,12 L4,16 L8,20"
REFRESH = "M480,800Q346,800 253,707Q160,614 160,480Q160,346 253,253Q346,160 480,160Q549,160 612,188.5Q675,217 720,270L720,160L800,160L800,440L520,440L520,360L688,360Q656,304 600.5,272Q545,240 480,240Q380,240 310,310Q240,380 240,480Q240,580 310,650Q380,720 480,720Q557,720 619,676Q681,632 706,560L790,560Q762,666 676,733Q590,800 480,800Z"
ARROW = "M647,520L160,520L160,440L647,440L423,216L480,160L800,480L480,800L423,744L647,520Z"
SETTINGS = "M370,880L354,752Q341,747 329.5,740Q318,733 307,725L188,775L78,585L181,507Q180,500 180,493.5Q180,487 180,480Q180,473 180,466.5Q180,460 181,453L78,375L188,185L307,235Q318,227 330,220Q342,213 354,208L370,80L590,80L606,208Q619,213 630.5,220Q642,227 653,235L772,185L882,375L779,453Q780,460 780,466.5Q780,473 780,480Q780,487 780,493.5Q780,500 778,507L881,585L771,775L653,725Q642,733 630,740Q618,747 606,752L590,880L370,880ZM440,800L519,800L533,694Q564,686 590.5,670.5Q617,655 639,633L738,674L777,606L691,541Q696,527 698,511.5Q700,496 700,480Q700,464 698,448.5Q696,433 691,419L777,354L738,286L639,328Q617,305 590.5,289.5Q564,274 533,266L520,160L441,160L427,266Q396,274 369.5,289.5Q343,305 321,327L222,286L183,354L269,418Q264,433 262,448Q260,463 260,480Q260,496 262,511Q264,526 269,541L183,606L222,674L321,632Q343,655 369.5,670.5Q396,686 427,694L440,800ZM482,620Q540,620 581,579Q622,538 622,480Q622,422 581,381Q540,340 482,340Q423,340 382.5,381Q342,422 342,480Q342,538 382.5,579Q423,620 482,620Z"
CHECK = "M382,720L154,492L211,435L382,606L749,239L806,296L382,720Z"
INFO = "M440,680L520,680L520,440L440,440L440,680ZM480,360Q497,360 508.5,348.5Q520,337 520,320Q520,303 508.5,291.5Q497,280 480,280Q463,280 451.5,291.5Q440,303 440,320Q440,337 451.5,348.5Q463,360 480,360ZM480,880Q397,880 324,848.5Q251,817 197,763Q143,709 111.5,636Q80,563 80,480Q80,397 111.5,324Q143,251 197,197Q251,143 324,111.5Q397,80 480,80Q563,80 636,111.5Q709,143 763,197Q817,251 848.5,324Q880,397 880,480Q880,563 848.5,636Q817,709 763,763Q709,817 636,848.5Q563,880 480,880ZM480,800Q614,800 707,707Q800,614 800,480Q800,346 707,253Q614,160 480,160Q346,160 253,253Q160,346 160,480Q160,614 253,707Q346,800 480,800Z"

BOLD, MED, REG, TXT = "google_sans_bold.ttf", "google_sans_medium.ttf", "google_sans_regular.ttf", "google_sans_text.ttf"

THEMES = {
    # Public-transport signage: institutional blue, a stop-sign strip and a line map.
    "light": dict(
        bg0="#FFFFFF", bg1="#E7EFF8", glow="#5AA2F0", glowA=0.20, glow2="#D5E8FF", glow2A=0.70,
        grid="#0B2A4F", gridA=0.06, ink="#0B2342", muted="#44566C", faint="#66788E",
        accent="#0E5BB5", chipFill="#FFFFFF", chipFillA=0.9,
        chipStroke="#0B2A4F", chipStrokeA=0.12, chipInk="#1A2E48", rim="#0B2A4F", rimA=0.10,
        tile0="#1A73D9", tile1="#0B3D7A", tileInk="#FFFFFF",
        strip="#0B3D7A", stripInk="#FFFFFF", stripMuted="#A9C6EA",
        lines=["#1A73D9", "#F2A900", "#14A3B8"], stop="#FFFFFF", stopRing="#0B2342",
        sBg="#F4F7FB", sSurface="#FFFFFF", sCont="#DCEBFF", sOnCont="#0B2E5C", sLabel="#1658A8",
        sPrimary="#0E5BB5", sOnPrimary="#FFFFFF", sVar="#E4ECF5", sOnVar="#4E5D6E",
        sOutline="#C9D6E5", sOn="#13233A", frame="#0E1A2A", frameEdge="#4A5A70",
        card0="#1A73D9", card1="#0B3D7A", cardInk="#FFFFFF", cardInkA=0.85,
        toast="#FFFFFF", toastA=0.97, toastStroke="#0B2A4F", toastStrokeA=0.10, toastInk="#0B2342",
        toastMuted="#55667B", shadow="#0B2A4F", shadowA=0.22,
    ),
    "dark": dict(
        bg0="#06101D", bg1="#0B1C32", glow="#2F7BE0", glowA=0.30, glow2="#14396A", glow2A=0.55,
        grid="#FFFFFF", gridA=0.06, ink="#EEF4FB", muted="#A9B9CC", faint="#7F93AA",
        accent="#6CB0FF", chipFill="#FFFFFF", chipFillA=0.05,
        chipStroke="#FFFFFF", chipStrokeA=0.11, chipInk="#DCE7F3", rim="#FFFFFF", rimA=0.07,
        tile0="#2B86F0", tile1="#0B3D7A", tileInk="#FFFFFF",
        strip="#0D3F7C", stripInk="#FFFFFF", stripMuted="#9DBDE6",
        lines=["#3D8EF0", "#F2B400", "#1FB8CC"], stop="#0B1A2E", stopRing="#EEF4FB",
        sBg="#0A131F", sSurface="#131F2E", sCont="#16365F", sOnCont="#DCEBFF", sLabel="#8CC2FF",
        sPrimary="#6CB0FF", sOnPrimary="#06213F", sVar="#1E2B3B", sOnVar="#A9B9CC",
        sOutline="#2C3B4E", sOn="#E6EEF7", frame="#03070C", frameEdge="#3A4A60",
        card0="#2B86F0", card1="#0E4C96", cardInk="#FFFFFF", cardInkA=0.85,
        toast="#132033", toastA=0.94, toastStroke="#FFFFFF", toastStrokeA=0.10, toastInk="#EEF4FB",
        toastMuted="#9FB2C8", shadow="#000000", shadowA=0.55,
    ),
}


def T(s, font, size, x, y, fill, anchor="start", tracking=0, opacity=None, extra=""):
    d, w = text_path(s, font, size, x, y, tracking, anchor)
    op = f' fill-opacity="{opacity}"' if opacity is not None else ""
    return f'<path d="{d}" fill="{fill}"{op}{extra}/>', w


def icon(path, x, y, size, color, viewbox=24, stroke=None):
    s = size / viewbox
    if stroke:
        return (f'<path d="{path}" transform="translate({x} {y}) scale({s:.4f})" fill="none" stroke="{color}" '
                f'stroke-width="{stroke}" stroke-linecap="round" stroke-linejoin="round"/>')
    return f'<path d="{path}" transform="translate({x} {y}) scale({s:.4f})" fill="{color}"/>'


def build(name):
    c = THEMES[name]
    o = []
    a = o.append

    a(f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" fill="none" role="img" aria-labelledby="t d">')
    a('<title id="t">Boletera</title>')
    a('<desc id="d">Tu STM, más simple. Consultá tu saldo y recargá tu tarjeta STM desde una app Android clara y nativa. '
      'Android 8 o posterior, código abierto, sin anuncios y sin servidores propios.</desc>')

    # ---------- defs ----------
    a('<defs>')
    a(f'<clipPath id="frame"><rect width="{W}" height="{H}" rx="32"/></clipPath>')
    a(f'<linearGradient id="bg" x1="0" y1="0" x2="{W}" y2="{H}" gradientUnits="userSpaceOnUse">'
      f'<stop stop-color="{c["bg0"]}"/><stop offset="1" stop-color="{c["bg1"]}"/></linearGradient>')
    a(f'<radialGradient id="glow" cx="960" cy="300" r="460" gradientUnits="userSpaceOnUse">'
      f'<stop stop-color="{c["glow"]}" stop-opacity="{c["glowA"]}"/>'
      f'<stop offset=".55" stop-color="{c["glow"]}" stop-opacity="{c["glowA"]*0.28:.3f}"/>'
      f'<stop offset="1" stop-color="{c["glow"]}" stop-opacity="0"/></radialGradient>')
    a(f'<radialGradient id="glow2" cx="80" cy="-40" r="560" gradientUnits="userSpaceOnUse">'
      f'<stop stop-color="{c["glow2"]}" stop-opacity="{c["glow2A"]}"/>'
      f'<stop offset="1" stop-color="{c["glow2"]}" stop-opacity="0"/></radialGradient>')
    a(f'<pattern id="dots" width="24" height="24" patternUnits="userSpaceOnUse">'
      f'<circle cx="12" cy="12" r="1.1" fill="{c["grid"]}" fill-opacity="{c["gridA"]}"/></pattern>')
    a(f'<radialGradient id="dotsFade" cx="930" cy="320" r="560" gradientUnits="userSpaceOnUse">'
      f'<stop stop-color="#fff"/><stop offset=".7" stop-color="#fff" stop-opacity=".35"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient>')
    a(f'<mask id="dotsMask"><rect width="{W}" height="{H}" fill="url(#dotsFade)"/></mask>')
    a(f'<linearGradient id="tile" x1="0" y1="0" x2="1" y2="1"><stop stop-color="{c["tile0"]}"/><stop offset="1" stop-color="{c["tile1"]}"/></linearGradient>')
    a(f'<linearGradient id="card" x1="0" y1="0" x2="1" y2="1"><stop stop-color="{c["card0"]}"/><stop offset="1" stop-color="{c["card1"]}"/></linearGradient>')
    a(f'<linearGradient id="cardSheen" x1="0" y1="0" x2="1" y2=".6"><stop stop-color="#fff" stop-opacity=".38"/>'
      f'<stop offset=".45" stop-color="#fff" stop-opacity="0"/></linearGradient>')
    a(f'<linearGradient id="edge" x1="0" y1="0" x2="0" y2="1"><stop stop-color="{c["frameEdge"]}"/>'
      f'<stop offset=".5" stop-color="{c["frameEdge"]}" stop-opacity=".35"/><stop offset="1" stop-color="{c["frameEdge"]}" stop-opacity=".1"/></linearGradient>')
    a(f'<linearGradient id="screenFade" x1="0" y1="0" x2="0" y2="1"><stop offset=".72" stop-color="{c["bg1"]}" stop-opacity="0"/>'
      f'<stop offset="1" stop-color="{c["bg1"]}"/></linearGradient>')
    a(f'<filter id="shadow" x="-40%" y="-40%" width="180%" height="180%">'
      f'<feDropShadow dx="0" dy="28" stdDeviation="30" flood-color="{c["shadow"]}" flood-opacity="{c["shadowA"]}"/></filter>')
    a(f'<filter id="soft" x="-40%" y="-60%" width="180%" height="220%">'
      f'<feDropShadow dx="0" dy="14" stdDeviation="16" flood-color="{c["shadow"]}" flood-opacity="{c["shadowA"]*0.8:.2f}"/></filter>')
    a('</defs>')

    # Motion is decorative and switched off for people who ask for less of it.
    route_d = "M700 700 L700 640 L780 560 L1160 560 L1260 460 L1260 -40"
    a('<style>'
      '.float{animation:float 7s ease-in-out infinite}'
      '.float2{animation:float 9s ease-in-out -3s infinite}'
      '.breathe{animation:breathe 8s ease-in-out infinite;transform-origin:960px 300px}'
      f'.bus{{offset-path:path("{route_d}");offset-rotate:0deg;animation:ride 11s cubic-bezier(.45,0,.25,1) infinite}}'
      '@keyframes float{0%,100%{transform:translateY(0)}50%{transform:translateY(-8px)}}'
      '@keyframes breathe{0%,100%{opacity:1;transform:scale(1)}50%{opacity:.78;transform:scale(1.04)}}'
      '@keyframes ride{0%{offset-distance:0%;opacity:0}8%{opacity:1}92%{opacity:1}100%{offset-distance:100%;opacity:0}}'
      '@media (prefers-reduced-motion:reduce){.float,.float2,.breathe{animation:none}.bus{animation:none;offset-distance:70%}}'
      '</style>')

    a('<g clip-path="url(#frame)">')
    a(f'<rect width="{W}" height="{H}" fill="url(#bg)"/>')
    a(f'<rect width="{W}" height="{H}" fill="url(#glow2)"/>')
    a(f'<rect width="{W}" height="{H}" fill="url(#dots)" mask="url(#dotsMask)"/>')
    a(f'<g class="breathe"><rect width="{W}" height="{H}" fill="url(#glow)"/></g>')

    # ---------- line map (behind everything on the right) ----------
    l1, l2, l3 = c["lines"]
    others = [
        (l3, "M1320 250 L1210 250 L1150 190 L1150 -40"),
        (l2, "M720 -40 L720 100 L800 180 L1320 180"),
    ]
    for col, d in others + [(l1, route_d)]:
        a(f'<path d="{d}" stroke="{col}" stroke-width="10" stroke-linecap="round" stroke-linejoin="round" stroke-opacity=".9"/>')
    for (sx, sy) in [(740, 600), (1260, 410), (1150, 120), (1260, 180), (761, 141)]:
        a(f'<circle cx="{sx}" cy="{sy}" r="8" fill="{c["stop"]}" stroke="{c["stopRing"]}" stroke-width="3.5"/>')
    # interchange where two lines cross
    a(f'<rect x="1138" y="168" width="24" height="24" rx="12" fill="{c["stop"]}" stroke="{c["stopRing"]}" stroke-width="3.5"/>')
    a(f'<g class="bus"><circle r="18" fill="{c["bg0"]}" fill-opacity=".9" stroke="{l1}" stroke-width="3"/>'
      f'<path d="M-7 -6 h14 a3 3 0 0 1 3 3 v9 h-20 v-9 a3 3 0 0 1 3 -3z M-10 1 h20 M-6 8 v1 M6 8 v1" '
      f'stroke="{c["stopRing"]}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none"/></g>')

    # ---------- left column ----------
    x0 = 96
    # signage strip, like a stop sign header; doubles as the independence notice
    a(f'<rect width="{W}" height="52" fill="{c["strip"]}"/>')
    a(f'<rect y="52" width="{W}" height="4" fill="{c["lines"][1]}"/>')
    a(T("APP ANDROID PARA TU TARJETA STM", MED, 15, x0, 32, c["stripInk"], tracking=140)[0])
    a(T("Proyecto independiente · No es una app oficial de STM", TXT, 14, W - x0, 31.5, c["stripMuted"], anchor="end")[0])

    # tile + wordmark
    ty = 178
    a(f'<rect x="{x0}" y="{ty}" width="92" height="92" rx="28" fill="url(#tile)"/>')
    a(f'<rect x="{x0 + .5}" y="{ty + .5}" width="91" height="91" rx="27.5" stroke="#fff" stroke-opacity=".22"/>')
    a(icon(CARD_ICON, x0 + 16, ty + 16, 60, c["tileInk"], stroke=1.8))
    wm, wmw = T("boletera", MED, 112, x0 + 118, ty + 80, c["ink"], tracking=-22)
    a(wm)

    # tagline
    t1, w1 = T("Tu STM, ", REG, 50, x0, 352, c["ink"], tracking=-12)
    a(t1)
    a(T("más simple.", REG, 50, x0 + w1, 352, c["accent"], tracking=-12)[0])

    # body
    a(T("Consultá tu saldo y recargá tu tarjeta desde", TXT, 21, x0, 400, c["muted"])[0])
    a(T("una app clara, nativa y sin vueltas.", TXT, 21, x0, 430, c["muted"])[0])

    # chips
    cx, cy, ch = x0, 474, 42
    for label in ["Android 8+", "Código abierto", "Sin anuncios", "Sin servidores"]:
        d, w = text_path(label, MED, 16, 0, 0)
        cw = w + 50
        a(f'<rect x="{cx}" y="{cy}" width="{cw:.1f}" height="{ch}" rx="21" fill="{c["chipFill"]}" fill-opacity="{c["chipFillA"]}" '
          f'stroke="{c["chipStroke"]}" stroke-opacity="{c["chipStrokeA"]}"/>')
        a(icon(CHECK, cx + 13, cy + 12.5, 17, c["accent"], viewbox=960))
        a(T(label, MED, 16, cx + 36, cy + 26.5, c["chipInk"])[0])
        cx += cw + 10

    # ---------- transit card, tilted behind the phone ----------
    a(f'<g class="float2"><g transform="translate(636 296) rotate(-12)" filter="url(#soft)">')
    a('<rect width="250" height="158" rx="20" fill="url(#card)"/>')
    a('<rect width="250" height="158" rx="20" fill="url(#cardSheen)"/>')
    a('<rect x=".5" y=".5" width="249" height="157" rx="19.5" stroke="#fff" stroke-opacity=".25"/>')
    a(T("STM", BOLD, 26, 22, 44, c["cardInk"], tracking=40)[0])
    # contactless arcs, next to the brand so they stay visible beside the phone
    for i, r in enumerate([7, 13, 19]):
        a(f'<path d="M{0} {-r * .8:.1f} A {r} {r} 0 0 1 {0} {r * .8:.1f}" transform="translate({86 + i * 3} 35)" '
          f'stroke="{c["cardInk"]}" stroke-opacity="{c["cardInkA"]}" stroke-width="2.6" stroke-linecap="round"/>')
    a(f'<rect x="22" y="66" width="40" height="30" rx="7" fill="{c["cardInk"]}" fill-opacity=".2"/>')
    a(f'<path d="M22 81 H62 M42 66 V96" stroke="{c["cardInk"]}" stroke-opacity=".18"/>')
    a(T("•••• 1234", MED, 16, 22, 134, c["cardInk"], opacity=c["cardInkA"], tracking=50)[0])
    a('</g></g>')

    # ---------- phone ----------
    px, py, pw, ph = 808, 70, 312, 660
    a(f'<g class="float">')
    a(f'<g filter="url(#shadow)"><rect x="{px}" y="{py}" width="{pw}" height="{ph}" rx="52" fill="{c["frame"]}"/></g>')
    a(f'<rect x="{px + .75}" y="{py + .75}" width="{pw - 1.5}" height="{ph - 1.5}" rx="51.25" stroke="url(#edge)" stroke-width="1.5"/>')
    sx, sy, sw, sh = px + 10, py + 10, pw - 20, ph - 20
    a(f'<clipPath id="screen"><rect x="{sx}" y="{sy}" width="{sw}" height="{sh}" rx="42"/></clipPath>')
    a('<g clip-path="url(#screen)">')
    a(f'<rect x="{sx}" y="{sy}" width="{sw}" height="{sh}" fill="{c["sBg"]}"/>')
    # status bar
    a(T("9:41", MED, 13, sx + 30, sy + 30, c["sOn"])[0])
    a(f'<rect x="{sx + sw / 2 - 11}" y="{sy + 14}" width="22" height="22" rx="11" fill="#000"/>')
    for i in range(3):
        a(f'<rect x="{sx + sw - 70 + i * 5}" y="{sy + 26 - i * 3}" width="3" height="{4 + i * 3}" rx="1" fill="{c["sOn"]}"/>')
    a(f'<rect x="{sx + sw - 50}" y="{sy + 19}" width="20" height="10" rx="3" stroke="{c["sOn"]}" stroke-opacity=".6"/>')
    a(f'<rect x="{sx + sw - 48}" y="{sy + 21}" width="13" height="6" rx="1.5" fill="{c["sOn"]}"/>')
    # header
    hx, hy = sx + 18, sy + 56
    a(f'<rect x="{hx}" y="{hy}" width="36" height="36" rx="11" fill="{c["sCont"]}"/>')
    a(icon(CARD_ICON, hx + 7, hy + 7, 22, c["sOnCont"], stroke=1.9))
    a(T("boletera", MED, 27, hx + 47, hy + 27, c["sOn"], tracking=-10)[0])
    a(f'<circle cx="{sx + sw - 36}" cy="{hy + 18}" r="18" fill="{c["sVar"]}"/>')
    a(icon(SETTINGS, sx + sw - 46, hy + 8, 20, c["sOn"], viewbox=960))
    a(T("STM · 1234", TXT, 13, hx + 2, hy + 66, c["sOnVar"])[0])
    # balance card
    bx, by, bw, bh = sx + 16, sy + 140, sw - 32, 204
    a(f'<rect x="{bx}" y="{by}" width="{bw}" height="{bh}" rx="30" fill="{c["sCont"]}"/>')
    a(T("Saldo disponible", MED, 16, bx + 22, by + 40, c["sLabel"])[0])
    a(icon(SWAP, bx + bw - 44, by + 22, 22, c["sLabel"], stroke=2))
    a(T("$ 1.240", MED, 58, bx + 20, by + 116, c["sOnCont"], tracking=-20)[0])
    a(T("Informado por STM", TXT, 13, bx + 22, by + 148, c["sLabel"])[0])
    a(f'<rect x="{bx + 22}" y="{by + 164}" width="{bw - 44}" height="1" fill="{c["sLabel"]}" fill-opacity=".28"/>')
    a(icon(REFRESH, bx + 21, by + 175, 15, c["sLabel"], viewbox=960))
    a(T("Actualizado a las 18:56", TXT, 12, bx + 42, by + 187, c["sLabel"])[0])
    # primary action
    rx_, ry_, rw_, rh_ = sx + 16, by + bh + 14, sw - 32, 52
    a(f'<rect x="{rx_}" y="{ry_}" width="{rw_}" height="{rh_}" rx="26" fill="{c["sPrimary"]}"/>')
    a(T("Recargar boletera", MED, 17, rx_ + 22, ry_ + 32, c["sOnPrimary"])[0])
    a(icon(ARROW, rx_ + rw_ - 42, ry_ + 15, 22, c["sOnPrimary"], viewbox=960))
    # minimum card
    mx, my, mw, mh = sx + 16, ry_ + rh_ + 14, sw - 32, 74
    a(f'<rect x="{mx}" y="{my}" width="{mw}" height="{mh}" rx="26" fill="{c["sSurface"]}"/>')
    a(f'<rect x="{mx + 14}" y="{my + 15}" width="44" height="44" rx="14" fill="{c["sVar"]}"/>')
    a(icon(CARD_ICON, mx + 24, my + 25, 24, c["sOn"], stroke=1.8))
    a(T("Recarga mínima", TXT, 13, mx + 72, my + 32, c["sOnVar"])[0])
    a(T("$ 260", MED, 21, mx + 72, my + 57, c["sOn"])[0])
    # footnote
    fy = my + mh + 22
    a(icon(INFO, mx + 4, fy, 15, c["sOnVar"], viewbox=960))
    a(T("Los viajes de las últimas 72 horas", TXT, 11.5, mx + 26, fy + 12, c["sOnVar"])[0])
    a(T("pueden estar pendientes de descuento.", TXT, 11.5, mx + 26, fy + 28, c["sOnVar"])[0])
    a('</g>')
    a(f'<rect x="{sx}" y="{sy}" width="{sw}" height="{sh}" fill="url(#screenFade)"/>')
    a('</g>')

    # ---------- floating confirmation ----------
    tx, ty2, tw, th = 1000, 546, 236, 70
    a(f'<g class="float2"><g filter="url(#soft)">'
      f'<rect x="{tx}" y="{ty2}" width="{tw}" height="{th}" rx="22" fill="{c["toast"]}" fill-opacity="{c["toastA"]}"/></g>')
    a(f'<rect x="{tx + .5}" y="{ty2 + .5}" width="{tw - 1}" height="{th - 1}" rx="21.5" stroke="{c["toastStroke"]}" stroke-opacity="{c["toastStrokeA"]}"/>')
    a(f'<circle cx="{tx + 35}" cy="{ty2 + 35}" r="19" fill="{c["sPrimary"]}"/>')
    a(icon(CHECK, tx + 24, ty2 + 24, 22, c["sOnPrimary"], viewbox=960))
    a(T("Recarga confirmada", MED, 15.5, tx + 66, ty2 + 31, c["toastInk"])[0])
    a(T("+ $ 260 · Prex", TXT, 13, tx + 66, ty2 + 52, c["toastMuted"])[0])
    a('</g>')

    a('</g>')
    a(f'<rect x=".5" y=".5" width="{W - 1}" height="{H - 1}" rx="31.5" stroke="{c["rim"]}" stroke-opacity="{c["rimA"]}"/>')
    a('</svg>')
    return "\n".join(o) + "\n"


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else str(Path(__file__).resolve().parents[2] / "docs/media")
    for name in THEMES:
        with open(f"{out}/boletera-hero-{name}.svg", "w", encoding="utf-8") as f:
            f.write(build(name))
