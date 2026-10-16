"""The banner's motion: the app's Carga Express moment, told in one 10-second loop.

Rest on «$ 980» → the bolt charges (spins faster, sinks, lightens, trembles with the tics) → it fires → the light runs
the banner's edge both ways, meets at the top and drops into the balance → the balance rolls up to «$ 1.240» and
«18 viajes» becomes «23 viajes» → rest → the same roll, rewound, back to the start.

The curves are the app's own (LabHome.LabExpressButton and LabExpressLight.kt), sampled into CSS keyframes every
10 ms where things move fast and every 100 ms where they do not, played linearly between samples. With «reduce
motion» the banner shows the final state, still.
"""
import math

LOOP = 10.0
HOLD = 1.2                              # EXPRESS_HOLD_MILLIS
CHARGE_AT = 2.0                         # the hold starts
FIRE_AT = CHARGE_AT + HOLD              # the bolt fires
TICKS = [0, .38, .64, .82, .95, 1.04, 1.105, 1.15]   # ExpressTicks, in seconds of the hold
RUN_START, MEET, HIT, END = .08, .5, .61, 1.1        # LabExpressLight, seconds after the shot
ROLL_AT, ROLL = FIRE_AT + HIT, .75      # the balance rolls once the light lands
TRIPS_AT = ROLL_AT + .12
RESET_AT, RESET = 9.25, .5              # the rewind back to «$ 980»


def clamp(x, a=0.0, b=1.0): return max(a, min(b, x))
def seg(t, a, b): return clamp((t - a) / (b - a))
def ease_in_out(t): return 2 * t * t if t < .5 else 1 - (-2 * t + 2) ** 2 / 2
def ease_out3(t): return 1 - (1 - t) ** 3


def spring(t, zeta=.62, w=14.0):
    """Unit step response of a damped spring (the app's springs feel), t in seconds."""
    if t <= 0: return 0.0
    wd = w * math.sqrt(1 - zeta * zeta)
    return 1 - math.exp(-zeta * w * t) * (math.cos(wd * t) + zeta * w / wd * math.sin(wd * t))


def times():
    """Sample times: 10 ms while something moves fast, 100 ms otherwise."""
    out, t = [], 0.0
    fast = [(CHARGE_AT - .05, FIRE_AT + END + .1), (ROLL_AT - .05, TRIPS_AT + ROLL + .1), (RESET_AT - .05, RESET_AT + RESET + .05)]
    while t < LOOP - 1e-9:
        out.append(round(t, 3))
        t += .01 if any(a <= t <= b for a, b in fast) else .1
    out.append(LOOP)
    return out


def mix_color(a, b, k):
    pa = [int(a[i:i + 2], 16) for i in (1, 3, 5)]; pb = [int(b[i:i + 2], 16) for i in (1, 3, 5)]
    return "#" + "".join(f"{round(x + (y - x) * k):02x}" for x, y in zip(pa, pb))


class Bolt:
    """LabExpressButton's clock: charge, spin, tremor, press and the white flash, integrated over the loop."""
    def __init__(self, scale):
        self.scale = scale  # px per dp
        self.angle = {}
        a, last = 0.0, 0.0
        for t in [i / 1000 for i in range(int(LOOP * 1000) + 1)]:
            a += (t - last) * 1000 * self.speed(t); last = t
            self.angle[round(t, 3)] = a

    def charge(self, t):
        if t < CHARGE_AT: return 0.0
        if t < FIRE_AT: return (t - CHARGE_AT) / HOLD
        return 1 - ease_in_out(seg(t, FIRE_AT + .3, FIRE_AT + .9))      # after the shot it settles back

    def shot(self, t): return t - FIRE_AT if FIRE_AT <= t <= FIRE_AT + 1.2 else -1

    def speed(self, t):  # degrees per millisecond, as in the app
        s, c = self.shot(t), self.charge(t)
        if s >= 0: return .85 * (1 - min(s / .6, 1)) ** 3 + .006
        return .006 + (.85 - .006) * c * c

    def state(self, t):
        c, s, ms = self.charge(t), self.shot(t), t * 1000
        charging = CHARGE_AT <= t < FIRE_AT
        amp = 1.7 * c ** 1.6 if charging else 0
        kick = 0.0
        if charging:
            for tick in TICKS:
                j = (t - CHARGE_AT - tick) * 1000
                if 0 <= j < 70: kick = math.sin(j / 70 * 2 * math.pi) * 1.3 * (1 - j / 70)
        if 0 <= s < .14: tx = math.sin(s / .14 * 4 * math.pi) * 2.2 * (1 - s / .14)
        else: tx = amp * (math.sin(ms * .138) + .6 * math.sin(ms * .211 + 1.3)) / 1.6 + kick
        ty = 0 if s >= 0 else amp * .5 * math.sin(ms * .173 + .7)
        if 0 <= s < .09: press = .9 + .22 * ease_out3(s / .09)
        elif s >= .09: press = 1.12 - .12 * ease_out3(clamp((s - .09) / .33))
        else: press = 1 - .1 * ease_out3(c)
        white = (.75 + .25 * s / .05 if s < .05 else 1 - clamp((s - .16) / .32)) if s >= 0 else 0
        fill = mix_color(mix_color("#B3D0FF", "#E2F3FF", c), "#EFFFFE", white)
        halo = .75 * white * (1 - clamp((s - .28) / .28)) if s >= 0 else .42 * c * c
        return dict(angle=self.angle[round(t, 3)] % 360, tx=tx * self.scale, ty=ty * self.scale, press=press, fill=fill, halo=halo)


def keyframes(name, samples, render):
    """samples: [(t, value)], render(value) -> CSS declarations. Repeated values are dropped (linear play keeps it exact)."""
    lines, prev, pending = [], None, None
    for i, (t, v) in enumerate(samples):
        css = render(v)
        if css == prev and i < len(samples) - 1:
            pending = (t, css); continue
        if pending: lines.append(f"{pending[0] / LOOP * 100:.3f}%{{{pending[1]}}}"); pending = None
        lines.append(f"{t / LOOP * 100:.3f}%{{{css}}}"); prev = css
    return f"@keyframes {name}{{{''.join(lines)}}}"
