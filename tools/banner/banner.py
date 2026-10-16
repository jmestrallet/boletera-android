"""Generates docs/media/boletera-banner.svg (README) and docs/media/boletera-social.svg (link preview).

The banner is the app's home turned sideways (Boletera Lab's design, 16/10/2026): the ambient light, the balance as
the app shows it, «Te alcanza para…», and the Recargar pill with the Express bolt. Every string is converted to
outlines with the app's own Google Sans files, so it renders the same on GitHub, in any browser and without fonts.
Shapes come from the app: the STM card and its badge (LabCardHero.kt), the bolt's nine-lobed button (LabDesign.kt
polarPath) and the colours of LabDesign.kt.
"""
import math
import sys
from pathlib import Path

from glyphs import text_path
import motion as M

REG, MED = "google_sans_regular.ttf", "google_sans_medium.ttf"
BASE, ON, MUTED, FAINT = "#0A0D13", "#ECEEF6", "#B9C2D3", "#8B95A8"
PRIMARY, ON_PRIMARY = "#B3D0FF", "#08284F"
CARD_BLUE, BADGE_GREEN, BADGE_RING = "#1A73D9", "#2FA35A", "#0B111A"

# LabCardHero.kt: the card is 44×28 units, turned −10°, with four slanted bars; the badge sits at (20, 12).
CARD_BARS = [
    "M-11.932,4.887 L-6.632,-3.313 A1.45,1.45 0 0,0 -9.068,-4.887 L-14.368,3.313 A1.45,1.45 0 0,0 -11.932,4.887 Z",
    "M-4.932,4.887 L0.368,-3.313 A1.45,1.45 0 0,0 -2.068,-4.887 L-7.368,3.313 A1.45,1.45 0 0,0 -4.932,4.887 Z",
    "M2.068,4.887 L7.368,-3.313 A1.45,1.45 0 0,0 4.932,-4.887 L-0.368,3.313 A1.45,1.45 0 0,0 2.068,4.887 Z",
    "M9.068,4.887 L14.368,-3.313 A1.45,1.45 0 0,0 11.932,-4.887 L6.632,3.313 A1.45,1.45 0 0,0 9.068,4.887 Z",
]
BOLT = ("M11,21h-1l1,-7H7.5c-0.58,0 -0.57,-0.32 -0.38,-0.66 0.19,-0.34 0.05,-0.08 0.07,-0.12C8.48,10.94 10.42,7.54 13,3h1l-1,7"
        "h3.5c0.49,0 0.56,0.33 0.47,0.51l-0.07,0.15C12.96,17.55 11,21 11,21z")
ADD = "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"


def T(s, font, size, x, y, fill, anchor="start", tracking=0):
    d, w = text_path(s, font, size, x, y, tracking, anchor)
    return f'<path d="{d}" fill="{fill}"/>', w


def mark(cx, cy, k):
    """The card with its «+» badge, as the app's entry shows it."""
    bars = "".join(f'<path d="{b}" fill="{CARD_BLUE}"/>' for b in CARD_BARS)
    return (f'<g transform="translate({cx} {cy}) scale({k})"><g transform="rotate(-10)">'
            f'<rect x="-22" y="-14" width="44" height="28" rx="5.5" fill="#fff"/>{bars}</g>'
            f'<g transform="translate(20 12)"><circle r="10.2" fill="{BADGE_RING}"/><circle r="9" fill="{BADGE_GREEN}"/>'
            f'<path d="M0,-4.2V4.2M-4.2,0H4.2" stroke="#fff" stroke-width="2.3" stroke-linecap="round"/></g></g>')


def bolt_button(cx, cy, size):
    """LabDesign.polarPath(size, 9 lobes, depth .07) with the bolt (ic_bolt) in the middle."""
    r = size / 2 * .98
    base = r / 1.07
    pts = []
    for i in range(181):
        t = i / 180 * 2 * math.pi
        rr = base * (1 + .07 * math.cos(9 * t))
        pts.append(f"{cx + rr * math.cos(t - math.pi / 2):.1f},{cy + rr * math.sin(t - math.pi / 2):.1f}")
    s = size / 76
    return (f'<path d="M{" L".join(pts)}Z" fill="{PRIMARY}"/>'
            f'<path d="{BOLT}" fill="{ON_PRIMARY}" transform="translate({cx - 14 * s:.1f} {cy - 14 * s:.1f}) scale({28 * s / 24:.4f})"/>')


def ambient(w, h):
    """LabAmbient: the dark base and three soft lights, sized by a phone-like width so a wide banner keeps the app's look."""
    u = min(w, h * .9)
    lights = [("#346ED6", .55, u * .18, h * .06, u * .95), ("#1696A0", .30, w, h * .42, u * .70), ("#3C5AC8", .45, w * .40, h * 1.02, u * 1.05)]
    defs = "".join(f'<radialGradient id="l{i}" gradientUnits="userSpaceOnUse" cx="{x:.0f}" cy="{y:.0f}" r="{r:.0f}">'
                   f'<stop offset="0" stop-color="{c}" stop-opacity="{a}"/><stop offset="1" stop-color="{c}" stop-opacity="0"/></radialGradient>'
                   for i, (c, a, x, y, r) in enumerate(lights))
    rects = f'<rect width="{w}" height="{h}" fill="{BASE}"/>' + "".join(f'<rect width="{w}" height="{h}" fill="url(#l{i})"/>' for i in range(3))
    return defs, rects


def build(w, h, corner, L, animated=False):
    """L: the layout for this size (positions in px). [animated]: the Carga Express loop (motion.py)."""
    defs, back = ambient(w, h)
    if animated:
        defs += ('<radialGradient id="halo"><stop offset="0" stop-color="#E9FFFE" stop-opacity=".9"/><stop offset="1" stop-color="#9EE3E0" stop-opacity="0"/></radialGradient>'
                 '<filter id="soft" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="6"/></filter>'
                 '<radialGradient id="glowHead"><stop offset="0" stop-color="#EAFFFE"/><stop offset=".35" stop-color="#CDFFFC" stop-opacity=".55"/><stop offset="1" stop-color="#CDFFFC" stop-opacity="0"/></radialGradient>'
                 '<radialGradient id="glowTop"><stop offset="0" stop-color="#E1FFFE"/><stop offset="1" stop-color="#E1FFFE" stop-opacity="0"/></radialGradient>'
                 '<linearGradient id="beam" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#9EE3E0" stop-opacity="0"/><stop offset="1" stop-color="#EAFFFE"/></linearGradient>'
                 '<linearGradient id="fadeV" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".14" stop-color="#fff"/>'
                 '<stop offset=".9" stop-color="#fff"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>')
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}" fill="none" role="img" aria-labelledby="t d">',
         '<title id="t">Boletera</title><desc id="d">App Android no oficial para consultar el saldo y recargar la tarjeta STM.</desc>',
         f'<defs>{defs}<clipPath id="c"><rect width="{w}" height="{h}" rx="{corner}"/></clipPath></defs><g clip-path="url(#c)">', back]
    a = o.append
    # brand, and what it is
    a(mark(L["mark"][0], L["mark"][1], L["mark"][2]))
    a(T("Boletera", MED, L["brand"][2], L["brand"][0], L["brand"][1], ON)[0])
    a(T("Tu STM, más simple.", REG, L["tag"][2], L["tag"][0], L["tag"][1], FAINT, anchor="end")[0])
    # the balance, exactly as the home shows it: «$» small and normal, the number Medium and tight
    a(T("Saldo", REG, L["label"][2], L["label"][0], L["label"][1], MUTED)[0])
    a(T("$", REG, L["dollar"][2], L["dollar"][0], L["dollar"][1], FAINT)[0])
    nx, ny, ns = L["number"]
    new_number, new_w = T("1.240", MED, ns, nx, ny, ON, tracking=-30)
    x, y, size = L["trips"]
    p1, w1 = T("Te alcanza para ", REG, size, x, y, ON)
    n_new, w_new = T("23", REG, size, x + w1, y, PRIMARY)
    n_old, w_old = T("18", REG, size, x + w1, y, PRIMARY)
    s2, ws2 = T(" viajes", REG, size, x + w1 + w_new, y, PRIMARY)
    s3, _ = T(" de 1 hora", REG, size, x + w1 + w_new + ws2, y, ON)
    if not animated:
        a(new_number); a(p1 + n_new + s2 + s3)
    else:
        # Both balances (and both trip counts) roll inside a soft window, like LabRollingNumber.
        a(f'<mask id="nm" maskUnits="userSpaceOnUse" x="0" y="{ny - ns * .86}" width="{w}" height="{ns * .96}">'
          f'<rect x="0" y="{ny - ns * .86}" width="{w}" height="{ns * .96}" fill="url(#fadeV)"/></mask>')
        a(f'<mask id="tm" maskUnits="userSpaceOnUse" x="0" y="{y - size * 1.05}" width="{w}" height="{size * 1.4}">'
          f'<rect x="0" y="{y - size * 1.05}" width="{w}" height="{size * 1.4}" fill="url(#fadeV)"/></mask>')
        old_number, old_w = T("980", MED, ns, nx, ny, ON, tracking=-30)
        a(f'<g mask="url(#nm)"><g class="n-old">{old_number}</g><g class="n-new">{new_number}</g></g>')
        a(p1 + f'<g mask="url(#tm)"><g class="t-old">{n_old}</g><g class="t-new">{n_new}</g></g><g class="t-suf">{s2 + s3}</g>')
    # the bar: the glass ring, the Recargar pill and the bolt
    bx, by, bw, bh = L["bar"]
    a(f'<rect x="{bx}" y="{by}" width="{bw}" height="{bh}" rx="{bh / 2}" fill="#fff" fill-opacity=".075" stroke="#fff" stroke-opacity=".09"/>')
    ins = bh * .077
    a(f'<rect x="{bx + ins:.1f}" y="{by + ins:.1f}" width="{bw - 2 * ins:.1f}" height="{bh - 2 * ins:.1f}" rx="{(bh - 2 * ins) / 2:.1f}" fill="{PRIMARY}"/>')
    ls = bh * .29
    label, lw = T("Recargar", MED, ls, 0, 0, ON_PRIMARY)
    gap, icon = ls * .45, ls * 1.05
    start = bx + bw / 2 - (icon + gap + lw) / 2
    a(f'<path d="{ADD}" fill="{ON_PRIMARY}" transform="translate({start:.1f} {by + bh / 2 - icon / 2:.1f}) scale({icon / 24:.4f})"/>')
    a(T("Recargar", MED, ls, start + icon + gap, by + bh / 2 + ls * .36, ON_PRIMARY)[0])
    if not animated:
        a(bolt_button(L["bolt"][0], L["bolt"][1], L["bolt"][2]))
    else:
        a(animated_bolt(*L["bolt"]))
        # The light falls on the number on screen at that moment (980), right in its middle.
        land = (nx + old_w / 2, ny - ns * .36)
        css, light = express_light(w, h, corner, L["bolt"], land, (nx, ny, ns), (y, size), w_old - w_new)
        o.insert(3, f'<style>{css}</style>')
        o[3] += ""
        a(light)
    a("</g>")
    if corner:
        a(f'<rect x=".5" y=".5" width="{w - 1}" height="{h - 1}" rx="{corner - .5}" stroke="#fff" stroke-opacity=".08"/>')
    a("</svg>")
    return "\n".join(o) + "\n"


def animated_bolt(cx, cy, size):
    r = size / 2 * .98; base = r / 1.07
    pts = [f"{cx + base * (1 + .07 * math.cos(9 * t)) * math.cos(t - math.pi / 2):.1f},{cy + base * (1 + .07 * math.cos(9 * t)) * math.sin(t - math.pi / 2):.1f}"
           for t in [i / 180 * 2 * math.pi for i in range(181)]]
    s = size / 76
    return (f'<g class="b-move"><circle class="b-halo" cx="{cx}" cy="{cy}" r="{size * .82:.1f}" fill="url(#halo)"/>'
            f'<g class="b-spin"><path class="b-fill" d="M{" L".join(pts)}Z" fill="{PRIMARY}"/></g>'
            f'<path d="{BOLT}" fill="{ON_PRIMARY}" transform="translate({cx - 14 * s:.1f} {cy - 14 * s:.1f}) scale({28 * s / 24:.4f})"/></g>')


def edge_halves(w, h, r, bolt, meet_x):
    """LabExpressLight.half(): from under the bolt down to the edge, then along it both ways. They meet on the top edge
    right above the balance ([meet_x]), so the light then drops straight into it (in the app the card is centred)."""
    inset = 2.0; bx = bolt[0]; by = bolt[1] + 38 * bolt[2] / 76
    right = (f"M{bx},{by} L{bx},{h - inset} L{w - r},{h - inset} Q{w - inset},{h - inset} {w - inset},{h - r} L{w - inset},{r} "
             f"Q{w - inset},{inset} {w - r},{inset} L{meet_x:.1f},{inset}")
    left = (f"M{bx},{by} L{bx},{h - inset} L{r},{h - inset} Q{inset},{h - inset} {inset},{h - r} L{inset},{r} "
            f"Q{inset},{inset} {r},{inset} L{meet_x:.1f},{inset}")
    corners = 2 * (math.pi / 2 * (r - inset))
    down, side = h - inset - by, h - 2 * r
    return [(right, down + (w - r - bx) + side + (w - r - meet_x) + corners),
            (left, down + (bx - r) + side + (meet_x - r) + corners)]


def express_light(w, h, corner, bolt, land, number, trips, trips_shift):
    """CSS keyframes for the bolt, the light, the drop and the two rolls, sampled from motion.py."""
    ts = M.times()
    B = M.Bolt(bolt[2] / 76)
    cx, cy = bolt[0], bolt[1]
    k = 1.45  # the light's dp → px here
    halves = edge_halves(w, h, corner, bolt, land[0])
    tail = 170 * k
    css = [f".b-move{{transform-box:view-box;transform-origin:{cx}px {cy}px;animation:bMove 10s linear infinite}}",
           f".b-spin{{transform-box:view-box;transform-origin:{cx}px {cy}px;animation:bSpin 10s linear infinite}}",
           ".b-fill{animation:bFill 10s linear infinite}.b-halo{opacity:0;animation:bHalo 10s linear infinite}"]
    bs = [(t, B.state(t)) for t in ts]
    css.append(M.keyframes("bMove", bs, lambda v: f"transform:translate({v['tx']:.2f}px,{v['ty']:.2f}px) scale({v['press']:.4f})"))
    css.append(M.keyframes("bSpin", bs, lambda v: f"transform:rotate({v['angle']:.2f}deg)"))
    css.append(M.keyframes("bFill", bs, lambda v: f"fill:{v['fill']}"))
    css.append(M.keyframes("bHalo", bs, lambda v: f"opacity:{v['halo']:.3f}"))
    out = []
    # the two runs along the edge (a glow and a bright core), each with a light at its head
    for i, (d, L) in enumerate(halves):
        run = [(t, M.ease_in_out(M.seg(t - M.FIRE_AT, M.RUN_START, M.MEET)) * L) for t in ts]
        fade = lambda t: (1 - M.seg(t - M.FIRE_AT, M.MEET, M.MEET + .36)) if t - M.FIRE_AT >= M.RUN_START else 0
        css.append(M.keyframes(f"run{i}", run, lambda v: f"stroke-dashoffset:{tail - v:.1f}"))
        css.append(M.keyframes(f"fade{i}", [(t, fade(t)) for t in ts], lambda v: f"opacity:{v:.3f}"))
        css.append(M.keyframes(f"head{i}", [(t, (v / L * 100, 1 if 0 < t - M.FIRE_AT < M.MEET else 0)) for t, v in run],
                               lambda v: f"offset-distance:{v[0]:.2f}%;opacity:{v[1]:.2f}"))
        css.append(f".run{i}{{stroke-dasharray:{tail:.0f} {L + tail:.0f};stroke-dashoffset:{tail:.0f};opacity:0;animation:run{i} 10s linear infinite,fade{i} 10s linear infinite}}")
        css.append(f".head{i}{{offset-path:path('{d}');offset-rotate:0deg;opacity:0;animation:head{i} 10s linear infinite}}")
        out.append(f'<path class="run{i}" d="{d}" stroke="#9EE3E0" stroke-opacity=".38" stroke-width="{26 * k:.0f}" stroke-linecap="round" stroke-linejoin="round" filter="url(#soft)"/>'
                   f'<path class="run{i}" d="{d}" stroke="#EAFFFE" stroke-width="{5 * k:.1f}" stroke-linecap="round" stroke-linejoin="round"/>'
                   f'<circle class="head{i}" r="{46 * k:.0f}" fill="url(#glowHead)"/>')
    # they meet at the top: a thin flare and the whole edge breathing once
    meet = lambda t: M.seg(t - M.FIRE_AT, M.MEET - .02, M.MEET + .38) if M.MEET - .02 < t - M.FIRE_AT < M.MEET + .4 else (1 if t - M.FIRE_AT >= M.MEET + .4 else 0)
    flare = [(t, meet(t)) for t in ts]
    css.append(M.keyframes("flare", flare, lambda q: f"opacity:{(1 - q) if 0 < q < 1 else 0:.3f};transform:translate({land[0]:.1f}px,2px) scale({(80 + 220 * M.ease_out3(q)) * k:.1f},{(80 + 220 * M.ease_out3(q)) * k * .1:.2f})"))
    css.append(M.keyframes("spot", flare, lambda q: f"opacity:{(1 - q) if 0 < q < 1 else 0:.3f};transform:translate({land[0]:.1f}px,2px) scale({(24 + 36 * q) * k:.1f})"))
    css.append(M.keyframes("rim", flare, lambda q: f"opacity:{.32 * (1 - q) if 0 < q < 1 else 0:.3f}"))
    css.append(".flare{transform-box:view-box;transform-origin:0 0;opacity:0;animation:flare 10s linear infinite}"
               ".spot{transform-box:view-box;transform-origin:0 0;opacity:0;animation:spot 10s linear infinite}"
               ".rim{opacity:0;animation:rim 10s linear infinite}")
    out.append(f'<circle class="flare" r="1" fill="url(#glowTop)"/><circle class="spot" r="1" fill="url(#glowTop)"/>'
               f'<rect class="rim" x="2" y="2" width="{w - 4}" height="{h - 4}" rx="{corner - 2}" stroke="#9EE3E0" stroke-width="{5 * k:.1f}"/>')
    # the energy drops into the balance
    def drop(t):
        s = t - M.FIRE_AT
        if not (M.MEET < s < M.HIT + .12): return (land[0], 2, 0)
        q = M.seg(s, M.MEET, M.HIT) ** 3
        return (land[0], 2 + (land[1] - 2) * q, 1 - M.seg(s, M.HIT, M.HIT + .12))   # straight down
    css.append(M.keyframes("drop", [(t, drop(t)) for t in ts], lambda v: f"opacity:{v[2]:.3f};transform:translate({v[0]:.1f}px,{v[1]:.1f}px)"))
    css.append(".drop{transform-box:view-box;transform-origin:0 0;opacity:0;animation:drop 10s linear infinite}")
    out.append(f'<g class="drop"><rect x="-1.5" y="{-140 * k:.0f}" width="3" height="{140 * k:.0f}" fill="url(#beam)"/>'
               f'<circle r="{26 * k:.0f}" fill="url(#glowHead)"/></g>')
    hit = lambda t: M.seg(t - M.FIRE_AT, M.HIT - .01, M.HIT + .3) if t - M.FIRE_AT > M.HIT - .01 else 0
    css.append(M.keyframes("hit", [(t, hit(t)) for t in ts], lambda q: f"opacity:{.85 * (1 - q) if 0 < q < 1 else 0:.3f};transform:translate({land[0]:.0f}px,{land[1]:.0f}px) scale({(20 + 34 * M.ease_out3(q)) * k * 1.6:.1f})"))
    css.append(".hit{transform-box:view-box;transform-origin:0 0;opacity:0;animation:hit 10s linear infinite}")
    out.append('<circle class="hit" r="1" fill="url(#glowTop)"/>')
    # the rolls: the old value leaves upward, the new one springs in from below; the loop ends with a soft fade back
    def swap(t, at, dist, sign):
        """One value leaves (accelerating, towards -sign) and the other arrives from +sign with a soft spring once it is out."""
        out_ = M.seg(t, at, at + .2); out_ = out_ * out_
        late = at + .14; in_ = M.spring(t - late, zeta=.7, w=15)
        leaving = (-sign * dist * out_, 1 - M.clamp(out_ * 1.4))
        arriving = (sign * dist * (1 - in_), M.clamp((t - late) / .12) if t > late else 0)
        return leaving, arriving, M.clamp(in_)
    def roll(t, at, dist):
        """((old y, old alpha), (new y, new alpha), how much of the new value is in)."""
        back_at = M.RESET_AT + (at - M.ROLL_AT)
        if t >= back_at:                         # rewind: 1.240 drops out downwards, 980 falls in from above
            leaving, arriving, k = swap(t, back_at, dist, -1)
            return arriving, leaving, 1 - k
        if t < at: return (0, 1), (dist, 0), 0
        leaving, arriving, k = swap(t, at, dist, 1)
        return leaving, arriving, k
    for cls, at, dist in [("n", M.ROLL_AT, number[2] * .55), ("t", M.TRIPS_AT, trips[1] * .9)]:
        rs = [(t, roll(t, at, dist)) for t in ts]
        if cls == "t":   # «viajes de 1 hora» follows the width of the number in front of it
            css.append(M.keyframes("tSuf", rs, lambda v: f"transform:translateX({trips_shift * (1 - v[2]):.2f}px)"))
            css.append(".t-suf{animation:tSuf 10s linear infinite}")
        # fill-opacity, not opacity: Chrome ignores a group's animated opacity inside a mask (it painted both values at once).
        css.append(M.keyframes(f"{cls}Old", rs, lambda v: f"transform:translateY({v[0][0]:.1f}px);fill-opacity:{v[0][1]:.3f}"))
        css.append(M.keyframes(f"{cls}New", rs, lambda v: f"transform:translateY({v[1][0]:.1f}px);fill-opacity:{v[1][1]:.3f}"))
        css.append(f".{cls}-old{{fill-opacity:0;animation:{cls}Old 10s linear infinite}}.{cls}-new{{animation:{cls}New 10s linear infinite}}")
    # «Reduce motion»: everything still, on the final state (the base styles above)
    css.append("@media (prefers-reduced-motion: reduce){*{animation:none!important}}")
    return "".join(css), "".join(out)


README = dict(mark=(150, 96, 1.55), brand=(208, 108, 34), tag=(1480, 108, 22), label=(120, 232, 30), dollar=(112, 300, 56),
              number=(162, 414, 210), trips=(124, 482, 32), bar=(938, 396, 452, 104), bolt=(1468, 448, 104))
SOCIAL = dict(mark=(110, 96, 1.4), brand=(162, 107, 30), tag=(1196, 106, 20), label=(84, 268, 28), dollar=(76, 330, 52),
              number=(122, 448, 200), trips=(88, 520, 30), bar=(650, 496, 420, 96), bolt=(1148, 544, 96))

if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[2] / "docs/media"
    (out / "boletera-banner.svg").write_text(build(1600, 560, 40, README, animated=True), encoding="utf-8")
    (out / "boletera-social.svg").write_text(build(1280, 640, 0, SOCIAL), encoding="utf-8")
