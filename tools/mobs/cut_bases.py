"""Cut a 3x3 SpriteCook still sheet (2048px raw) into 144px base images for pose-sheet references.
    python3 tools/mobs/cut_bases.py <sheet_raw.png> name1 ... name9   (use - to skip a cell)
Names ending in ':loose' keep small detached bits (sparks, embers)."""
import sys
from PIL import Image
import numpy as np
from scipy import ndimage

r = Image.open(sys.argv[1]).convert('RGBA'); W = r.width // 3
for i, n in enumerate(sys.argv[2:11]):
    if n == '-': continue
    loose = n.endswith(':loose'); n = n.split(':')[0]
    c = np.array(r.crop(((i % 3) * W, (i // 3) * W, (i % 3 + 1) * W, (i // 3 + 1) * W)))
    m = c[..., 3] > 100; lab, k = ndimage.label(m); idx = range(1, k + 1)
    sz = ndimage.sum(m, lab, idx); cm = ndimage.center_of_mass(m, lab, idx)
    big = max(sz); ok = []
    for j, (s, (cy, cx)) in enumerate(zip(sz, cm)):
        if s < (60 if loose else 300): continue
        # small bits near the cell edge belong to the neighbouring creature
        if s < big * 0.5 and not (0.06 * W < cx < 0.94 * W and 0.06 * W < cy < 0.94 * W): continue
        ok.append(j + 1)
    keep = np.isin(lab, ok); c[..., 3] = np.where(keep, c[..., 3], 0)
    im = Image.fromarray(c); bb = Image.fromarray((keep * 255).astype('uint8')).getbbox(); im = im.crop(bb)
    s = 140 / max(im.size)
    im = im.resize((max(1, round(im.width * s)), max(1, round(im.height * s))), Image.LANCZOS)
    b = Image.new('RGBA', (144, 144)); b.alpha_composite(im, ((144 - im.width) // 2, 142 - im.height))
    b.save(f'tools/spritecook/mobs/{n}_base.png'); print(n, im.size)
