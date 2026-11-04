"""Sure Shot Dungeon mod: hero sprite sheets with the worn armor tinted by its material.

    python3 tools/recolor_hero_armor.py [--preview out.png]

For every hero class and material this writes sprites/hero_<class>_<material>.png: a copy of the
class sheet where the armor rows of that material's kind are recoloured with the same colour
ramps as the item sprites (tools/recolor_materials.py):
  cloth   -> row 1 (cloth armor, gambeson)
  leather -> row 2 (leather, studded leather)
  metal   -> rows 3-5 (mail/brigandine, scale/half plate, plate)
Armor pixels are found by comparing each frame with the unarmored frame in row 0, so skin,
hair and the death-animation bones are never touched.
"""
import colorsys
import sys
from PIL import Image

sys.path.insert(0, "tools")
from recolor_materials import METAL, CLOTH, LEATHER, ramp  # noqa: E402

DIR = "core/src/main/assets/sprites/"
CLASSES = ["warrior", "mage", "rogue", "huntress", "duelist", "cleric"]
FW, FH = 12, 15
DEATH = range(8, 13)          # the death-animation frames (bones) are never recoloured
ROWS = {"cloth": [1], "leather": [2], "metal": [3, 4, 5]}


def hsv(r, g, b):
    return colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)


def is_metal(r, g, b):
    h, s, v = hsv(r, g, b)
    # grey steel, plus the khaki/gold tones of the scale armor row
    return (max(r, g, b) - min(r, g, b) <= 26 and v > 0.2) or (0.09 <= h <= 0.2 and s > 0.15 and v > 0.35)


def is_cloth(r, g, b):
    h, s, v = hsv(r, g, b)
    return s < 0.22 and v > 0.45


def is_leather(r, g, b):
    h, s, v = hsv(r, g, b)
    return 0.02 <= h <= 0.13 and s > 0.25 and 0.15 < v < 0.9


TEST = {"metal": is_metal, "cloth": is_cloth, "leather": is_leather}


def recolor(base, kind, colors):
    out = base.copy()
    bp, op = base.load(), out.load()
    frames = base.width // FW
    for row in ROWS[kind]:
        for f in range(frames):
            if f in DEATH:
                continue
            for y in range(FH):
                for x in range(FW):
                    px, py = f * FW + x, row * FH + y
                    r, g, b, a = bp[px, py]
                    if a < 255:
                        continue
                    if bp[f * FW + x, y] == (r, g, b, a):   # the same as unarmored: skin, hair...
                        continue
                    if max(r, g, b) < 30 or not TEST[kind](r, g, b):
                        continue
                    v = (r + g + b) / 3
                    if kind == "leather":
                        v = min(255, v * 1.4)
                    op[px, py] = ramp(colors, v) + (a,)
    return out


def main():
    made = {}
    for cls in CLASSES:
        base = Image.open(DIR + cls + ".png").convert("RGBA")
        for kind, table in (("metal", METAL), ("cloth", CLOTH), ("leather", LEATHER)):
            for name, colors in table.items():
                im = recolor(base, kind, colors)
                im.save(DIR + f"hero_{cls}_{name}.png", optimize=True)
                made[(cls, name)] = im
    print(f"wrote {len(made)} hero sheets")
    if "--preview" in sys.argv:
        preview(made, sys.argv[sys.argv.index("--preview") + 1])


def preview(made, path):
    names = list(METAL) + list(CLOTH) + list(LEATHER)
    rowfor = {**{n: 4 for n in METAL}, **{n: 1 for n in CLOTH}, **{n: 2 for n in LEATHER}}
    out = Image.new("RGBA", (len(names) * 14 + 2, len(CLASSES) * 17 * 2 + 2), (40, 40, 48, 255))
    for i, cls in enumerate(CLASSES):
        for j, n in enumerate(names):
            im = made[(cls, n)]
            for k, row in enumerate([rowfor[n], 5 if n in METAL else rowfor[n]]):
                fr = im.crop((0, row * FH, FW, row * FH + FH))
                out.paste(fr, (2 + j * 14, 2 + (i * 2 + k) * 17), fr)
    out.resize((out.width * 4, out.height * 4), Image.NEAREST).save(path)


if __name__ == "__main__":
    main()
