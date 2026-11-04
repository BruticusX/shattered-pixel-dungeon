"""Sure Shot Dungeon mod: build the 32x32 gear sheet (sprites/items_hd.png) from the SpriteCook masters.

    python3 tools/hd/build_hd_items.py [--preview out.png]

Every gear slot in rows 7-13 and 33-34 gets a 32x32 sprite:
  - redrawn types come from the SpriteCook sheets in tools/spritecook/gear (grid cells),
    trimmed, scaled so their longest side matches the type's real size (LENGTH), given a
    clean 1px outline and drawn top-left in the cell like the rest of the sheet;
  - every other gear slot (darts, class armor, forge items...) is the old 16px art at 2x.
The 16px items.png gets a 2x downscale of each redrawn sprite, so the old pipeline (material
recolors, glow masks, aura maps) and anything reading the 16px sheet keep matching.
Also writes ItemSpriteSheetHD.java with each sprite's rect.
"""
import colorsys
import sys
from PIL import Image

sys.path.insert(0, "tools/hd")
import slots  # noqa: E402

G = "tools/spritecook/gear/"
SHEET16 = "core/src/main/assets/sprites/items.png"
ORIGINAL16 = "tools/hd/items16_original.png"   # the 16px sheet before any redraw (old art for unchanged slots)
OUT_HD = "core/src/main/assets/sprites/items_hd.png"
JAVA = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/ItemSpriteSheetHD.java"
GEAR_ROWS = [7, 8, 9, 10, 11, 12, 13, 33, 34]
COLS, ROWS = 16, 34
OUTLINE = (22, 18, 24, 255)

# sheet file -> grid columns, rows
SHEETS = {"A": ("A_raw.png", 4, 4), "B": ("B_raw.png", 4, 4), "C": ("C_raw.png", 4, 4),
          "D": ("D_raw.png", 4, 4), "E": ("E_raw.png", 2, 2), "R": ("armor_pix.png", 3, 3),
          "F": ("F_raw.png", 4, 4)}

# slot -> (sheet, cell, longest side in px of the 32px cell)
SRC = {
    # daggers and fist
    "STILETTO": ("A", 0, 20), "KRIS": ("A", 1, 22), "DAGGER": ("B", 13, 20), "DIRK": ("B", 14, 23),
    "ASSASSINS_BLADE": ("D", 0, 23), "KATAR": ("A", 10, 21), "TIGER_CLAWS": ("A", 11, 21),
    "GLOVES": ("B", 10, 20), "GAUNTLETS": ("B", 11, 22), "SAI": ("B", 12, 23), "SICKLE": ("B", 15, 22),
    # swords
    "WORN_SHORTSWORD": ("B", 0, 24), "SHORTSWORD": ("B", 1, 24), "SWORD": ("B", 2, 27),
    "RAPIER": ("B", 5, 28), "KATANA": ("B", 6, 29), "BLOODLETTER": ("C", 14, 27), "RUNIC_BLADE": ("C", 15, 28),
    "KHOPESH": ("A", 2, 26), "FALCHION": ("A", 3, 26), "LONGSWORD": ("E", 2, 30), "GREATSWORD": ("E", 0, 32),
    # axes
    "HAND_AXE": ("C", 0, 23), "BEARDED_AXE": ("A", 4, 26), "BATTLE_AXE": ("C", 1, 28), "GREATAXE": ("C", 2, 31),
    # blunt
    "CUDGEL": ("B", 7, 23), "MACE": ("B", 8, 26), "MORNING_STAR": ("A", 5, 28), "WAR_HAMMER": ("E", 3, 29),
    "MAUL": ("A", 6, 32),
    # polearms
    "SPEAR": ("C", 3, 30), "GLAIVE": ("C", 4, 31), "WAR_SCYTHE": ("C", 5, 31), "HALBERD": ("A", 7, 32),
    "PIKE": ("D", 12, 32),
    # staves
    "QUARTERSTAFF": ("C", 6, 31), "MAGES_STAFF": ("C", 7, 30), "IRONSHOD_STAFF": ("A", 9, 32),
    # shields
    "BUCKLER": ("A", 12, 21), "ROUND_SHIELD": ("C", 8, 25), "KITE_SHIELD": ("A", 13, 28), "GREATSHIELD": ("E", 1, 30),
    # flexible
    "WHIP": ("C", 10, 25), "FLAIL": ("C", 11, 27), "KUSARIGAMA": ("A", 14, 28), "METEOR_HAMMER": ("A", 15, 29),
    # ranged
    "CROSSBOW": ("C", 12, 28), "SPIRIT_BOW": ("C", 13, 30),
    "SHURIKEN": ("D", 7, 16), "THROWING_KNIFE": ("D", 6, 19), "KUNAI": ("D", 8, 20), "TOMAHAWK": ("D", 1, 22),
    "FISHING_SPEAR": ("D", 2, 28), "BOOMERANG": ("D", 10, 22), "JAVELIN": ("D", 9, 28), "THROWING_AXE": ("D", 5, 23),
    "THROWING_HAMMER": ("D", 3, 24), "TRIDENT": ("D", 4, 30),
    # armor
    "ARMOR_CLOTH": ("R", 0, 28), "ARMOR_GAMBESON": ("R", 1, 28), "ARMOR_LEATHER": ("R", 2, 28),
    "ARMOR_STUDDED": ("R", 3, 29), "ARMOR_MAIL": ("R", 4, 29), "ARMOR_SCALE": ("R", 5, 29),
    "ARMOR_BRIGANDINE": ("R", 6, 29), "ARMOR_HALF_PLATE": ("R", 7, 30), "ARMOR_PLATE": ("R", 8, 31),
    # class armor, thrown odds and ends, forge items
    "THROWING_STONE": ("F", 1, 17), "BOLAS": ("F", 2, 24), "REPAIR_KIT": ("F", 3, 26),
    "ARMOR_WARRIOR": ("F", 4, 29), "ARMOR_MAGE": ("F", 5, 29), "ARMOR_ROGUE": ("F", 6, 29),
    "ARMOR_HUNTRESS": ("F", 7, 29), "ARMOR_DUELIST": ("F", 8, 29), "ARMOR_CLERIC": ("F", 9, 29),
    "INGOT": ("F", 10, 24), "CLOTH_BOLT": ("F", 11, 24), "LEATHER_PIECE": ("F", 12, 24),
    "METAL_ORE": ("F", 13, 22), "FORGE_SATCHEL": ("F", 14, 24), "TOME_UNLEARNING": ("F", 15, 24),
}
# every dart is the same dart (sheet F, cell 0, flipped to point up-right) with its tip
# tinted to the colour of the old 16px dart's tip
DARTS = ["DART", "ROT_DART", "INCENDIARY_DART", "ADRENALINE_DART", "HEALING_DART", "CHILLING_DART",
         "SHOCKING_DART", "POISON_DART", "CLEANSING_DART", "PARALYTIC_DART", "HOLY_DART",
         "DISPLACING_DART", "BLINDING_DART"]
DART_LEN = 26
ORE_SLOTS = {"METAL_ORE"}                        # rock is warmed up so only the veins take metal tints
CLOTH_SLOTS = {"ARMOR_CLOTH", "ARMOR_GAMBESON", "CLOTH_BOLT"}   # off-white fabric is made neutral grey so cloth tints apply

# ---- non-gear items (tools/hd/misc_items.py) ----
import misc_items  # noqa: E402
M = "tools/spritecook/misc/"
for _k in range((len(misc_items.ITEMS) + 15) // 16):
    SHEETS["M%d" % _k] = ("../misc/M%d_raw.png" % _k, 4, 4)
SHEETS["BASE"] = ("../misc/BASE_raw.png", 3, 3)
for _i, (_name, _desc, _len) in enumerate(misc_items.ITEMS):
    SRC[_name] = ("M%d" % (_i // 16), _i % 16, _len)

# families drawn from one neutral base: (base cell, longest side, how)
POTION_COLOURS = ["CRIMSON", "AMBER", "GOLDEN", "JADE", "TURQUOISE", "AZURE", "INDIGO", "MAGENTA",
                  "BISTRE", "CHARCOAL", "SILVER", "IVORY"]
RUNES = ["KAUNAN", "SOWILO", "LAGUZ", "YNGVI", "GYFU", "RAIDO", "ISAZ", "MANNAZ", "NAUDIZ",
         "BERKANAN", "ODAL", "TIWAZ"]
STONES = ["AGGRESSION", "AUGMENTATION", "FEAR", "BLAST", "BLINK", "CLAIRVOYANCE", "SLEEP", "DETECT",
          "ENCHANT", "FLOCK", "INTUITION", "SHOCK"]
RINGS = ["GARNET", "RUBY", "TOPAZ", "EMERALD", "ONYX", "OPAL", "TOURMALINE", "SAPPHIRE", "AMETHYST",
         "QUARTZ", "AGATE", "DIAMOND"]
DERIVED = []
DERIVED += [("POTION_" + c, 0, 24, "liquid") for c in POTION_COLOURS]
DERIVED += [("EXOTIC_" + c, 1, 24, "liquid") for c in POTION_COLOURS]
DERIVED += [(b, 2, 24, "liquid") for b in ["BREW_INFERNAL", "BREW_BLIZZARD", "BREW_SHOCKING", "BREW_CAUSTIC", "BREW_AQUA"]]
DERIVED += [(e, 3, 24, "liquid") for e in ["ELIXIR_HONEY", "ELIXIR_AQUA", "ELIXIR_MIGHT", "ELIXIR_DRAGON",
                                           "ELIXIR_TOXIC", "ELIXIR_ICY", "ELIXIR_ARCANE", "ELIXIR_FEATHER"]]
DERIVED += [("RING_" + g, 4, 21, "gem") for g in RINGS]
DERIVED += [("SCROLL_" + r, 5, 28, "rune_dark") for r in RUNES] + [("SCROLL_ARSENAL", 5, 28, "rune_red")]
DERIVED += [("EXOTIC_" + r, 6, 28, "rune_glow") for r in RUNES]
DERIVED += [("STONE_" + t, 7, 21, "rune_glow") for t in STONES]
DERIVED += [(d, 8, 21, "page") for d in ["GUIDE_PAGE", "ALCH_PAGE", "SEWER_PAGE", "PRISON_PAGE", "CAVES_PAGE",
                                         "CITY_PAGE", "HALLS_PAGE"]]

_cache = {}


def sheet(key):
    if key not in _cache:
        f, c, r = SHEETS[key]
        _cache[key] = (Image.open(G + f).convert("RGBA"), c, r)
    return _cache[key]


SOLE = {"SPARKLING_POTION"}   # sheets where a neighbour's bits sit right under the item: keep the main body only


def cell(key, i, sole=False):
    im, c, r = sheet(key)
    W, H = im.size
    x, y = i % c, i // c
    cr = im.crop((x * W // c, y * H // r, (x + 1) * W // c, (y + 1) * H // r))
    return keep_main(cr, sole)


def keep_main(cr, sole=False):
    """Drop stray specks from neighbouring cells: keep connected parts at least 4% of the biggest."""
    a = cr.getchannel("A").point(lambda v: 255 if v > 60 else 0)
    w, h = a.size
    ap = a.load()
    seen = [[False] * w for _ in range(h)]
    comps = []
    for y0 in range(h):
        for x0 in range(w):
            if ap[x0, y0] and not seen[y0][x0]:
                stack, pts = [(x0, y0)], []
                seen[y0][x0] = True
                while stack:
                    x, y = stack.pop()
                    pts.append((x, y))
                    for dx in (-1, 0, 1):
                        for dy in (-1, 0, 1):
                            nx, ny = x + dx, y + dy
                            if 0 <= nx < w and 0 <= ny < h and ap[nx, ny] and not seen[ny][nx]:
                                seen[ny][nx] = True
                                stack.append((nx, ny))
                comps.append(pts)
    main = max(comps, key=len)
    big = len(main)
    mx0, my0 = min(p[0] for p in main), min(p[1] for p in main)
    mx1, my1 = max(p[0] for p in main), max(p[1] for p in main)
    pad = max(w, h) * 0.03
    keep = Image.new("L", (w, h), 0)
    kp = keep.load()
    for pts in comps:
        # big parts always stay; small ones only if they sit next to the main body
        near = any(mx0 - pad <= x <= mx1 + pad and my0 - pad <= y <= my1 + pad for x, y in pts[:50])
        if pts is main or (not sole and (len(pts) >= big * 0.4 or (len(pts) >= big * 0.04 and near))):
            for p in pts:
                kp[p] = 255
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.paste(cr, (0, 0), keep)
    return out.crop(keep.getbbox())


def size_for(length):
    """Real sizes run 16..32; squeeze them into 21..32 so small things stay readable."""
    return max(16, min(32, round(24 + (length - 20) * 8 / 12)))


def shrink(im, longest):
    """Scale to `longest` px on the longest side (minus the outline), hard alpha, then outline."""
    inner = longest - 2
    w, h = im.size
    s = inner / max(w, h)
    nw, nh = max(1, round(w * s)), max(1, round(h * s))
    # premultiply so transparent edge pixels don't bleed dark/light fringes into the average
    small = im.resize((nw, nh), Image.BOX)
    px = small.load()
    # how much of each output pixel came from wood/leather (brown) source pixels: thin shafts
    # average out to a muddy grey that the metal recolor would tint, so snap those back to brown
    brown = Image.new("L", im.size, 0)
    bp, ip = brown.load(), im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = ip[x, y]
            if a > 60:
                h, s_, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                if 0.0 <= h <= 0.12 and s_ > 0.22 and v > 0.12:
                    bp[x, y] = 255
    brownf = brown.resize((nw, nh), Image.BOX).load()
    out = Image.new("RGBA", (nw + 2, nh + 2), (0, 0, 0, 0))
    op = out.load()
    for y in range(nh):
        for x in range(nw):
            r, g, b, a = px[x, y]
            if a >= 100:
                if brownf[x, y] >= 90 and max(r, g, b) - min(r, g, b) <= 30:
                    r, g, b = wood((r + g + b) / 3)
                op[x + 1, y + 1] = (r, g, b, 255)
    # lone grey pixels inside wood (shaft specks) become wood too, so they don't take the metal tint
    def is_grey(c):
        return c[3] and max(c[:3]) - min(c[:3]) <= 22 and max(c[:3]) >= 24
    def is_wood(c):
        if not c[3]:
            return False
        h, s_, v = colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)
        return h <= 0.12 and s_ > 0.22
    fixes = []
    for y in range(1, nh + 1):
        for x in range(1, nw + 1):
            c = op[x, y]
            if not is_grey(c):
                continue
            nb = [op[x + dx, y + dy] for dx in (-1, 0, 1) for dy in (-1, 0, 1) if dx or dy]
            if sum(is_grey(n) for n in nb) <= 1 and sum(is_wood(n) for n in nb) >= 2:
                fixes.append((x, y, c))
    for x, y, c in fixes:
        op[x, y] = wood(sum(c[:3]) / 3) + (255,)
    # 1px outline around the silhouette (4-neighbour), plus darken any very light edge pixel
    solid = {(x, y) for y in range(nh + 2) for x in range(nw + 2) if op[x, y][3]}
    for (x, y) in list(solid):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if p not in solid and 0 <= p[0] < nw + 2 and 0 <= p[1] < nh + 2:
                op[p] = OUTLINE
    return out


WOOD = ((46, 28, 18), (112, 70, 40), (176, 124, 78))


def wood(v):
    lo, mid, hi = WOOD
    t = max(0.0, min(1.0, v / 200))
    a, b, f = (lo, mid, t / 0.5) if t < 0.5 else (mid, hi, (t - 0.5) / 0.5)
    return tuple(round(a[i] + (b[i] - a[i]) * f) for i in range(3))


def neutralize_cloth(im):
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s < 0.3 and v > 0.45:
                l = round((r + g + b) / 3 * 1.02)
                px[x, y] = (min(255, l), min(255, l), min(255, l), a)
    return im


def warm_rock(im):
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a and max(r, g, b) - min(r, g, b) <= 24 and (r + g + b) / 3 < 150 and (r, g, b, a) != OUTLINE:
                px[x, y] = (min(255, r + 22), g + 6, max(0, b - 14), a)
    return im


def old_tip_color(old, n):
    """The average colour of the old 16px dart's head (its top-right corner), ignoring the outline."""
    col, row = n % COLS, n // COLS
    pts = []
    for y in range(row * 16, row * 16 + 5):
        for x in range(col * 16 + 10, col * 16 + 16):
            r, g, b, a = old.getpixel((x, y))
            if a == 255 and max(r, g, b) >= 45:
                pts.append((r, g, b))
    if not pts:
        return (200, 200, 200)
    return tuple(round(sum(p[i] for p in pts) / len(pts)) for i in range(3))


def tint_tip(dart, c):
    out = dart.copy()
    px = out.load()
    w, h = out.size
    lo = tuple(round(v * 0.4) for v in c)
    hi = tuple(round(v + (255 - v) * 0.6) for v in c)
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            # the tip is the grey metal in the upper-right third
            if not a or x < w * 0.55 or y > h * 0.45 or max(r, g, b) - min(r, g, b) > 22 or (r, g, b, a) == OUTLINE:
                continue
            t = (r + g + b) / 3 / 255
            if t < 0.5:
                f = t / 0.5
                px[x, y] = tuple(round(lo[i] + (c[i] - lo[i]) * f) for i in range(3)) + (a,)
            else:
                f = (t - 0.5) / 0.5
                px[x, y] = tuple(round(c[i] + (hi[i] - c[i]) * f) for i in range(3)) + (a,)
    return out


def old_cell(old, n):
    col, row = n % COLS, n // COLS
    return old.crop((col * 16, row * 16, col * 16 + 16, row * 16 + 16))


def mode_colour(pts):
    from collections import Counter
    if not pts:
        return (200, 200, 200)
    return Counter(pts).most_common(1)[0][0]


def ramp3(c, t):
    """t in 0..1 along dark (40%) -> c -> light (c toward white)."""
    lo = tuple(v * 0.4 for v in c)
    hi = tuple(v + (255 - v) * 0.5 for v in c)
    if t < 0.5:
        f = t / 0.5
        return tuple(round(lo[i] + (c[i] - lo[i]) * f) for i in range(3))
    f = (t - 0.5) / 0.5
    return tuple(round(c[i] + (hi[i] - c[i]) * f) for i in range(3))


def is_whiteish(r, g, b, s_max=0.18, v_min=0.55):
    h, s_, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return s_ <= s_max and v >= v_min


def tint_region(spr, colour, y_from, y_to):
    """Recolour the near-white pixels between two heights (fractions) with a ramp of colour."""
    out = spr.copy()
    px = out.load()
    for y in range(int(out.height * y_from), int(out.height * y_to)):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a and (r, g, b, a) != OUTLINE and is_whiteish(r, g, b):
                v = max(r, g, b) / 255
                px[x, y] = ramp3(colour, max(0.0, min(1.0, (v - 0.55) / 0.45))) + (a,)
    return out


def old_liquid(o):
    """The old potion's liquid colour: the commonest non-outline colour in its lower half."""
    pts = [o.getpixel((x, y))[:3] for y in range(8, 15) for x in range(2, 14)
           if o.getpixel((x, y))[3] == 255 and max(o.getpixel((x, y))[:3]) >= 45]
    return mode_colour(pts)


def old_gem(o):
    best, bs = (220, 220, 220), -1
    for y in range(0, 8):
        for x in range(3, 13):
            r, g, b, a = o.getpixel((x, y))
            if a < 255 or max(r, g, b) < 40:
                continue
            h, s_, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if 0.09 < h < 0.17 and s_ > 0.5:   # skip the gold of the band
                continue
            score = s_ + v * 0.3
            if score > bs:
                bs, best = score, (r, g, b)
    return best


def old_glyph(o, kind):
    """The rune pixels of an old scroll/stone (centre area), as a set of (x, y) in 16px space."""
    pts = set()
    for y in range(3, 13):
        for x in range(3, 13):
            r, g, b, a = o.getpixel((x, y))
            if a < 255:
                continue
            h, s_, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if kind == "rune_dark" and 40 < (r + g + b) / 3 < 75:
                pts.add((x, y))
            elif kind == "rune_red" and s_ > 0.5 and (h < 0.05 or h > 0.93):
                pts.add((x, y))
            elif kind == "rune_glow" and s_ > 0.45 and 0.03 <= h <= 0.16 and v > 0.5:
                pts.add((x, y))
    return pts


def stamp_glyph(spr, pts, colour, shade, k=2):
    """Draw the old glyph at k x scale, centred on the sprite."""
    if not pts:
        return spr
    out = spr.copy()
    px = out.load()
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    gw, gh = (max(xs) - min(xs) + 1) * k, (max(ys) - min(ys) + 1) * k
    ox = (out.width - gw) // 2 - min(xs) * k
    oy = (out.height - gh) // 2 - min(ys) * k
    for (x, y) in pts:
        for dx in range(k):
            for dy in range(k):
                X, Y = ox + x * k + dx, oy + y * k + dy
                if 0 <= X < out.width and 0 <= Y < out.height and px[X, Y][3]:
                    px[X, Y] = (shade if (k > 1 and dy == k - 1) else colour) + (255,)
    return out


def derived(old, idx):
    """Potions, rings, scrolls, stones and pages made from their neutral base."""
    made = {}
    bases = {}
    for name, cellno, longest, how in DERIVED:
        n = idx[name]
        key = (cellno, longest)
        if key not in bases:
            bases[key] = shrink(cell("BASE", cellno), size_for(longest))
        base = bases[key]
        o = old_cell(old, n)
        if how == "liquid":
            spr = tint_region(base, old_liquid(o), 0.3, 1.0)
        elif how == "gem":
            spr = tint_region(base, old_gem(o), 0.0, 0.55)
        elif how == "page":
            pts = [o.getpixel((x, y))[:3] for y in range(2, 14) for x in range(2, 14) if o.getpixel((x, y))[3] == 255 and max(o.getpixel((x, y))[:3]) > 30 and colorsys.rgb_to_hsv(*[v / 255 for v in o.getpixel((x, y))[:3]])[1] < 0.6]
            c = mode_colour(pts)
            spr = base.copy()
            px = spr.load()
            for y in range(spr.height):
                for x in range(spr.width):
                    r, g, b, a = px[x, y]
                    if a and (r, g, b, a) != OUTLINE:
                        px[x, y] = ramp3(c, max(0.0, min(1.0, (r + g + b) / 3 / 255 * 1.1))) + (a,)
        elif how == "rune_dark":
            spr = stamp_glyph(base, old_glyph(o, how), (70, 44, 26), (70, 44, 26), 1)
        elif how == "rune_red":
            spr = stamp_glyph(base, old_glyph(o, how), (196, 40, 40), (196, 40, 40), 1)
        else:  # rune_glow: 1x on the slim exotic scrolls, 2x on the round stones
            spr = stamp_glyph(base, old_glyph(o, how), (255, 176, 48), (214, 120, 24), 2 if cellno == 7 else 1)
        made[n] = spr
    return made


def even(n):
    return n + (n & 1)


def main():
    idx = slots.load()
    old = Image.open(ORIGINAL16).convert("RGBA")
    hd = Image.new("RGBA", (COLS * 32, ROWS * 32), (0, 0, 0, 0))
    rects = {}
    drawn = {}
    for name, (key, i, longest) in SRC.items():
        spr = shrink(cell(key, i, name in SOLE), size_for(longest))
        if name in CLOTH_SLOTS:
            spr = neutralize_cloth(spr)
        if name in ORE_SLOTS:
            spr = warm_rock(spr)
        drawn[idx[name]] = spr
    drawn.update(derived(old, idx))
    dart = shrink(cell("F", 0).rotate(180), size_for(DART_LEN))
    for name in DARTS:
        n = idx[name]
        drawn[n] = dart if name == "DART" else tint_tip(dart, old_tip_color(old, n))
    for row in GEAR_ROWS:
        for col in range(COLS):
            n = (row - 1) * COLS + col
            x0, y0 = col * 32, (row - 1) * 32
            if n in drawn:
                spr = drawn[n]
                # centre a sprite narrower than the cell? no: SPD draws sprites top-left aligned
                hd.paste(spr, (x0, y0))
                rects[n] = (even(spr.width), even(spr.height))
            else:
                o = old.crop((col * 16, (row - 1) * 16, col * 16 + 16, (row - 1) * 16 + 16))
                if o.getbbox() is None:
                    continue
                hd.paste(o.resize((32, 32), Image.NEAREST), (x0, y0))
                bb = o.getbbox()
                rects[n] = (bb[2] * 2, bb[3] * 2)
    # non-gear slots that were redrawn (gear rows are handled above)
    for n, spr in drawn.items():
        if (n // COLS + 1) in GEAR_ROWS:
            continue
        col, row = n % COLS, n // COLS
        hd.paste(spr, (col * 32, row * 32))
        rects[n] = (even(spr.width), even(spr.height))
    hd.save(OUT_HD, optimize=True)

    # the 16px sheet gets each redrawn sprite at half size
    cur = Image.open(SHEET16).convert("RGBA")
    for n, spr in drawn.items():
        col, row = n % COLS, n // COLS
        if (row + 1) not in GEAR_ROWS:
            continue  # other items keep their old 16px art there (potion colours are picked from it)
        half = spr.resize(((spr.width + 1) // 2, (spr.height + 1) // 2), Image.BOX)
        hp = half.load()
        for y in range(half.height):
            for x in range(half.width):
                r, g, b, a = hp[x, y]
                hp[x, y] = (r, g, b, 255 if a >= 128 else 0)
        box = (col * 16, row * 16, col * 16 + 16, row * 16 + 16)
        cur.paste(Image.new("RGBA", (16, 16), (0, 0, 0, 0)), box[:2])
        cur.paste(half, box[:2])
    cur.save(SHEET16, optimize=True)

    # where each weapon is held: a point 12% of the way from the pommel (bottom-left end) to the tip,
    # and the angle of that pommel->tip axis (degrees from straight up, + = right)
    grips = {}
    for n in rects:
        if (n // COLS + 1) not in GEAR_ROWS:
            continue
        col, row = n % COLS, n // COLS
        cellim = hd.crop((col * 32, row * 32, col * 32 + 32, row * 32 + 32))
        pts = [(x, y) for y in range(32) for x in range(32) if cellim.getpixel((x, y))[3] > 0]
        if not pts:
            continue
        p0 = min(pts, key=lambda p: p[0] - p[1]); p1 = max(pts, key=lambda p: p[0] - p[1])
        gx = p0[0] + 0.12 * (p1[0] - p0[0]); gy = p0[1] + 0.12 * (p1[1] - p0[1])
        import math as _m
        axis = _m.degrees(_m.atan2(p1[0] - p0[0], -(p1[1] - p0[1])))
        grips[n] = (gx, gy, axis)
    lines = []
    for n, (w, h) in sorted(rects.items()):
        if n in grips:
            gx, gy, ax = grips[n]
            lines.append(f"\t\tr({n}, {w}, {h}, {gx:.1f}f, {gy:.1f}f, {ax:.1f}f);")
        else:
            lines.append(f"\t\tr({n}, {w}, {h});")
    open(JAVA, "w").write(JAVA_TEMPLATE.replace("%RECTS%", "\n".join(lines)))
    print(f"built {len(drawn)} redrawn sprites, {len(rects)} HD slots")
    if "--preview" in sys.argv:
        preview(hd, sys.argv[sys.argv.index("--preview") + 1])


def preview(hd, path):
    rows = list(range(1, ROWS + 1))
    crop = Image.new("RGBA", (COLS * 32, len(rows) * 32))
    for k, row in enumerate(rows):
        crop.paste(hd.crop((0, (row - 1) * 32, COLS * 32, row * 32)), (0, k * 32))
    s = 3
    bg = Image.new("RGBA", (crop.width * s, crop.height * s), (40, 40, 48, 255))
    big = crop.resize(bg.size, Image.NEAREST)
    bg.paste(big, (0, 0), big)
    bg.save(path)


JAVA_TEMPLATE = """/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.watabou.noosa.TextureFilm;

//MOD: GENERATED by tools/hd/build_hd_items.py, do not edit by hand.
//The 32x32 gear sheet (sprites/items_hd.png and its material recolors): same slot layout as
//items.png, at twice the resolution. ItemSprite draws these slots at half size, so they take
//the same space on screen as the 16px art with twice the detail.
public class ItemSpriteSheetHD {

	public static final int SIZE = 32;
	private static final int TX_WIDTH = 512;
	private static final int TX_HEIGHT = 1088;
	private static final int WIDTH = TX_WIDTH / SIZE;

	public static TextureFilm film = new TextureFilm( TX_WIDTH, TX_HEIGHT, SIZE, SIZE );
	private static final boolean[] HAS = new boolean[WIDTH * (TX_HEIGHT / SIZE)];

	public static boolean has( int image ){
		return image >= 0 && image < HAS.length && HAS[image];
	}

	public static String sheet( String sheet16 ){
		//sprites/items.png -> sprites/items_hd.png, sprites/items_tin.png -> sprites/items_hd_tin.png
		return sheet16.replace("sprites/items", "sprites/items_hd");
	}

	private static void r( int item, int width, int height ){
		int x = (item % WIDTH) * SIZE;
		int y = (item / WIDTH) * SIZE;
		film.add( item, x, y, x+width, y+height );
		HAS[item] = true;
	}

	//gear: where it is held (in 32px sprite pixels) and the angle of its pommel->tip axis
	private static final float[][] GRIP = new float[WIDTH * (TX_HEIGHT / SIZE)][];

	private static void r( int item, int width, int height, float gripX, float gripY, float axis ){
		r( item, width, height );
		GRIP[item] = new float[]{ gripX, gripY, axis };
	}

	//{x, y, axis degrees} or null
	public static float[] grip( int image ){
		return image >= 0 && image < GRIP.length ? GRIP[image] : null;
	}

	static {
%RECTS%
	}
}
"""

if __name__ == "__main__":
    main()
