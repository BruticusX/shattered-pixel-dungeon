"""Touch up a hero base drawing before it is animated: bring very large drawings down to the usual
pixel size, and lengthen the neck by repeating the pixel row just above the collar.
    python3 tools/heroes/neck_fix.py <in.png> <out.png> <extra_rows> [target_height]"""
import sys
from PIL import Image
import numpy as np

src, dst, extra = sys.argv[1], sys.argv[2], int(sys.argv[3])
target = int(sys.argv[4]) if len(sys.argv) > 4 else 0
im = Image.open(src).convert("RGBA")
im = im.crop(im.getchannel("A").getbbox())
if target and im.height > target * 1.5:
    w = round(im.width * target / im.height)
    a = np.array(im).astype(float)
    a[..., :3] *= a[..., 3:4] / 255
    pm = np.array(Image.fromarray(a.astype(np.uint8)).resize((w, target), Image.BOX)).astype(float)
    al = np.array(im.getchannel("A").resize((w, target), Image.BOX)).astype(float)
    rgb = np.where(al[..., None] > 0, pm[..., :3] * 255 / np.maximum(al[..., None], 1), 0)
    small = Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB")
    small = small.quantize(colors=40, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert("RGBA")
    small.putalpha(Image.fromarray(np.where(al >= 128, 255, 0).astype(np.uint8)))
    im = small
a = np.array(im)
h, w = a.shape[:2]
def tunic(p):
    return p[3] > 0 and min(p[:3]) > 190 and max(p[:3]) - min(p[:3]) < 40
collar = next(y for y in range(int(h * 0.12), h) if sum(tunic(a[y, x]) for x in range(w)) >= 3)
# skin colours from the face (front half of the head, below the hairline)
import colorsys
def skinish(p):
    if p[3] < 200: return False
    hh, ss, vv = colorsys.rgb_to_hsv(p[0] / 255, p[1] / 255, p[2] / 255)
    return 0.0 <= hh < 0.12 and 0.2 < ss < 0.7 and vv > 0.3
mid = w // 2
face = [tuple(int(v) for v in a[y, x][:3]) for y in range(int(collar * 0.35), collar) for x in range(mid, w) if skinish(a[y, x])]
face.sort(key=lambda c: sum(c))
skin = face[len(face) // 2] if face else (200, 150, 110)
shade = tuple(int(v * 0.72) for v in skin)
OUT = (22, 18, 24, 255)
# the neck sits under the back of the jaw: take the skin run in the row above the collar
r = a[collar - 1]
xs = [x for x in range(w) if skinish(r)] if False else [x for x in range(w) if skinish(r[x])]
if xs:
    x0, x1 = min(xs), max(xs)
    nx0, nx1 = x0, max(x0 + 1, x0 + round((x1 - x0) * 0.75))
else:
    nx0, nx1 = mid - 2, mid + 1
rows = []
for i in range(extra):
    nr = r.copy()
    for x in range(w):
        if skinish(r[x]) or nx0 <= x <= nx1:
            nr[x] = (0, 0, 0, 0)
    for x in range(nx0, nx1 + 1):
        nr[x] = (*(shade if i == 0 else skin), 255)
    if nx0 - 1 >= 0 and nr[nx0 - 1][3] == 0: nr[nx0 - 1] = OUT
    if nx1 + 1 < w and nr[nx1 + 1][3] == 0: nr[nx1 + 1] = OUT
    rows.append(nr)
out = np.concatenate([a[:collar], np.array(rows).reshape(-1, w, 4), a[collar:]], axis=0) if rows else a
Image.fromarray(out).save(dst)
print(src, "collar at", collar, "->", out.shape[1], "x", out.shape[0])
