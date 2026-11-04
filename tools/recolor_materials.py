"""Sure Shot Dungeon mod: build one recoloured item sheet per material.

    python3 tools/recolor_materials.py [--preview preview.png]

Reads core/src/main/assets/sprites/items.png and writes items_<material>.png next to it.
Only the gear rows (weapons, thrown weapons, armor) are kept; everything else is left
transparent so the files stay small. The game picks the sheet from the item's material.

  metals   : neutral grey pixels are remapped onto the metal's colour ramp by brightness
             (plus gold heads on a few weapons, e.g. the war hammer)
  cloth    : neutral light pixels (the fabric) are remapped onto the cloth's ramp
  leather  : brown pixels are remapped onto the leather's ramp
"""
import colorsys
import sys
from PIL import Image

SRC = "core/src/main/assets/sprites/items.png"
OUT = "core/src/main/assets/sprites/items_{}.png"
SIZE = 16
GEAR_ROWS = [7, 8, 9, 10, 11, 12, 13]     # 1-based sheet rows holding weapons and armor
GOLD_HEAD_SLOTS = {129}                 # war hammer: its head is gold, recolour it with the metal

METAL = {
    "tin":      ((96, 92, 84),   (168, 164, 152), (224, 222, 212)),
    "copper":   ((92, 40, 22),   (184, 101, 56),  (250, 190, 140)),
    "bronze":   ((84, 58, 26),   (176, 128, 64),  (240, 206, 140)),
    "iron":     ((58, 58, 60),   (122, 122, 126), (196, 196, 200)),
    "steel":    ((62, 70, 84),   (150, 162, 180), (240, 246, 255)),
    "obsidian": ((14, 8, 22),    (54, 34, 74),    (170, 130, 220)),
    "titanium": ((66, 70, 112),  (146, 158, 218), (232, 238, 255)),
    "tungsten": ((24, 28, 36),   (68, 76, 90),    (138, 150, 168)),
}
CLOTH = {
    "linen":       ((110, 98, 78),  (196, 184, 160), (244, 238, 222)),
    "wool":        ((62, 52, 46),   (128, 110, 96),  (190, 176, 160)),
    "silk":        ((60, 30, 90),   (140, 80, 180),  (220, 190, 240)),
    "spider_silk": ((90, 100, 110), (190, 205, 215), (250, 252, 255)),
}
LEATHER = {
    "hide":           ((80, 50, 26),  (150, 100, 60), (210, 170, 120)),
    "boiled_leather": ((50, 28, 16),  (100, 58, 32),  (160, 110, 70)),
    "wyvern_hide":    ((30, 60, 34),  (70, 120, 64),  (150, 190, 120)),
    "dragonhide":     ((60, 12, 12),  (140, 30, 24),  (220, 90, 60)),
}


def ramp(colors, v):
    lo, mid, hi = colors
    t = max(0.0, min(1.0, v / 255.0))
    a, b, f = (lo, mid, t / 0.5) if t < 0.5 else (mid, hi, (t - 0.5) / 0.5)
    return tuple(round(a[i] + (b[i] - a[i]) * f) for i in range(3))


def neutral(r, g, b, spread=12):
    return max(r, g, b) - min(r, g, b) <= spread


def is_gold(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return 0.08 <= h <= 0.17 and s > 0.35 and v > 0.35


def is_brown(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return 0.02 <= h <= 0.11 and 0.25 < s and 0.15 < v < 0.85


def make(base, colors, kind):
    out = Image.new("RGBA", base.size, (0, 0, 0, 0))
    src, px = base.load(), out.load()
    w = base.width
    for row in GEAR_ROWS:
        y0 = (row - 1) * SIZE
        for y in range(y0, y0 + SIZE):
            for x in range(w):
                r, g, b, a = src[x, y]
                px[x, y] = (r, g, b, a)
                if a < 255 or max(r, g, b) < 24:
                    continue
                slot = (y // SIZE) * (w // SIZE) + x // SIZE
                v = (r + g + b) / 3
                if kind == "metal":
                    if neutral(r, g, b, 22) or (slot in GOLD_HEAD_SLOTS and is_gold(r, g, b)):
                        px[x, y] = ramp(colors, v) + (a,)
                elif kind == "cloth":
                    if neutral(r, g, b, 18) and v >= 80:
                        px[x, y] = ramp(colors, v) + (a,)
                elif kind == "leather":
                    if is_brown(r, g, b):
                        px[x, y] = ramp(colors, min(255, v * 1.4)) + (a,)
    return out


def main():
    base = Image.open(SRC).convert("RGBA")
    sheets = {}
    for kind, table in (("metal", METAL), ("cloth", CLOTH), ("leather", LEATHER)):
        for name, colors in table.items():
            sheets[name] = make(base, colors, kind)
            sheets[name].save(OUT.format(name), optimize=True)
    if "--preview" in sys.argv:
        preview(sheets, sys.argv[sys.argv.index("--preview") + 1])
    print(f"wrote {len(sheets)} material sheets")


def preview(sheets, path):
    picks = {"metal": [(7, 0), (8, 1), (8, 3), (11, 2), (11, 4)], "cloth": [(11, 0)], "leather": [(11, 1), (7, 5)]}
    rows = []
    for kind, table in (("metal", METAL), ("cloth", CLOTH), ("leather", LEATHER)):
        for name in table:
            rows.append((name, [sheets[name].crop((c * SIZE, r * SIZE, c * SIZE + SIZE, r * SIZE + SIZE)) for r, c in picks[kind]]))
    sc, cell = 3, 52
    im = Image.new("RGBA", (cell * 6 + 8, cell * len(rows) + 8), (34, 34, 38, 255))
    for i, (_, tiles) in enumerate(rows):
        for j, t in enumerate(tiles):
            t = t.resize((SIZE * sc, SIZE * sc), Image.NEAREST)
            im.paste(t, (4 + j * cell, 4 + i * cell), t)
    im.save(path)


if __name__ == "__main__":
    main()
