"""Cut a 4x4 SpriteCook pose sheet (1024px raw) into clean pixel-art frames for a monster.
    python3 tools/mobs/cut_frames.py <mob> <height_px>
Reads tools/spritecook/mobs/<mob>_frames_raw.png, writes tools/spritecook/mobs/<mob>_frames.png:
one row of 16 equal frames (row-major from the sheet), feet on a shared baseline, body centred.
All frames get one common scale, taken from the first idle-ish frame (row 4, col 1), so the
monster doesn't grow or shrink between poses."""
import sys
from PIL import Image
import numpy as np
from scipy import ndimage

MOB, H = sys.argv[1], int(sys.argv[2])
SRC = f"tools/spritecook/mobs/{MOB}_frames_raw.png"
OUTLINE = (22, 18, 24, 255)

raw = Image.open(SRC).convert("RGBA")
C = raw.width // 4


def clean(cell):
    a = np.array(cell)
    solid = a[..., 3] >= 180            # drops motion-blur smears and soft shadows
    lab, n = ndimage.label(solid)
    if n == 0:
        return None
    sizes = ndimage.sum(solid, lab, range(1, n + 1))
    big = sizes.max()
    keep = np.zeros_like(solid)
    for i, s in enumerate(sizes, 1):
        if s >= big * 0.04:             # main body plus real detached bits (droplets, tail)
            keep |= lab == i
    a[..., 3] = np.where(keep, 255, 0)
    im = Image.fromarray(a.astype(np.uint8))
    return im.crop(im.getbbox())


cells = [clean(raw.crop((c * C, r * C, c * C + C, r * C + C))) for r in range(4) for c in range(4)]
ref = cells[12]
s = H / ref.height
small = []
for im in cells:
    w, h = max(1, round(im.width * s)), max(1, round(im.height * s))
    # premultiplied box downscale, then hard alpha
    a = np.array(im).astype(float)
    a[..., :3] *= a[..., 3:4] / 255
    pm = Image.fromarray(a.astype(np.uint8)).resize((w, h), Image.BOX)
    al = im.getchannel("A").resize((w, h), Image.BOX)
    p = np.array(pm).astype(float); al = np.array(al).astype(float)
    rgb = np.where(al[..., None] > 0, p[..., :3] * 255 / np.maximum(al[..., None], 1), 0)
    out = np.dstack([np.clip(rgb, 0, 255), np.where(al >= 110, 255, 0)]).astype(np.uint8)
    o = Image.fromarray(out)
    # 1px outline like the items and heroes
    pad = Image.new("RGBA", (w + 2, h + 2)); pad.paste(o, (1, 1))
    pa = np.array(pad); m = pa[..., 3] > 0
    ring = ndimage.binary_dilation(m, structure=[[0, 1, 0], [1, 1, 1], [0, 1, 0]]) & ~m
    pa[ring] = OUTLINE
    small.append(Image.fromarray(pa))
F = max(max(im.width, im.height) for im in small) + 4
sheet = Image.new("RGBA", (F * 16, F))
for i, im in enumerate(small):
    a = np.array(im)[..., 3] > 0
    cx = (np.nonzero(a)[1].mean())  # centre of mass keeps the body still between frames
    sheet.paste(im, (i * F + round(F / 2 - cx), F - 2 - im.height), im)
sheet.save(f"tools/spritecook/mobs/{MOB}_frames.png")
print(MOB, "frame", F)
