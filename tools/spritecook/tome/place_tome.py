"""Cut the magic tome art (closed icon + open held book) into the item sheets at slots 102/103."""
import glob
from PIL import Image
raw = Image.open("tools/spritecook/tome/tome_raw.png").convert("RGBA")
W = raw.width
def cut(x0, x1):
    c = raw.crop((x0, 0, x1, raw.height))
    a = c.getchannel("A").point(lambda v: 255 if v > 40 else 0)
    return c.crop(a.getbbox())
closed = cut(0, W // 2 - 60)
opened = cut(W // 2 - 60, W)
def fit(im, w, h):
    s = min(w / im.width, h / im.height)
    return im.resize((max(1, round(im.width * s)), max(1, round(im.height * s))), Image.LANCZOS)
hd_closed = fit(closed, 28, 30); hd_open = fit(opened, 32, 30)
sd_closed = fit(closed, 14, 15); sd_open = fit(opened, 16, 15)
print("hd", hd_closed.size, hd_open.size, "sd", sd_closed.size, sd_open.size)
def slot(i, S): return ((i % 16) * S, (i // 16) * S)
for f in glob.glob("core/src/main/assets/sprites/items*.png"):
    if "icons" in f: continue
    hd = "items_hd" in f; S = 32 if hd else 16
    im = Image.open(f).convert("RGBA")
    for i, art in ((102, hd_closed if hd else sd_closed), (103, hd_open if hd else sd_open)):
        x, y = slot(i, S)
        im.paste((0, 0, 0, 0), (x, y, x + S, y + S))
        if "mask" in f:
            a = art.getchannel("A").point(lambda v: 255 if v > 100 else 0)
            w = Image.new("RGBA", art.size, (255, 255, 255, 255)); w.putalpha(a); art = w
        im.paste(art, (x, y), art)
    im.save(f, optimize=True)
open("tools/spritecook/tome/sizes.txt", "w").write(f"{hd_closed.size} {hd_open.size} {sd_closed.size} {sd_open.size}\n")
