"""Warrior sample: the new hero animations with a held weapon (fingers over the grip) and mail armor.
Writes a preview sheet and GIFs to the scratchpad given as argv[1]."""
import colorsys, math, sys
from PIL import Image
sys.path.insert(0, "tools/heroes"); sys.path.insert(0, "tools/hd")
import hands, slots

OUT = sys.argv[1]
D = "tools/spritecook/heroes/"
FS = 62
ANIMS = [("idle", 8), ("run", 10), ("attack", 8), ("death", 12)]
# blade angle per frame: degrees from straight up, positive = forward (to the right)
ANGLES = {
    "idle": [55] * 8,
    "run": [60, 55, 40, 35, 50, 60, 55, 40, 35, 50],
    "attack": [55, 20, -115, -120, 95, 100, 80, 55],
}
HAND_FIX = {("run", 2): "avg", ("run", 6): "avg"}

def frames(name, k):
    im = Image.open(D + f"warrior_{name}_sheet.png").convert("RGBA")
    return im, [im.crop((f * FS, 0, f * FS + FS, FS)) for f in range(k)]

def is_tunic(p):
    r, g, b, a = p
    if a < 200: return False
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return s < 0.22 and v > 0.55 and not hands.is_skin(p)

def mail(fr):
    out = fr.copy(); px = out.load()
    for y in range(FS):
        for x in range(FS):
            p = px[x, y]
            if is_tunic(p):
                v = (p[0] + p[1] + p[2]) / 3 / 255          # tunic shading 0.55..1
                t = max(0.0, min(1.0, (v - 0.55) / 0.45))
                if (x + y) % 2:                              # ring highlight
                    c = 120 + 70 * t
                else:                                        # gap between rings
                    c = 52 + 30 * t
                px[x, y] = (round(c), round(c * 0.97), round(c * 0.95), 255)
    return out

def weapon(name):
    v = slots.load(); n = v[name]
    hd = Image.open("core/src/main/assets/sprites/items_hd_steel.png").convert("RGBA")
    c, r = n % 16, n // 16
    spr = hd.crop((c * 32, r * 32, c * 32 + 32, r * 32 + 32))
    pts = [(x, y) for y in range(32) for x in range(32) if spr.getpixel((x, y))[3] > 0]
    p0 = min(pts, key=lambda p: p[0] - p[1]); p1 = max(pts, key=lambda p: p[0] - p[1])
    grip = (p0[0] + 0.12 * (p1[0] - p0[0]), p0[1] + 0.12 * (p1[1] - p0[1]))
    axis = math.degrees(math.atan2(p1[0] - p0[0], -(p1[1] - p0[1])))   # from up, + = right
    return spr, grip, axis

def compose(fr, hand, angle, wep, scale=1.0):
    """Body, then the weapon rotated about its grip at the hand, then the hand's skin on top."""
    spr, grip, axis = wep
    PADL, PADR = 16, 34
    padded = Image.new("RGBA", (FS + PADL + PADR, FS))
    padded.paste(fr, (PADL, 0))
    fr = padded
    if hand is not None:
        hand = (hand[0] + PADL, hand[1], [(x + PADL, y) for (x, y) in hand[2]])
    out = fr.copy()
    if hand is not None and angle is not None:
        hx, hy, comp = hand
        big = spr.resize((round(32 * scale), round(32 * scale)), Image.NEAREST)
        g = (grip[0] * scale, grip[1] * scale)
        pad = Image.new("RGBA", (160, 160)); off = (80 - g[0], 80 - g[1])
        pad.paste(big, (round(off[0]), round(off[1])), big)
        rot = pad.rotate(-(angle - axis), center=(80, 80), resample=Image.NEAREST)
        out.alpha_composite(rot, (round(hx - 80), round(hy - 80))) if False else None
        layer = Image.new("RGBA", out.size)
        layer.paste(rot, (round(hx - 80), round(hy - 80)), rot)
        out = Image.alpha_composite(out, layer)
        px = out.load(); fp = fr.load()
        for (x, y) in comp:            # fingers close over the grip
            px[x, y] = fp[x, y]
    return out

def main():
    sword = weapon("SWORD"); axe = weapon("BATTLE_AXE")
    sheet_rows = []
    gif_a, gif_b = [], []
    for name, k in ANIMS:
        im, frs = frames(name, k)
        hs = [hands.front_hand(im, f) for f in range(k)]
        for f in range(k):
            if HAND_FIX.get((name, f)) == "avg":
                a, b = hs[f - 1], hs[(f + 1) % k]
                pts = [p for p in a[2]]
                dx = (b[0] - a[0]) / 2; dy = (b[1] - a[1]) / 2
                hs[f] = (a[0] + dx, a[1] + dy, [])
        row_a, row_b = [], []
        for f in range(k):
            ang = ANGLES[name][f] if name in ANGLES else None
            h = hs[f] if name in ANGLES else None
            a = compose(frs[f], h, ang, sword)
            b = compose(mail(frs[f]), h, ang, axe)
            row_a.append(a); row_b.append(b)
        sheet_rows.append((row_a, row_b))
        reps = 2 if name == "idle" else 1
        if name != "death":
            gif_a += row_a * reps; gif_b += row_b * reps
    # preview sheet at 3x on a floor-ish background
    S = 3
    W = max(len(r[0]) for r in sheet_rows) * (FS + 50) * S
    H = len(sheet_rows) * 2 * FS * S
    bg = Image.new("RGBA", (W, H), (52, 52, 60, 255))
    for i, (ra, rb) in enumerate(sheet_rows):
        for j, (fa, fb) in enumerate(zip(ra, rb)):
            for k2, fimg in enumerate((fa, fb)):
                big = fimg.resize((fimg.width * S, FS * S), Image.NEAREST)
                bg.paste(big, (j * (FS + 50) * S, (i * 2 + k2) * FS * S), big)
    bg.save(OUT + "/warrior_sample_sheet.png")
    for nm, seq in (("warrior_sample_sword.gif", gif_a), ("warrior_sample_mail_axe.gif", gif_b)):
        fr = []
        for s in seq:
            c = Image.new("RGBA", ((FS + 50) * 4, FS * 4), (52, 52, 60, 255))
            big = s.resize((s.width * 4, FS * 4), Image.NEAREST); c.paste(big, (0, 0), big)
            fr.append(c.convert("P", palette=Image.ADAPTIVE))
        fr[0].save(OUT + "/" + nm, save_all=True, append_images=fr[1:], duration=110, loop=0)

main()
