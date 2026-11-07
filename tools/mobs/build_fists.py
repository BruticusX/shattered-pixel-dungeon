"""Stack Yog's six fists (cut with cut_frames.py) into one sheet, one row per type in FistSprite's
order, and drop flat slivers above the body (the bottom of the pose above in the source grid).
    python3 tools/mobs/build_fists.py"""
import numpy as np
from PIL import Image
from scipy import ndimage
S = "tools/spritecook/mobs/"; OUT = "core/src/main/assets/sprites/fists_hd.png"
names = ["burning", "soiled", "rotting", "rusted", "bright", "dark"]
ims = [Image.open(S + f"fist_{n}_frames.png").convert("RGBA") for n in names]
F = max(im.height for im in ims)
out = Image.new("RGBA", (F * 16, F * len(ims)))
for r, im in enumerate(ims):
    f0 = im.height
    for i in range(16):
        fr = im.crop((i * f0, 0, i * f0 + f0, f0))
        out.paste(fr, (i * F + (F - f0) // 2, r * F + (F - f0)), fr)
a = np.array(out)
for y0 in range(0, a.shape[0], F):
    for x0 in range(0, a.shape[1], F):
        fr = a[y0:y0 + F, x0:x0 + F]; m = fr[..., 3] > 0
        lab, k = ndimage.label(m, np.ones((3, 3), bool))
        if k < 2: continue
        sz = ndimage.sum(m, lab, range(1, k + 1)); objs = ndimage.find_objects(lab); bi = int(sz.argmax()) + 1
        btop = objs[bi - 1][0].start
        for i in range(1, k + 1):
            if i == bi: continue
            sl = objs[i - 1]; h = sl[0].stop - sl[0].start; w = sl[1].stop - sl[1].start
            if sz[i - 1] < sz.max() * 0.25 and (sl[0].stop < btop - 6 and w >= 4 * h
                    or sz[i - 1] < sz.max() * 0.08 and sl[0].stop < F * 0.3 and w >= 2 * h):
                fr[lab == i] = 0
Image.fromarray(a).save(OUT, optimize=True)
print("fists", F)
