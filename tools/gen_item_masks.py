"""Sure Shot Dungeon mod: build sprites/items_mask.png, the "glowing parts" of every gear sprite.

    python3 tools/gen_item_masks.py

White pixels mark where a glow is drawn (umbralite's purple pulse, glyph sigils):
  metal gear   : the metal pixels (the same neutral greys tools/recolor_materials.py tints)
  leather gear : the leather (brown) pixels
  cloth armor  : a small rune sigil stamped on the fabric
Everything else is transparent. The sheet has the same layout as items.png.
Also rewrites the aura maps so glyph particles on metal armor come off the plates.
"""
import colorsys
from PIL import Image

SRC = "core/src/main/assets/sprites/items.png"
OUT = "core/src/main/assets/sprites/items_mask.png"
SIZE, COLS = 16, 16
GEAR_ROWS = [7, 8, 9, 10, 11, 12, 13]
ARMOR_ROW = 12
CLOTH, LEATHER = 0, 1           # columns of cloth and leather armor in the armor row
WHIP = (9, 5)                   # row 9 (tier 3 weapons), column 5
SIGIL = [                       # a rune, centred on the cloth armor's chest
    "..#..",
    ".###.",
    "#.#.#",
    "..#..",
    ".#.#.",
]


def neutral(r, g, b, spread=22):
    return max(r, g, b) - min(r, g, b) <= spread


GOLD_SLOTS = {129, 179}          # war hammer head, scale armor: gold-toned metal


def is_gold(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return 0.11 <= h <= 0.18 and s > 0.15 and v > 0.35


def is_brown(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return 0.02 <= h <= 0.11 and 0.25 < s and 0.15 < v < 0.85


def main():
    src = Image.open(SRC).convert("RGBA")
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    for row in GEAR_ROWS:
        for col in range(COLS):
            x0, y0 = col * SIZE, (row - 1) * SIZE
            leather = (row == ARMOR_ROW and col == LEATHER) or (row, col) == WHIP
            cloth = row == ARMOR_ROW and col == CLOTH
            body = []
            for y in range(y0, y0 + SIZE):
                for x in range(x0, x0 + SIZE):
                    r, g, b, a = sp[x, y]
                    if a < 255 or max(r, g, b) < 24:
                        continue
                    body.append((x, y))
                    if cloth:
                        continue
                    slot = (row - 1) * COLS + col
                    gold = slot in GOLD_SLOTS and is_gold(r, g, b)
                    if (leather and is_brown(r, g, b)) or (not leather and (neutral(r, g, b) or gold)):
                        op[x, y] = (255, 255, 255, 255)
            if cloth and body:
                cx = sum(p[0] for p in body) // len(body)
                cy = sum(p[1] for p in body) // len(body)
                solid = set(body)
                for dy, line in enumerate(SIGIL):
                    for dx, ch in enumerate(line):
                        px, py = cx - 2 + dx, cy - 2 + dy
                        if ch == "#" and (px, py) in solid:
                            op[px, py] = (255, 255, 255, 255)
    out.save(OUT, optimize=True)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
