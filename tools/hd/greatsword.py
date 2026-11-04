"""Hand-drawn greatsword. The SpriteCook greatsword came out as just a slightly bigger longsword, so this
draws a proper two-hander pixel by pixel: a broad fullered blade, a wide crossguard, a long two-hand
grip and a heavy pommel. It is drawn on the 45-degree diagonal, pommel bottom-left, tip top-right.

In pixel coordinates, s = x - y runs along the sword and d = x + y - 31 runs across it."""
from PIL import Image

OUTLINE = (22, 18, 24, 255)
STEEL = {"hi": (174, 171, 174), "lt": (150, 147, 150), "md": (118, 115, 118), "dk": (78, 75, 78), "fl": (94, 91, 94)}
GOLD = {"hi": (186, 142, 62), "md": (146, 104, 40), "dk": (98, 66, 24)}
WOOD = {"lt": (86, 54, 32), "dk": (58, 34, 20)}


def _px(s, d):
    """Colour at (s, d), or None."""
    a = abs(d)
    if -28 <= s <= -24:                      # pommel: a rounded knob
        r = 1 if s in (-28, -24) else 2
        if a <= r:
            return GOLD["hi"] if d < 0 and a == r - 1 else (GOLD["dk"] if d > 0 and a == r else GOLD["md"])
    elif -23 <= s <= -12:                    # long grip, leather wrapped
        if a <= 1:
            band = ((s + d) // 2) % 2 == 0
            return WOOD["lt"] if (band and d <= 0) else WOOD["dk"]
    elif -11 <= s <= -9:                     # crossguard, flared ends
        if a <= 7:
            if a >= 6 and s != -10:
                return None
            return GOLD["hi"] if s == -11 and d < 0 else (GOLD["dk"] if s == -9 or d > 4 else GOLD["md"])
    elif -8 <= s <= -6:                      # ricasso
        if a <= 2:
            return STEEL["lt"] if d < 0 else (STEEL["dk"] if d == 2 else STEEL["md"])
    elif -5 <= s <= 28:                      # the blade, tapering over the last stretch
        w = 3 if s <= 20 else max(0, 3 - (s - 19) // 2)
        if a <= w:
            if s >= 27:
                return STEEL["lt"]
            if d in (0, 1) and s <= 18:
                return STEEL["fl"]           # fuller
            if d == -w:
                return STEEL["hi"]
            if d == w:
                return STEEL["dk"]
            return STEEL["lt"] if d < 0 else STEEL["md"]
    return None


def draw():
    im = Image.new("RGBA", (34, 34), (0, 0, 0, 0))
    p = im.load()
    for y in range(34):
        for x in range(34):
            c = _px(x - y, x + y - 33)
            if c:
                p[x, y] = c + (255,)
    a = im.copy().load()
    for y in range(34):
        for x in range(34):
            if a[x, y][3] == 0 and any(0 <= x + dx < 34 and 0 <= y + dy < 34 and a[x + dx, y + dy][3]
                                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                p[x, y] = OUTLINE
    return im.crop(im.getchannel("A").getbbox())
