#!/usr/bin/env python3
"""Sintetiza todos los sonidos del mod (mono, .ogg Vorbis) con numpy/scipy + ffmpeg.
Son sonidos propios generados por codigo: podes reemplazar cualquier .ogg en
src/main/resources/assets/weraisedsirenhead/sounds/ manteniendo el nombre y sin tocar nada mas."""
import os, subprocess, tempfile
import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "weraisedsirenhead", "sounds")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(2024)

def t(sec): return np.arange(int(SR * sec)) / SR
def noise(sec): return rng.uniform(-1, 1, int(SR * sec))
def bp(x, lo, hi, order=4):
    sos = signal.butter(order, [lo, hi], btype="band", fs=SR, output="sos"); return signal.sosfilt(sos, x)
def lp(x, f, order=4):
    sos = signal.butter(order, f, btype="low", fs=SR, output="sos"); return signal.sosfilt(sos, x)
def hp(x, f, order=4):
    sos = signal.butter(order, f, btype="high", fs=SR, output="sos"); return signal.sosfilt(sos, x)
def env_ad(n, a, d):
    e = np.ones(n); na = max(1, int(a * SR)); e[:na] = np.linspace(0, 1, na)
    nd = max(1, min(n, int(d * SR))); e[-nd:] *= np.linspace(1, 0, nd); return e
def expdecay(n, tau): return np.exp(-np.arange(n) / (tau * SR))
def dist(x, g): return np.tanh(g * x) / np.tanh(g)
def crush(x, bits=6, hold=3):
    q = 2 ** bits; y = np.round(x * q) / q; return np.repeat(y[::hold], hold)[:len(x)]
def radio(x, g=2.5):
    y = bp(x, 300, 3400, 3); y = dist(y, g); return y
def echo(x, delay, fb, times=3):
    y = x.copy(); d = int(delay * SR)
    for i in range(1, times + 1):
        pad = np.zeros(len(x)); pad[d * i:] = x[:len(x) - d * i] * (fb ** i); y = y + pad
    return y
def norm(x, peak=0.9):
    m = np.max(np.abs(x)) + 1e-9; return x / m * peak
def fade(x, ms=8):
    n = int(SR * ms / 1000); x = x.copy(); x[:n] *= np.linspace(0, 1, n); x[-n:] *= np.linspace(1, 0, n); return x

def save(name, x, peak=0.9):
    x = fade(norm(x, peak)); pcm = (x * 32767).astype(np.int16)
    with tempfile.TemporaryDirectory() as d:
        w = os.path.join(d, "a.wav"); wavfile.write(w, SR, pcm)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", w, "-ac", "1", "-c:a", "libvorbis", "-q:a", "4",
                        os.path.join(OUT, name + ".ogg")], check=True)
    print("ok", name, round(len(x) / SR, 2), "s")

# ------------------------------------------------------------------ sonidos del bebe
def chirp(seed_f0, dur):
    tt = t(dur); n = len(tt)
    f = seed_f0 * (1 + 1.2 * np.sin(2 * np.pi * 9 * tt) * 0.2) + np.linspace(0, 1100, n)
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = np.sign(np.sin(ph)) * 0.5 + np.sin(ph) * 0.5
    x *= (np.sin(2 * np.pi * 38 * tt) > -0.2)
    x += 0.35 * bp(noise(dur), 2000, 7000)
    return radio(x * env_ad(n, 0.01, 0.15), 3)
save("baby_chirp_1", chirp(900, 0.32), 0.6)
save("baby_chirp_2", chirp(1300, 0.45), 0.6)

# ------------------------------------------------------------------ estatica e interferencias
def static(dur, bursty):
    n = int(SR * dur); x = noise(dur)
    x = hp(x, 1200)
    gate = (lp(rng.uniform(-1, 1, n), 14 if bursty else 3, 2) > (0.02 if bursty else -0.6)).astype(float)
    gate = lp(gate, 200, 2)
    crackle = (rng.uniform(0, 1, n) > 0.9992) * rng.uniform(-1, 1, n) * 4
    return dist(x * (0.25 + 0.75 * gate) + lp(crackle, 6000), 1.8) * env_ad(n, 0.05, 0.25)
save("static_1", static(1.8, True)); save("static_2", static(3.0, False), 0.8)

def interference(dur, base):
    tt = t(dur); n = len(tt)
    f = base + 900 * np.sin(2 * np.pi * 0.7 * tt) + 500 * np.sign(np.sin(2 * np.pi * 3.1 * tt))
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = np.sin(ph) * (0.6 + 0.4 * np.sin(2 * np.pi * 50 * tt)) + 0.4 * bp(noise(dur), 800, 5000)
    x *= (np.sin(2 * np.pi * 5.3 * tt) > -0.5)
    return crush(radio(x, 3), 6, 2) * env_ad(n, 0.1, 0.4)
save("interference_1", interference(2.6, 700)); save("interference_2", interference(3.4, 1100), 0.85)

# ------------------------------------------------------------------ alarmas y sirenas
tt = t(3.2); n = len(tt)
two = np.sin(2 * np.pi * 853 * tt) + np.sin(2 * np.pi * 960 * tt)
gate = ((tt % 0.8) < 0.55).astype(float); gate = lp(gate, 80, 2)
save("alarm", radio(two * gate * 0.5 + 0.05 * noise(3.2), 2.2) * env_ad(n, 0.02, 0.1), 0.9)

dur = 9.0; tt = t(dur); n = len(tt)
sweep = 420 + 360 * (0.5 - 0.5 * np.cos(2 * np.pi * tt / 4.5))
ph = 2 * np.pi * np.cumsum(sweep) / SR
wail = sum(np.sin(k * ph) / k for k in (1, 2, 3, 4, 5)) * 0.4
wail = echo(dist(wail, 2.2), 0.09, 0.5, 3)
wail += 0.05 * lp(noise(dur), 900)
save("air_raid", wail * env_ad(n, 1.2, 1.5), 0.9)

# ------------------------------------------------------------------ voces distorsionadas (sintesis de formantes)
FORMANTS = {"a": (800, 1200, 2600), "i": (300, 2300, 3100), "u": (320, 800, 2300), "e": (500, 1900, 2600), "o": (500, 900, 2400), "n": (250, 1500, 2500)}
def voice(seq, f0_start, f0_end, name, peak=0.9):
    out = []
    tot = sum(d for _, d in seq); n_total = int(SR * tot); pos = 0
    f0 = np.linspace(f0_start, f0_end, n_total) * (1 + 0.025 * np.sin(2 * np.pi * 5.5 * np.arange(n_total) / SR))
    ph = 2 * np.pi * np.cumsum(f0) / SR
    src = sum(np.sin(k * ph) / k ** 0.9 for k in range(1, 18))
    src = src + 0.06 * noise(tot)
    y = np.zeros(n_total)
    for v, d in seq:
        m = int(SR * d)
        seg = src[pos:pos + m]; res = np.zeros(m)
        for f, bw, amp in zip(FORMANTS[v], (90, 110, 160), (1.0, 0.7, 0.4)):
            res += amp * bp(seg, max(60, f - bw), min(SR / 2 - 100, f + bw), 2)
        res *= env_ad(m, 0.03, 0.05)
        y[pos:pos + m] += res; pos += m
    y = y / (np.max(np.abs(y)) + 1e-9)
    y = radio(y, 3.5)
    y = crush(y, 7, 2)
    y = echo(y, 0.13, 0.45, 3) + 0.04 * bp(noise(len(y) / SR), 1000, 4000)
    y *= (1 + 0.35 * np.sin(2 * np.pi * 23 * np.arange(len(y)) / SR))
    save(name, y, peak)
voice([("a", .28), ("i", .12), ("u", .16), ("a", .34)], 190, 150, "voice_1")           # "a-yu-da"
voice([("o", .30), ("n", .10), ("o", .36)], 210, 130, "voice_2")                      # "no..."
voice([("e", .22), ("n", .10), ("i", .14), ("a", .30), ("o", .26)], 170, 140, "voice_3")  # "ven... aqui"

# ------------------------------------------------------------------ pasos, golpes, respiracion
def step(dur, f0, rough):
    n = int(SR * dur); tt = t(dur)
    body = np.sin(2 * np.pi * (f0 * np.exp(-tt * 6) + 22) * tt) * expdecay(n, 0.12)
    thud = lp(noise(dur), 220) * expdecay(n, 0.05) * 3
    crack = bp(noise(dur), 900, 2500) * expdecay(n, 0.012) * rough
    return dist(body * 1.4 + thud + crack, 1.5)
save("step_1", step(0.7, 70, 0.35)); save("step_2", step(0.8, 55, 0.5))

def knock(count, spacing, f):
    dur = spacing * count + 0.3; x = np.zeros(int(SR * dur))
    for i in range(count):
        s = int(i * spacing * SR); m = int(0.22 * SR)
        hit = (bp(noise(0.22), f * 0.6, f * 2.5, 2) * expdecay(m, 0.03) + np.sin(2 * np.pi * f * t(0.22)) * expdecay(m, 0.04) * 0.8)
        x[s:s + m] += hit * (1.0 - 0.1 * i)
    return dist(x, 1.4)
save("knock_1", knock(3, 0.28, 190)); save("knock_2", knock(2, 0.45, 150))

dur = 0.6; n = int(SR * dur)
glass = (np.sin(2 * np.pi * 1850 * t(dur)) + 0.6 * np.sin(2 * np.pi * 2740 * t(dur))) * expdecay(n, 0.09)
glass += bp(noise(dur), 300, 4000) * expdecay(n, 0.03) * 2 + lp(noise(dur), 160) * expdecay(n, 0.06) * 2
save("window_bang", dist(glass, 1.3))

def breath(dur):
    tt = t(dur); n = len(tt); cyc = (tt / (dur / 2)) % 1.0
    amp = np.where(cyc < 0.5, np.sin(np.pi * cyc / 0.5) ** 1.5, 0.55 * np.sin(np.pi * (cyc - 0.5) / 0.5) ** 1.2)
    x = bp(noise(dur), 350, 1800, 2) * amp * (1 + 0.5 * np.sin(2 * np.pi * 31 * tt))
    x += 0.12 * np.sin(2 * np.pi * (3500 + 80 * np.sin(2 * np.pi * 0.4 * tt)) * tt) * amp
    return x * env_ad(n, 0.2, 0.4)
save("breath_1", breath(3.6), 0.8); save("breath_2", breath(4.6), 0.8)

# ------------------------------------------------------------------ transformacion
dur = 9.0; tt = t(dur); n = len(tt)
rise = (tt / dur) ** 2
rumble = lp(noise(dur), 90, 4) * 6 * (0.3 + rise)
cut = 150 + 1800 * rise
grind = np.zeros(n)
blocks = 40
for i in range(blocks):
    a, b = int(i * n / blocks), int((i + 1) * n / blocks)
    grind[a:b] = lp(noise(dur)[a:b], float(cut[a]), 2)
fm = np.sin(2 * np.pi * np.cumsum(80 + 900 * rise ** 1.5) / SR + 3 * np.sin(2 * np.pi * 7 * tt) * rise)
siren = np.sin(2 * np.pi * np.cumsum(300 + 700 * rise) / SR)
x = rumble + 1.4 * grind * rise + 0.5 * fm * rise + 0.4 * siren * (rise > 0.3) * rise
boom_n = int(SR * 2.0); boom = np.zeros(n); k = int(SR * 7.0)
bt = t(2.0); boom[k:k + boom_n] = (np.sin(2 * np.pi * (70 * np.exp(-bt * 2) + 25) * bt) * expdecay(boom_n, 0.4) * 5 + lp(noise(2.0), 400) * expdecay(boom_n, 0.3) * 2)[:n - k]
x = dist(x + boom, 1.6)
x[int(SR * 7.0):] += 0.6 * hp(static(2.0, True), 1500)[:n - int(SR * 7.0)]
save("transform", x * env_ad(n, 0.4, 0.8), 0.95)

# ------------------------------------------------------------------ banquete (masticar y desgarrar, humedo)
def feast(dur, seed):
    rg = np.random.default_rng(seed)
    n = int(SR * dur); x = np.zeros(n)
    t0 = 0.05
    while t0 < dur - 0.3:
        m = int(rg.uniform(0.08, 0.22) * SR); s = int(t0 * SR)
        burst = bp(rg.uniform(-1, 1, m), 150, rg.uniform(900, 2600), 2) * expdecay(m, 0.04) * rg.uniform(0.6, 1.4)
        seg = burst[:max(0, min(m, n - s))]; x[s:s + len(seg)] += seg
        t0 += rg.uniform(0.15, 0.45)
    squelch = lp(rg.uniform(-1, 1, n), 400) * (0.5 + 0.5 * np.sin(2 * np.pi * 3.0 * t(dur))) * 1.2
    x = x + squelch
    for _ in range(int(dur * 2)):
        s = int(rg.uniform(0, dur - 0.1) * SR); m = int(0.05 * SR)
        click = hp(rg.uniform(-1, 1, m), 2000) * expdecay(m, 0.004) * 3.0
        seg = click[:max(0, min(m, n - s))]; x[s:s + len(seg)] += seg
    return dist(x, 1.6) * env_ad(n, 0.05, 0.3)
save("feast_1", feast(3.2, 11)); save("feast_2", feast(4.0, 12))
