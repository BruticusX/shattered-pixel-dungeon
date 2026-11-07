"""Sure Shot Dungeon: icons for the mod's always-identified scrolls, drawn on the Arsenal scroll's
parchment with their own glyph and ink colour (Arsenal keeps its red cross).
python3 tools/items/mod_scroll_icons.py"""
from PIL import Image

SHEETS = {"core/src/main/assets/sprites/items_hd.png": 32, "core/src/main/assets/sprites/items.png": 16}
ARSENAL = 19 * 16 + 12
# slot, ink colour, glyph (HD: 5 wide x 7 tall, drawn at x 12..16, y 3..9)
SCROLLS = {
    "mending":  (19 * 16 + 14, (70, 170, 70), [   # a heart
        ".....",
        ".X.X.",
        "XXXXX",
        "XXXXX",
        ".XXX.",
        "..X..",
        "....."]),
    "focus":    (19 * 16 + 15, (60, 120, 230), [  # an eye
        ".....",
        ".XXX.",
        "X...X",
        "X.X.X",
        "X...X",
        ".XXX.",
        "....."]),
    "smelting": (20 * 16 + 12, (235, 125, 25), [  # a flame
        "..X..",
        "..XX.",
        ".XXX.",
        ".XXXX",
        "XXXXX",
        "XXXXX",
        ".XXX."]),
}
# the small sheet: 1 wide x 4 tall marks at x 7, y 3..6 (same spot as Arsenal's)
SMALL = {"mending": ["X", "X", ".", "."], "focus": [".", "X", "X", "."], "smelting": ["X", "X", "X", "X"]}

def is_ink(p):
    return p[3] > 0 and p[0] > 150 and p[1] < 90

for path, S in SHEETS.items():
    im = Image.open(path).convert("RGBA")
    src = im.crop(((ARSENAL % 16) * S, (ARSENAL // 16) * S, (ARSENAL % 16) * S + S, (ARSENAL // 16) * S + S))
    px = src.load()
    # blank parchment: every red ink pixel replaced by the parchment beside it
    blank = src.copy(); bp = blank.load()
    for y in range(S):
        for x in range(S):
            if is_ink(px[x, y]):
                for dx in (-1, 1, -2, 2, -3, 3):
                    q = px[x + dx, y] if 0 <= x + dx < S else (0, 0, 0, 0)
                    if q[3] > 0 and not is_ink(q):
                        bp[x, y] = q; break
    for name, (slot, ink, glyph) in SCROLLS.items():
        tile = blank.copy(); tp = tile.load()
        if S == 32:
            for gy, row in enumerate(glyph):
                for gx, c in enumerate(row):
                    if c == "X": tp[12 + gx, 3 + gy] = ink + (255,)
        else:
            for gy, row in enumerate(SMALL[name]):
                if row == "X": tp[7, 3 + gy] = ink + (255,)
        x0, y0 = (slot % 16) * S, (slot // 16) * S
        im.paste((0, 0, 0, 0), (x0, y0, x0 + S, y0 + S))
        im.paste(tile, (x0, y0))
    im.save(path, optimize=True)
print("mod scroll icons drawn")
