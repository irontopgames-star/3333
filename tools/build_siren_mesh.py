#!/usr/bin/env python3
"""Convierte la malla de Siren Head (.bbmodel de Blockbench, tools/source/siren.bbmodel) en los archivos que usa el mod:
  assets/weraisedsirenhead/models/siren_head.bin   (malla + esqueleto + pesos de piel)
  assets/weraisedsirenhead/textures/entity/siren_head.png
El esqueleto (15 huesos) y los pesos se calculan aca; las animaciones estan en SirenPose.java.
Uso: python3 build_siren_mesh.py [ruta.bbmodel] [--preview]"""
import base64, io, json, math, os, struct, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = next((a for a in sys.argv[1:] if a.endswith(".bbmodel")), os.path.join(HERE, "source", "siren.bbmodel"))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "weraisedsirenhead")

# --- huesos (el orden debe coincidir con SirenBones.java)
BONES = ["root", "hips", "spine", "neck", "head", "speakerA", "speakerB",
         "armL_up", "armL_low", "armR_up", "armR_low", "legL_up", "legL_low", "legR_up", "legR_low"]
PARENT = {"root": None, "hips": "root", "spine": "hips", "neck": "spine", "head": "neck", "speakerA": "head", "speakerB": "head",
          "armL_up": "spine", "armL_low": "armL_up", "armR_up": "spine", "armR_low": "armR_up",
          "legL_up": "hips", "legL_low": "legL_up", "legR_up": "hips", "legR_low": "legR_up"}
# pivotes (articulaciones) en unidades de Blockbench, con los pies en y=0
PIVOT = {"root": (0, 0, 0), "hips": (0.0, 15.8, 0.3), "spine": (0.0, 17.5, 0.3), "neck": (0.1, 24.3, 0.3), "head": (0.1, 26.2, 0.3),
         "speakerA": (-0.8, 27.6, 0.3), "speakerB": (1.4, 27.6, 0.3),
         "armL_up": (-3.3, 22.6, 0.3), "armL_low": (-4.2, 13.0, -0.4), "armR_up": (3.3, 22.6, 0.3), "armR_low": (4.1, 13.0, -0.4),
         "legL_up": (-1.5, 15.2, 0.3), "legL_low": (-2.2, 8.0, -0.4), "legR_up": (1.4, 15.2, 0.3), "legR_low": (2.2, 8.0, -0.4)}
# segmentos (inicio, fin) de cada hueso para calcular pesos por distancia
SEG = {"hips": ((0, 15.4, 0.3), (0, 17.3, 0.3)), "spine": ((0, 17.3, 0.3), (0, 24.0, 0.3)), "neck": ((0.1, 24.0, 0.3), (0.1, 26.0, 0.3)),
       "head": ((0.1, 26.0, 0.3), (0.1, 26.8, 0.3)),
       "armL_up": ((-3.3, 22.6, 0.3), (-4.2, 13.0, -0.4)), "armL_low": ((-4.2, 13.0, -0.4), (-5.1, 3.0, -1.0)),
       "armR_up": ((3.3, 22.6, 0.3), (4.1, 13.0, -0.4)), "armR_low": ((4.1, 13.0, -0.4), (5.0, 3.0, -1.0)),
       "legL_up": ((-1.5, 15.2, 0.3), (-2.2, 8.0, -0.4)), "legL_low": ((-2.2, 8.0, -0.4), (-2.9, 0.5, -0.2)),
       "legR_up": ((1.4, 15.2, 0.3), (2.2, 8.0, -0.4)), "legR_low": ((2.2, 8.0, -0.4), (3.0, 0.5, -0.2))}
IDX = {n: i for i, n in enumerate(BONES)}


def seg_dist(p, a, b):
    a = np.array(a, float); b = np.array(b, float); ab = b - a
    t = np.clip(np.dot(p - a, ab) / np.dot(ab, ab), 0, 1)
    return float(np.linalg.norm(p - (a + t * ab)))


def candidates(p):
    x, y = p[0] - 0.1, p[1]
    c = []
    if y > 26.3:
        return [("speakerA" if p[0] < 0.1 else "speakerB")]
    if y > 22.0: c += ["neck"] if y > 23.0 else []
    if 25.0 > y > 16.4: c += ["spine"]
    if 18.2 > y > 14.0: c += ["hips"]
    if 27.0 > y > 23.0: c += ["neck"]
    if y > 25.2: c += ["head"]
    if abs(x) > 2.4 and y < 25.0:
        side = "L" if p[0] < 0 else "R"
        c += ["arm%s_up" % side, "arm%s_low" % side]
    if y < 17.5:
        side = "L" if p[0] < 0.1 else "R"
        c += ["leg%s_up" % side, "leg%s_low" % side]
    return list(dict.fromkeys(c)) or ["spine"]


def skin(P):
    n = len(P); b0 = np.zeros(n, np.uint8); b1 = np.zeros(n, np.uint8); w0 = np.ones(n, np.float32)
    for i, p in enumerate(P):
        cand = candidates(p)
        if len(cand) == 1 and cand[0].startswith("speaker"):
            b0[i] = b1[i] = IDX[cand[0]]; continue
        ds = sorted((seg_dist(p, *SEG[c]), c) for c in cand)[:2]
        ws = [1.0 / (d + 0.2) ** 4 for d, _ in ds]
        s = sum(ws); ws = [w / s for w in ws]
        if len(ds) == 1 or ws[1] < 0.1:
            b0[i] = b1[i] = IDX[ds[0][1]]; w0[i] = 1.0
        else:
            b0[i] = IDX[ds[0][1]]; b1[i] = IDX[ds[1][1]]; w0[i] = ws[0]
    return b0, b1, w0


def load():
    d = json.load(open(SRC))
    m = d["elements"][0]
    ids = list(m["vertices"].keys()); index = {k: i for i, k in enumerate(ids)}
    P = np.array([m["vertices"][k] for k in ids], float) + np.array(m["origin"], float)
    ymin = P[:, 1].min(); P[:, 1] -= ymin
    tex = Image.open(io.BytesIO(base64.b64decode(d["textures"][0]["source"].split(",", 1)[1]))).convert("RGBA")
    res = d.get("resolution", {"width": 16, "height": 16})
    faces = []
    for f in m["faces"].values():
        vs = f["vertices"]
        if len(vs) == 3:
            vs = vs + [vs[2]]
        uv = [(f["uv"][k][0] / res["width"], f["uv"][k][1] / res["height"]) for k in vs]
        faces.append(([index[k] for k in vs], uv))
    return P, faces, tex


# ---------- matematicas de pose (iguales a SirenPose/SirenHeadRenderer en Java)
def T(x, y, z):
    M = np.eye(4); M[:3, 3] = (x, y, z); return M
def Rx(a): c, s = math.cos(a), math.sin(a); return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]], float)
def Ry(a): c, s = math.cos(a), math.sin(a); return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]], float)
def Rz(a): c, s = math.cos(a), math.sin(a); return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], float)

def skin_matrices(rot, offset=(0, 0, 0)):
    G = {}; S = []
    for name in BONES:
        px, py, pz = PIVOT[name]; par = PARENT[name]
        rx, ry, rz = [math.radians(v) for v in rot.get(name, (0, 0, 0))]
        if par is None:
            g = T(*offset) @ T(px, py, pz)
        else:
            qx, qy, qz = PIVOT[par]; g = G[par] @ T(px - qx, py - qy, pz - qz)
        g = g @ Rz(rz) @ Ry(ry) @ Rx(rx)
        G[name] = g; S.append(g @ T(-px, -py, -pz))
    return S


def preview(P, faces, tex, b0, b1, w0, poses, path):
    W, H = tex.size; cols = []
    for vs, uv in faces:
        u, v = np.mean(uv, axis=0); cols.append(tex.getpixel((min(W - 1, int(u * W)), min(H - 1, int(v * H))))[:3])
    from PIL import ImageDraw
    S_ = 14; tiles = []
    for rot in poses:
        S = skin_matrices(rot); Ph = np.hstack([P, np.ones((len(P), 1))])
        out = np.zeros_like(P)
        for i in range(len(P)):
            out[i] = (w0[i] * (S[b0[i]] @ Ph[i]) + (1 - w0[i]) * (S[b1[i]] @ Ph[i]))[:3]
        for view in (0, 2):
            h = 0 if view == 0 else 2
            lo, hi = out[:, h].min() - 3, out[:, h].max() + 3
            ymax = out[:, 1].max() + 1
            im = Image.new("RGB", (int((hi - lo) * S_), int(ymax * S_)), (35, 35, 35)); dr = ImageDraw.Draw(im)
            depth = out[:, 2 if view == 0 else 0]
            order = sorted(range(len(faces)), key=lambda k: -np.mean([depth[j] for j in faces[k][0]]) * (1 if view == 0 else -1))
            for k in order:
                pts = [((out[j][h] - lo) * S_, im.height - out[j][1] * S_) for j in faces[k][0]]
                dr.polygon(pts, fill=cols[k])
            tiles.append(im)
    Wt = sum(t.width for t in tiles) + 10 * len(tiles); Ht = max(t.height for t in tiles)
    sheet = Image.new("RGB", (Wt, Ht), (35, 35, 35)); x = 0
    for t in tiles:
        sheet.paste(t, (x, 0)); x += t.width + 10
    sheet.save(path); print("preview", path, sheet.size)


def export(P, faces, tex, b0, b1, w0):
    cx = (P[:, 0].min() + P[:, 0].max()) / 2; cz = (P[:, 2].min() + P[:, 2].max()) / 2
    shift = np.array([cx, 0, cz])
    Pb = (P - shift) / 16.0
    os.makedirs(os.path.join(ASSETS, "models"), exist_ok=True); os.makedirs(os.path.join(ASSETS, "textures", "entity"), exist_ok=True)
    with open(os.path.join(ASSETS, "models", "siren_head.bin"), "wb") as f:
        f.write(struct.pack(">iiii", 0x53495245, len(P), len(faces), len(BONES)))
        for n in BONES:
            par = PARENT[n]; px, py, pz = (np.array(PIVOT[n], float) - shift) / 16.0
            name = n.encode("utf-8"); f.write(struct.pack(">H", len(name)) + name)
            f.write(struct.pack(">ifff", -1 if par is None else IDX[par], px, py, pz))
        for i in range(len(P)):
            f.write(struct.pack(">fffBBf", Pb[i][0], Pb[i][1], Pb[i][2], int(b0[i]), int(b1[i]), float(w0[i])))
        for vs, uv in faces:
            f.write(struct.pack(">HHHH", *vs))
            for u, v in uv:
                f.write(struct.pack(">ff", u, v))
    tex.save(os.path.join(ASSETS, "textures", "entity", "siren_head.png"), optimize=True)
    print("alto en bloques: %.3f" % (Pb[:, 1].max()), "| vertices", len(P), "| caras", len(faces))


if __name__ == "__main__":
    P, faces, tex = load()
    b0, b1, w0 = skin(P)
    if "--preview" in sys.argv:
        poses = [{},
                 {"legL_up": (35, 0, 0), "legL_low": (-45, 0, 0), "armR_up": (-30, 0, 0), "armR_low": (20, 0, 0), "head": (0, 35, 0), "spine": (0, 0, 6), "speakerA": (0, 25, 0)},
                 {"spine": (-28, 0, 0), "neck": (-45, 0, 0), "armL_up": (60, 0, 0), "armL_low": (45, 0, 0), "armR_up": (140, 0, 0), "legR_up": (-25, 0, 0), "legR_low": (50, 0, 0)}]
        preview(P, faces, tex, b0, b1, w0, poses, "/tmp/rig_preview.png")
    else:
        export(P, faces, tex, b0, b1, w0)
