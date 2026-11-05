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
FS = Image.open(SRC + f"{CLS}_idle_sheet.png").height  # frames are square; size differs per class
hands.FS = FS
ANIMS = [("idle", 8), ("run", 10), ("attack", 8), ("death", 12)]

# per frame: blade angle (degrees from straight up, + = forward) and whether the weapon goes behind
# the body. None = weapon not shown.
POSE = {
    "idle":   [(55, False)] * 8,
    "run":    [(62, False), (56, False), (46, True), (42, False), (52, False),
               (62, False), (56, True), (46, False), (42, False), (52, False)],
    # the attack animations are punches: the weapon hand draws back and drives forward, so the blade
    # is raised over the shoulder on the draw and swept down through the punch
    "attack": [(55, False), (15, False), (-15, False), (-25, False), (75, False), (115, False), (95, False), (65, False)],
    "death":  [None] * 12,
}
TABARD = (52, 84, 168)   # class armor tabard colour (warrior blue)


class _Tunic:
    """The tunic is the body's light, low-saturation cloth. Its colours are sampled from the torso of
    the first idle frame, so white hair and beards (which sit above the torso or differ in tone) are
    left alone."""
    palette = None

    def learn(self, fr):
        from collections import Counter
        bb = fr.getchannel("A").getbbox()
        top, h = bb[1], bb[3] - bb[1]
        c = Counter()
        for y in range(top + int(h * 0.42), top + int(h * 0.58)):
            for x in range(bb[0], bb[2]):
                p = fr.getpixel((x, y))
                if p[3] >= 200 and loose_tunic(p):
                    c[p[:3]] += 1
        self.palette = [col for col, n in c.most_common(14) if n >= 2]


def loose_tunic(p):
    r, g, b, a = p
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return s < 0.22 and v > 0.55 and not hands.is_skin(p)


tunic = _Tunic()


def is_tunic(p):
    if p[3] < 200 or not loose_tunic(p):
        return False
    if not tunic.palette:
        return True
    return min(abs(p[0] - c[0]) + abs(p[1] - c[1]) + abs(p[2] - c[2]) for c in tunic.palette) <= 18


def cool_area(fr, x, y):
    """White hair and beards are shaded with cool greys, the tunic with warm ones: look at the light
    pixels around this one and say whether they lean cool."""
    cool = warm = 0
    for dy in range(-3, 4):
        for dx in range(-3, 4):
            X, Y = x + dx, y + dy
            if 0 <= X < FS and 0 <= Y < FS:
                r, g, b, a = fr.getpixel((X, Y))
                if a < 200 or max(r, g, b) < 120 or max(r, g, b) > 235:
                    continue   # the brightest highlight is shared by hair and cloth: ignore it
                if b >= r:
                    cool += 1
                elif r > b + 8:
                    warm += 1
    return cool > warm


def shade(p):
    v = (p[0] + p[1] + p[2]) / 3 / 255
    return max(0.0, min(1.0, (v - 0.55) / 0.45))


def grey(c):
    c = max(0, min(255, round(c)))
    return (c, c, c, 255)


def restyle(fr, tier):
    out = fr.copy(); px = out.load()
    if tier == 0: return out
    mask = {(x, y) for y in range(FS) for x in range(FS) if is_tunic(px[x, y]) and not cool_area(fr, x, y)}
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
        elif tier == 3:    # mail: small interlocking rings, each row offset by half a ring, soft contrast
            u = (x + (y % 2) * 2) % 4
            ring = (1.0, 0.82, 0.62, 0.82)[u]
            px[x, y] = grey((70 + 85 * t) * ring + 22 - (14 if edge else 0))
        elif tier == 4:    # scale: overlapping rounded scales, light at the top and shaded at the rim
            row = y // 3
            u = (x + (row % 2) * 2) % 4
            v = y % 3
            shade_k = ((0.95, 1.12, 1.12, 0.95), (0.9, 1.0, 1.0, 0.9), (0.62, 0.78, 0.78, 0.62))[v][u]
            px[x, y] = grey((75 + 85 * t) * shade_k + 14 - (14 if edge else 0))
        else:              # plate (5) and class armor (6): smooth steel, plate seams, a shine
            seam = (y % 7 == 0)
            shine = abs(x - (cx + 2)) <= 1
            c = 110 + 90 * t + (26 if shine else 0) - (16 if seam else 0) - (22 if edge else 0)
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
    idle0 = Image.open(SRC + f"{CLS}_idle_sheet.png").convert("RGBA").crop((0, 0, FS, FS))
    hands.learn(idle0)
    tunic.learn(idle0)
    for name, k in ANIMS:
        im = Image.open(SRC + f"{CLS}_{name}_sheet.png").convert("RGBA")
        start = len(allframes)
        prev = None
        found = [hands.front_hand(im, f) for f in range(k)]
        if name == "attack":
            # a punching fist can be missed by the skin test (sleeves, shading): when the arm sticks out
            # past the idle silhouette, the fist is its far end
            ib = idle0.getchannel("A").getbbox()
            ih = ib[3] - ib[1]
            for f in range(k):
                fr = im.crop((f * FS, 0, f * FS + FS, FS))
                best = None
                for y in range(ib[1] + int(ih * 0.28), ib[1] + int(ih * 0.6)):
                    xs = [x for x in range(FS) if fr.getpixel((x, y))[3] >= 200]
                    if xs and (best is None or xs[-1] > best[0]):
                        best = (xs[-1], y)
                if best and best[0] > ib[2] + 4 and (found[f] is None or best[0] - 3 > found[f][0] + 2):
                    found[f] = (best[0] - 3, best[1], [])
        for f in range(k):
            fr = im.crop((f * FS, 0, f * FS + FS, FS))
            pose = POSE[name][f]
            h = found[f]
            if pose is not None and pose[1] and h is not None:
                # the hand is behind the body in these frames: no finger overlay, keep the position
                h = (h[0], h[1], [])
            if pose is not None and (h is None or h[1] < 20):
                # borrow from the nearest frames on either side that did find a hand
                near = [found[(f + d) % k] for d in (-1, 1, -2, 2, -3, 3) if found[(f + d) % k] is not None]
                near = [q for q in near if q[1] >= 20][:2] or [handinfo[-1][0]]
                h = (sum(q[0] for q in near) / len(near), sum(q[1] for q in near) / len(near), [])
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
    starts = {nm: (st, k) for nm, st, k in anim_ranges}
    open(JAVA + clsname + ".java", "w").write(lic + f"""package com.shatteredpixel.shatteredpixeldungeon.sprites;

//MOD: GENERATED by tools/heroes/build_hd_hero.py, do not edit by hand.
//Layout of the redrawn {CLS} sheets (sprites/hero_hd_{CLS}*.png): {FS}x{FS} frames, one row per armor
//look (0 none, 1 cloth, 2 leather, 3 mail, 4 scale, 5 plate, 6 class armor), and per frame where the
//weapon hand is, the blade angle (degrees from up, + = forward) and whether the weapon goes behind.
public class {clsname} {{

	public static final HeroHD.Layout LAYOUT = new HeroHD.Layout(
			{FS}, {n}, {height},
			new int[]{{{starts['idle'][0]}, {starts['idle'][1]}, {starts['run'][0]}, {starts['run'][1]}, {starts['attack'][0]}, {starts['attack'][1]}, {starts['death'][0]}, {starts['death'][1]}}},
			new int[]{{{avatar[0]}, {avatar[1]}, {avatar[2]}, {avatar[3]}}},
			new float[]{{{hx}}},
			new float[]{{{hy}}},
			new float[]{{{ang}}},
			new boolean[]{{{behind}}});
}}
""")
    print(f"built {CLS}: {n} frames x 7 looks, avatar {avatar}, body {height}px")


main()
