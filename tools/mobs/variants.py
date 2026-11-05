"""Stack recoloured variants under a monster's redrawn frame row (one row of 16 frames each).
    python3 tools/mobs/variants.py
Reads tools/spritecook/mobs/<mob>_frames.png, writes core/src/main/assets/sprites/<mob>_hd.png with the
base row first and one recoloured row per variant, in the order listed in VARIANTS."""
import colorsys
from PIL import Image

OUT = "core/src/main/assets/sprites/"
SRC = "tools/spritecook/mobs/"
OUTLINE = (22, 18, 24)


def hsv_map(fn):
    def f(p):
        r, g, b, a = p
        if not a or max(abs(r - OUTLINE[0]), abs(g - OUTLINE[1]), abs(b - OUTLINE[2])) < 10:
            return p
        h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
        h, s, v = fn(h, s, v)
        r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0, min(1, s)), max(0, min(1, v)))
        return (round(r * 255), round(g * 255), round(b * 255), a)
    return f


def shift(deg, sat=1.0, val=1.0, keep_red=False):
    def fn(h, s, v):
        if keep_red and (h < 0.03 or h > 0.95) and s > 0.6:
            return h, s, v                  # eyes stay red
        return h + deg / 360, s * sat, v * val
    return hsv_map(fn)


def pale(keep_red=True):
    """Albino: fur goes off-white, pink skin and red eyes stay."""
    def fn(h, s, v):
        if keep_red and (h < 0.03 or h > 0.9) and s > 0.35:
            return h, s, v
        return 0.1, s * 0.12, 0.45 + v * 0.55
    return hsv_map(fn)


def grey():
    return hsv_map(lambda h, s, v: (h, s * 0.15, v * 0.95))


VARIANTS = {
    "rat":   [pale(), shift(55, 0.9, 0.85, keep_red=True)],          # albino rat, fetid rat
    "gnoll": [shift(230, 0.8, 0.9), grey()],                         # trickster, exile
    "crab":  [shift(-25, 1.0, 0.7), shift(25, 0.75, 0.8)],           # great crab, hermit crab
    "slime": [shift(160, 1.0, 0.95)],                                # caustic slime
    "skeleton": [shift(190, 0.5, 0.9)],                             # vault skeleton
    "thief": [shift(-150, 0.9, 1.0)],                                # bandit
    "dm100": [shift(180, 0.8, 0.95)],                                # vault DM-100
    "guard": [],
    "necromancer": [shift(170, 0.8, 1.15)],                          # spectral necromancer
    "rotheart": [],
    "rotlasher": [],
    "snake": [],
    "swarm": [],
    "bat": [],
    "brute": [shift(200, 0.35, 0.75)],                               # armored brute
    "shaman": [shift(215, 1.0, 0.95), shift(270, 0.9, 0.95), shift(110, 0.8, 0.95)],  # blue, purple, vault
    "spinner": [shift(110, 0.55, 1.05)],                             # fungal spinner
    "dm200": [shift(180, 0.8, 0.95), shift(90, 0.7, 0.8)],           # vault DM-200, DM-201
    "gnollguard": [],
    "gnollsapper": [],
    "ghoul": [],
    "elemental": [shift(18, 0.8, 1.15), shift(180, 0.9, 1.0), shift(40, 0.45, 1.2), shift(270, 0.9, 0.95)],  # newborn, frost, shock, chaos
    "warlock": [],
    "monk": [shift(-30, 1.0, 0.8)],                                  # senior monk
    "golem": [],
    "ripper": [],
    "spawner": [],
    "wraith": [],
    "piranha": [shift(190, 0.45, 1.15)],                             # phantom piranha
    "mimic": [shift(18, 1.35, 1.15), shift(185, 0.7, 1.15), shift(0, 0.25, 0.5)],  # golden, crystal, ebony
    "bee": [],
    "succubus": [],
    "eye": [],
    "scorpio": [shift(100, 0.9, 1.0)],                               # acidic scorpio
    "sheep": [],
    "crystalguardian": [shift(25), shift(-35), shift(195)],          # blue, green, red
    "crystalwisp": [shift(-80), shift(170)],                         # green, red
    "fungalsentry": [],
    "sentry": [shift(190), shift(-25, 1.2, 1.0)],                    # vault scan, vault laser
    "earthguardian": [],
    "shopkeeper": [],
    "ghost": [shift(150, 0.8, 0.85)],                                # tormented spirit
    "wandmaker": [],
    "blacksmith": [],
    "imp": [],
    "ratking": [],
}

import sys
for mob, variants in VARIANTS.items():
    if len(sys.argv) > 1 and mob not in sys.argv[1:]: continue
    base = Image.open(SRC + f"{mob}_frames.png").convert("RGBA")
    sheet = Image.new("RGBA", (base.width, base.height * (1 + len(variants))))
    sheet.paste(base, (0, 0))
    for i, fn in enumerate(variants, 1):
        row = Image.new("RGBA", base.size)
        row.putdata([fn(p) for p in base.getdata()])
        sheet.paste(row, (0, i * base.height))
    sheet.save(OUT + f"{mob}_hd.png", optimize=True)
    print(mob, sheet.size)
