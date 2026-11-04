"""Sure Shot Dungeon mod: hand-drawn 16x16 sprites for ingots, cloth bolts, leather and ore.

    python3 tools/draw_material_items.py

Draws into row 13 of core/src/main/assets/sprites/items.png (slots 1-5, after the repair kit;
slot 5 is the forge satchel bag).
They are drawn in neutral greys (metal, cloth) and browns (leather) so that
tools/recolor_materials.py tints them per material like the weapons and armor.
"""
from PIL import Image

SRC = "core/src/main/assets/sprites/items.png"
ROW_Y = 12 * 16

# palette: o outline, 1-5 grey dark->light, b/c/d browns, r/s rock
PAL = {
    "o": (18, 18, 20, 255),
    "1": (70, 70, 70, 255), "2": (110, 110, 110, 255), "3": (150, 150, 150, 255),
    "4": (195, 195, 195, 255), "5": (240, 240, 240, 255),
    "b": (96, 58, 30, 255), "c": (140, 88, 48, 255), "d": (184, 128, 78, 255),
    "r": (84, 62, 50, 255), "s": (118, 92, 74, 255), "t": (62, 46, 38, 255),
    "p": (92, 40, 120, 255), "l": (210, 170, 255, 255), "w": (230, 222, 200, 255),
}

INGOT = """
................
................
................
................
......oooooooo..
.....o5544444o..
....o554444443o.
...oooooooooo3o.
...o44444444o3o.
...o43333333o2o.
...o33333333o2o.
...o32222222oo..
...oooooooooo...
................
................
................
"""

BOLT = """
................
................
.....oooo.......
....o5544o......
...o554444oooooo
...o5o..o4444445o
...o5o..o4444444
...o44oo44444443o
...o4444444444o3o
...o3444444443o2o
....o33333333o2o.
....oo2222222oo..
......ooooooo...
................
................
................
"""

LEATHER = """
................
................
................
....oo....oo....
...odcoooocdo...
...occccccccco..
..odcccccccccdo.
..occccccccccco.
..obcccdcccccbo.
...obcccccccbo..
...obccccccccbo.
..obccccccdccbo.
..obbccccccbbo..
...oo.obbbo.oo..
.......ooo......
................
"""

ORE = """
................
................
................
......oooo......
....oossrrooo...
...osrr44rrsso..
..osr4552rrrrso.
..orr3442rrsrro.
..osrr22rs4rrro.
..orrrrrr352rso.
..ortrrrr422rro.
...ortrrrrrrto..
....oottttoo....
......oooo......
................
................
"""


SATCHEL = """
................
................
.....oooooo.....
....o.o..o.o....
....o.o..o.o....
...oooooooooo...
..odddddddddddo.
..odccccccccddo.
..occoooooocco..
..occo4223occo..
..occo3221occo..
..occooooooccco.
..occcccccccbco.
..obbbbbbbbbbbo.
...ooooooooooo..
................
"""


TOME = """
................
................
...oooooooooo...
..oppppppppppo..
..opoooooooopo..
..opo......opo..
..opo.l..l.opo..
..opo..ll..opo..
..opo..ll..opo..
..opo.l..l.opo..
..opo......opo..
..opoooooooopo..
..oppppppppppo..
..owwwwwwwwwwo..
...oooooooooo...
................
"""


def grid(art):
    lines = [l for l in art.strip("\n").split("\n")]
    return [l.ljust(16, ".")[:16] for l in lines][:16]


def main():
    im = Image.open(SRC).convert("RGBA")
    px = im.load()
    for slot, art in ((1, INGOT), (2, BOLT), (3, LEATHER), (4, ORE), (5, SATCHEL), (6, TOME)):
        x0 = slot * 16
        for x in range(x0, x0 + 16):
            for y in range(ROW_Y, ROW_Y + 16):
                px[x, y] = (0, 0, 0, 0)
        for y, line in enumerate(grid(art)):
            for x, ch in enumerate(line):
                if ch in PAL:
                    px[x0 + x, ROW_Y + y] = PAL[ch]
    im.save(SRC, optimize=True)
    print("drew material item sprites")


if __name__ == "__main__":
    main()
