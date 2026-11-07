"""Cut the SpriteCook spell effect sheet into the textures the tome spells use."""
from PIL import Image
raw = Image.open("tools/spritecook/effects/spells_raw.png").convert("RGBA")
W, H = raw.size
OUT = "core/src/main/assets/effects/"
def piece(box, w, trim_x=0.0):
    c = raw.crop(box)
    a = c.getchannel("A").point(lambda v: 255 if v > 12 else 0)
    c = c.crop(a.getbbox())
    if trim_x:
        t = int(c.width * trim_x); c = c.crop((t, 0, c.width - t, c.height))
    h = max(1, round(c.height * w / c.width))
    return c.resize((w, h), Image.LANCZOS)
piece((0, 0, W // 2, H // 2), 64).save(OUT + "arcane_missile.png")
piece((W // 2, 0, W, H // 2), 96).save(OUT + "fireball_hd.png")
piece((0, H // 2, W * 2 // 3, H), 128, trim_x=0.08).save(OUT + "ice_beam.png")   # tips trimmed: it gets stretched
piece((W * 2 // 3, H // 2, W, H), 48).save(OUT + "arcane_burst.png")
for n in ("arcane_missile", "fireball_hd", "ice_beam", "arcane_burst"):
    print(n, Image.open(OUT + n + ".png").size)
