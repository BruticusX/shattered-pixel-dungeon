"""Audit (and with --fix, clean) the finished monster sheets:
 - stray bits: pieces of a frame not attached to the body (bleed from a neighbouring pose, motion
   specks) that are small next to the body
 - halos: light, washed-out pixels on the rim just inside the outline, left from the old background
python3 tools/mobs/audit.py [--fix] [sheets...]"""
import sys, re, glob
import numpy as np
from PIL import Image
from scipy import ndimage

FIX = "--fix" in sys.argv
names = [a for a in sys.argv[1:] if not a.startswith("--")]
J = open("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Assets.java").read()
sheets = names or re.findall(r'"(sprites/\w+_hd\.png)"', J)
EIGHT = np.ones((3, 3), bool)

def lum_sat(rgb):
    r, g, b = [rgb[..., i].astype(float) for i in range(3)]
    mx = np.maximum(np.maximum(r, g), b); mn = np.minimum(np.minimum(r, g), b)
    return 0.3 * r + 0.59 * g + 0.11 * b, np.where(mx > 0, (mx - mn) / np.maximum(mx, 1), 0)

def frame_width(path, H):
    # frames are square in a row of 16 per variant: width = height of one row
    return H

report = []
for s in sheets:
    p = "core/src/main/assets/" + s
    try: im = Image.open(p).convert("RGBA")
    except FileNotFoundError: continue
    a = np.array(im); H = a.shape[0]
    # rows: some sheets stack variants vertically? frames are F x F where F = height / rows
    F = None
    for rows in (1, 2, 3, 4, 5, 6):
        if H % rows == 0 and a.shape[1] % (H // rows) == 0 and a.shape[1] // (H // rows) in (16, 32, 48, 64, 80, 96):
            F = H // rows; break
    if F is None: report.append((s, "?size", a.shape)); continue
    strays = halos = 0
    for y0 in range(0, H, F):
        for x0 in range(0, a.shape[1], F):
            fr = a[y0:y0+F, x0:x0+F]
            m = fr[..., 3] > 0
            if not m.any(): continue
            lab, n = ndimage.label(m, EIGHT)
            if n > 1:
                sizes = ndimage.sum(m, lab, range(1, n + 1)); big = sizes.max(); bi = sizes.argmax() + 1
                body = ndimage.binary_dilation(lab == bi, iterations=3)
                for i, sz in enumerate(sizes, 1):
                    if i == bi: continue
                    # small and not hugging the body: a stray
                    if sz < big * 0.08 and not (body & (lab == i)).any():
                        strays += 1
                        if FIX: fr[lab == i] = 0
            # halo: rim pixels (touching the dark outline) that are light, grey and lighter than
            # the pixels just inside them
            m = fr[..., 3] > 0
            outline = m & (fr[..., :3].astype(int).sum(2) < 90)
            L, S = lum_sat(fr[..., :3])
            inner = m & ~outline
            near_out = ndimage.binary_dilation(outline, EIGHT) & inner
            deep = inner & ~ndimage.binary_dilation(~inner | outline, iterations=2)
            Ld = np.where(deep, L, 0); cnt = ndimage.uniform_filter(deep.astype(float), 7)
            avg = np.where(cnt > 0, ndimage.uniform_filter(Ld, 7) / np.maximum(cnt, 1e-6), 255)
            halo = near_out & (L > 175) & (S < 0.22) & (L > avg + 45)
            halos += int(halo.sum())
            if FIX and halo.any():
                fr[halo] = [22, 18, 24, 255]
    if FIX: Image.fromarray(a).save(p, optimize=True)
    report.append((s.split("/")[-1], F, strays, halos))
for r in report: print(*r)
