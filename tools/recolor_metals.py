"""Sure Shot Dungeon mod: generate per-metal recolors of weapon and armor sprites.

Grey (metal) pixels in the weapon/armor rows of items.png are remapped onto each
metal's colour ramp by brightness; wood, leather, gold and gems are left alone.

Usage:  python3 tools/recolor_metals.py <items.png> <out_dir> [--preview preview.png]
"""
import sys
from PIL import Image

# (shadow, mid, highlight) per metal, weakest -> strongest (Mohs hardness)
METALS = {
    "tin":      ((96, 92, 84),   (168, 164, 152), (224, 222, 212)),
    "copper":   ((92, 40, 22),   (184, 101, 56),  (250, 190, 140)),
    "bronze":   ((84, 58, 26),   (176, 128, 64),  (240, 206, 140)),
    "iron":     ((58, 58, 60),   (122, 122, 126), (196, 196, 200)),
    "steel":    ((62, 70, 84),   (150, 162, 180), (240, 246, 255)),
    "obsidian": ((14, 8, 22),    (54, 34, 74),    (170, 130, 220)),
    "titanium": ((66, 70, 112),  (146, 158, 218), (232, 238, 255)),
    "tungsten": ((24, 28, 36),   (68, 76, 90),    (138, 150, 168)),
}

# sprite-sheet rows (16px each, 1-based like ItemSpriteSheet.xy) holding metal gear
ROWS = [7, 8, 9, 10, 12]
SIZE = 16


def is_metal(r, g, b):
    # neutral greys: channels within a small spread of each other
    return max(r, g, b) - min(r, g, b) <= 12


def ramp(colors, v):
    lo, mid, hi = colors
    t = v / 255.0
    if t < 0.5:
        a, b, f = lo, mid, t / 0.5
    else:
        a, b, f = mid, hi, (t - 0.5) / 0.5
    return tuple(round(a[i] + (b[i] - a[i]) * f) for i in range(3))


def recolor(img, colors):
    out = img.copy()
    px = out.load()
    w = img.width
    for row in ROWS:
        y0 = (row - 1) * SIZE
        for y in range(y0, y0 + SIZE):
            for x in range(w):
                r, g, b, a = px[x, y]
                # keep pure black outlines/shadows and translucent drop shadows
                if a < 255 or max(r, g, b) < 24 or not is_metal(r, g, b):
                    continue
                v = (r + g + b) / 3
                px[x, y] = ramp(colors, v) + (a,)
    return out


def main():
    src, out_dir = sys.argv[1], sys.argv[2]
    preview = sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None
    base = Image.open(src).convert("RGBA")
    sheets = {}
    for name, colors in METALS.items():
        sheets[name] = recolor(base, colors)
        sheets[name].save(f"{out_dir}/items_{name}.png", optimize=True)
    if preview:
        make_preview(base, sheets, preview)


def make_preview(base, sheets, path):
    # sample: (row, col) 0-based slot coords of a spread of weapons + armor
    samples = [(6, 1), (6, 4), (6, 8), (6, 13), (7, 0), (7, 1), (7, 5), (7, 8),
               (7, 9), (8, 0), (8, 1), (8, 2), (8, 5), (11, 2), (11, 4)]
    scale, pad = 3, 4
    cell = SIZE * scale + pad
    names = list(METALS)
    im = Image.new("RGBA", (cell * len(names) + pad, cell * len(samples) + pad), (34, 34, 38, 255))
    for ci, name in enumerate(names):
        for ri, (r, c) in enumerate(samples):
            t = sheets[name].crop((c * SIZE, r * SIZE, c * SIZE + SIZE, r * SIZE + SIZE))
            t = t.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
            im.paste(t, (pad + ci * cell, pad + ri * cell), t)
    im.save(path)


if __name__ == "__main__":
    main()
