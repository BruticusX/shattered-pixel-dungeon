"""Stone version of a redrawn hero sheet, for statues.
    python3 tools/heroes/stone_sheet.py [cls]   (default warrior)
Reads sprites/hero_hd_<cls>.png, writes sprites/hero_hd_<cls>_stone.png: every pixel mapped by its
brightness onto a weathered stone ramp, outlines kept dark."""
import sys
from PIL import Image

cls = sys.argv[1] if len(sys.argv) > 1 else "warrior"
src = Image.open(f"core/src/main/assets/sprites/hero_hd_{cls}.png").convert("RGBA")
DARK, LIGHT = (58, 56, 52), (222, 218, 200)
px = src.load()
out = Image.new("RGBA", src.size)
op = out.load()
for y in range(src.height):
    for x in range(src.width):
        r, g, b, a = px[x, y]
        if not a:
            continue
        lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
        t = min(1.0, max(0.0, (lum - 0.08) / 0.8)) ** 0.85   # a little more contrast in the mid tones
        op[x, y] = tuple(round(DARK[i] + (LIGHT[i] - DARK[i]) * t) for i in range(3)) + (a,)
out.save(f"core/src/main/assets/sprites/hero_hd_{cls}_stone.png", optimize=True)
print("wrote stone sheet for", cls)
