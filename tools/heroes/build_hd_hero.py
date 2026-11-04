"""Sure Shot Dungeon mod: build the redrawn (HD) hero sheets from the SpriteCook animations.

    python3 tools/heroes/build_hd_hero.py warrior

Writes, for the class:
  sprites/hero_hd_<cls>.png             7 armor looks (rows) x all frames (columns), 62x62 each
  sprites/hero_hd_<cls>_<material>.png  the same with the armor tinted per material
  sprites/hero_hd_<cls>_hands.png       one row: only the weapon hand of each frame (drawn over the grip)
  java .../sprites/HeroHD<Cls>.java     frame layout, hand positions and blade angles

Armor looks are made from the SpriteCook body by restyling the tunic pixels in every frame:
  0 none   1 cloth (neutral robe, takes cloth tints)   2 leather   3 mail (from the SpriteCook mail still)
  4 scale  5 plate   6 class armor (plate with a coloured tabard)
"""
import colorsys, math, sys
from PIL import Image
sys.path.insert(0, "tools/heroes"); sys.path.insert(0, "tools")
import hands
from recolor_materials import METAL, CLOTH, LEATHER, ramp

CLS = sys.argv[1] if len(sys.argv) > 1 else "warrior"
SRC = "tools/spritecook/heroes/"
OUT = "core/src/main/assets/sprites/"
JAVA = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/"
FS = 62
ANIMS = [("idle", 8), ("run", 10), ("attack", 8), ("death", 12)]

# per frame: blade angle (degrees from straight up, + = forward) and whether the weapon goes behind
# the body. None = weapon not shown.
POSE = {
    "idle":   [(55, False)] * 8,
    "run":    [(62, False), (56, False), (46, True), (42, False), (52, False),
               (62, False), (56, True), (46, False), (42, False), (52, False)],
    "attack": [(55, False), (25, False), (-100, True), (-105, True), (92, False), (96, False), (80, False), (60, False)],
    "death":  [None] * 12,
}
TABARD = (52, 84, 168)   # class armor tabard colour (warrior blue)


def is_tunic(p):
    r, g, b, a = p
    if a < 200: return False
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return s < 0.22 and v > 0.55 and not hands.is_skin(p)


def shade(p):
    v = (p[0] + p[1] + p[2]) / 3 / 255
    return max(0.0, min(1.0, (v - 0.55) / 0.45))


def grey(c):
    c = max(0, min(255, round(c)))
    return (c, c, c, 255)


def restyle(fr, tier):
    out = fr.copy(); px = out.load()
    if tier == 0: return out
    mask = {(x, y) for y in range(FS) for x in range(FS) if is_tunic(px[x, y])}
    if not mask: return out
    xs = [m[0] for m in mask]; cx = (min(xs) + max(xs)) / 2
    for (x, y) in mask:
        t = shade(px[x, y])
        edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if tier == 1:      # cloth robe: neutral fabric that the cloth tints recolour
            px[x, y] = grey(150 + 95 * t)
        elif tier == 2:    # leather jerkin: brown with darker seams at the edges
            k = 0.65 if edge else 1.0
            px[x, y] = (round((90 + 70 * t) * k), round((54 + 44 * t) * k), round((30 + 26 * t) * k), 255)
        elif tier == 3:    # mail: rings and gaps
            px[x, y] = grey(120 + 70 * t) if (x + y) % 2 else grey(52 + 30 * t)
        elif tier == 4:    # scale: overlapping rows of scales, each row offset
            row = y // 3
            u = (x + (row % 2) * 2) % 4
            if y % 3 == 2 or u == 0:
                px[x, y] = grey(60 + 30 * t)
            else:
                px[x, y] = grey(125 + 75 * t - (u == 3) * 18)
        else:              # plate (5) and class armor (6): smooth steel, plate seams, a shine
            seam = (y % 6 == 0)
            shine = abs(x - (cx + 2)) <= 1
            c = 110 + 90 * t + (30 if shine else 0) - (40 if seam else 0) - (25 if edge else 0)
            px[x, y] = grey(c)
            if tier == 6 and abs(x - cx) <= 2 and not seam:
                k = 0.6 + 0.5 * t
                px[x, y] = (round(TABARD[0] * k), round(TABARD[1] * k), round(TABARD[2] * k), 255)
            if tier == 6 and edge:
                px[x, y] = (200, 160, 60, 255)   # gold trim
    return out


KIND = {1: "cloth", 2: "leather", 3: "metal", 4: "metal", 5: "metal"}


def tint(base_row, armor_row, kind, colors):
    """Recolour the pixels that differ from the unarmored row, like tools/recolor_hero_armor.py."""
    out = armor_row.copy(); op = out.load(); ap = armor_row.load(); bp = base_row.load()
    for y in range(out.height):
        for x in range(out.width):
            p = ap[x, y]
            if p[3] < 200 or p == bp[x, y]: continue
            r, g, b, a = p
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            lum = (r + g + b) / 3
            if kind == "metal" and s < 0.15:
                op[x, y] = ramp(colors, lum) + (a,)
            elif kind == "cloth" and s < 0.15:
                op[x, y] = ramp(colors, lum) + (a,)
            elif kind == "leather" and 0.02 <= h <= 0.12 and s > 0.25:
                op[x, y] = ramp(colors, min(255, lum * 1.4)) + (a,)
    return out


def main():
    allframes, handinfo, anim_ranges = [], [], []
    for name, k in ANIMS:
        im = Image.open(SRC + f"{CLS}_{name}_sheet.png").convert("RGBA")
        start = len(allframes)
        prev = None
        found = [hands.front_hand(im, f) for f in range(k)]
        for f in range(k):
            fr = im.crop((f * FS, 0, f * FS + FS, FS))
            pose = POSE[name][f]
            h = found[f]
            if pose is not None and pose[1] and h is not None:
                # the hand is behind the body in these frames: no finger overlay, keep the position
                h = (h[0], h[1], [])
            if pose is not None and (h is None or h[1] < 20):
                a, b = found[f - 1], found[(f + 1) % k]
                h = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, [])
            allframes.append(fr)
            handinfo.append((h, pose))
        anim_ranges.append((name, start, k))
    n = len(allframes)

    rows = []
    for tier in range(7):
        row = Image.new("RGBA", (n * FS, FS))
        for i, fr in enumerate(allframes):
            row.paste(restyle(fr, tier), (i * FS, 0))
        rows.append(row)
    sheet = Image.new("RGBA", (n * FS, 7 * FS))
    for t, row in enumerate(rows): sheet.paste(row, (0, t * FS))
    sheet.save(OUT + f"hero_hd_{CLS}.png", optimize=True)

    for kind, table in (("metal", METAL), ("cloth", CLOTH), ("leather", LEATHER)):
        for mname, colors in table.items():
            s2 = sheet.copy()
            for tier, k in KIND.items():
                if k != kind: continue
                s2.paste(tint(rows[0], rows[tier], kind, colors), (0, tier * FS))
            s2.save(OUT + f"hero_hd_{CLS}_{mname}.png", optimize=True)

    handsheet = Image.new("RGBA", (n * FS, FS))
    hp = handsheet.load()
    for i, ((h, pose), fr) in enumerate(zip(handinfo, allframes)):
        if h is None or pose is None: continue
        fp = fr.load()
        for (x, y) in h[2]:
            hp[i * FS + x, y] = fp[x, y]
    handsheet.save(OUT + f"hero_hd_{CLS}_hands.png", optimize=True)

    # the avatar crop: head to knees of the first idle frame, 4:5 like the old 12x15 avatar
    bb = allframes[0].getchannel("A").getbbox()
    aw = round((bb[3] - bb[1]) * 0.85 * 0.8); ah = round((bb[3] - bb[1]) * 0.85)
    acx = (bb[0] + bb[2]) // 2
    avatar = (acx - aw // 2, bb[1], aw, ah)
    height = bb[3] - bb[1]

    def jf(v): return "%.1ff" % v
    hx = ", ".join(jf(h[0]) if (h and p) else "-1f" for h, p in handinfo)
    hy = ", ".join(jf(h[1]) if (h and p) else "-1f" for h, p in handinfo)
    ang = ", ".join(jf(p[0]) if p else "0f" for h, p in handinfo)
    behind = ", ".join(("true" if p[1] else "false") if p else "false" for h, p in handinfo)
    ranges = "\n".join(f"\tpublic static final int {nm.upper()}_START = {st}, {nm.upper()}_COUNT = {k};" for nm, st, k in anim_ranges)
    clsname = "HeroHD" + CLS.capitalize()
    lic = open(JAVA + "HeldWeapon.java").read()
    lic = lic[:lic.index("package")]
    open(JAVA + clsname + ".java", "w").write(lic + f"""package com.shatteredpixel.shatteredpixeldungeon.sprites;

//MOD: GENERATED by tools/heroes/build_hd_hero.py, do not edit by hand.
//Layout of the redrawn {CLS} sheets (sprites/hero_hd_{CLS}*.png): {FS}x{FS} frames, one row per armor
//look (0 none, 1 cloth, 2 leather, 3 mail, 4 scale, 5 plate, 6 class armor), and per frame where the
//weapon hand is, the blade angle (degrees from up, + = forward) and whether the weapon goes behind.
public class {clsname} {{

	public static final int FRAME = {FS};
	public static final int FRAMES = {n};
	//the character stands {height} px tall in its frame
	public static final int BODY_HEIGHT = {height};
{ranges}
	public static final int[] AVATAR = {{{avatar[0]}, {avatar[1]}, {avatar[2]}, {avatar[3]}}};

	public static final float[] HAND_X = {{{hx}}};
	public static final float[] HAND_Y = {{{hy}}};
	public static final float[] ANGLE = {{{ang}}};
	public static final boolean[] BEHIND = {{{behind}}};
}}
""")
    print(f"built {CLS}: {n} frames x 7 looks, avatar {avatar}, body {height}px")


main()
