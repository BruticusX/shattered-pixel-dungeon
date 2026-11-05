"""Find the weapon hand (the front fist) in each frame of a SpriteCook hero animation sheet.

Skin is recognised by its closeness to the colours of the character's own face (sampled from the top
of the first frame), so it works for light and dark skin alike."""
import colorsys
from collections import Counter
from PIL import Image

FS = 62
_palette = None


def broad_skin(p):
    r, g, b, a = p
    if a < 200:
        return False
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return 0.0 <= h < 0.12 and 0.2 < s < 0.78 and v > 0.18


def learn(face_frame):
    """Sample the face: skin-like pixels in the front half of the head."""
    global _palette
    bb = face_frame.getchannel("A").getbbox()
    top, h = bb[1], bb[3] - bb[1]
    midx = (bb[0] + bb[2]) / 2
    c = Counter()
    for y in range(top, top + int(h * 0.22)):
        for x in range(int(midx), bb[2]):
            p = face_frame.getpixel((x, y))
            if broad_skin(p):
                c[p[:3]] += 1
    # hair and beards are sampled too; drop any colour that also shows up on the trousers and boots
    legs = Counter()
    for y in range(bb[3] - int(h * 0.3), bb[3]):
        for x in range(bb[0], bb[2]):
            p = face_frame.getpixel((x, y))
            if p[3] >= 200:
                legs[p[:3]] += 1
    _palette = [col for col, n in c.most_common(10) if n >= 2
                and not any(legs[q] >= 3 and sum(abs(a - b) for a, b in zip(col, q)) <= 30 for q in legs)]


def is_skin(p):
    if _palette is None:
        # default: light skin
        r, g, b, a = p
        if a < 200:
            return False
        h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
        return 0.015 < h < 0.11 and 0.2 < s < 0.75 and v > 0.6
    if p[3] < 200:
        return False
    return min(abs(p[0] - c[0]) + abs(p[1] - c[1]) + abs(p[2] - c[2]) for c in _palette) <= 30


def blobs(im, f):
    pts = {(x, y) for y in range(FS) for x in range(FS) if is_skin(im.getpixel((f * FS + x, y)))}
    out = []
    while pts:
        stack = [pts.pop()]
        comp = []
        while stack:
            x, y = stack.pop()
            comp.append((x, y))
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    q = (x + dx, y + dy)
                    if q in pts:
                        pts.remove(q)
                        stack.append(q)
        out.append(comp)
    return out


def front_hand(im, f):
    """The rightmost skin blob between the shoulders and the hips (not the face, not the legs)."""
    fr = im.crop((f * FS, 0, f * FS + FS, FS))
    bb = fr.getchannel("A").getbbox()
    top, bottom = bb[1], bb[3]
    h = bottom - top
    best = None
    for comp in blobs(im, f):
        if len(comp) < 4:
            continue
        cy = sum(p[1] for p in comp) / len(comp)
        cx = sum(p[0] for p in comp) / len(comp)
        if not (top + h * 0.3 <= cy <= top + h * 0.75):
            continue
        # brown trousers and boots can match darker or warmer skin: a hand is a small patch, so
        # drop big blobs, and middling ones sitting down at leg height
        if len(comp) > 0.10 * FS * FS or (len(comp) > 0.04 * FS * FS and cy > top + h * 0.62):
            continue
        if best is None or cx > best[0]:
            best = (cx, cy, comp)
    # the weapon hand is on the front half of the body. If the skin test only found the back hand
    # (skin close to the trousers' colour gets filtered out), look for warm skin-like pixels on the
    # front half at arm height instead
    midx = (bb[0] + bb[2]) / 2
    if best is None or best[0] < midx:
        pts = [(x, y) for y in range(top + int(h * 0.35), top + int(h * 0.62))
               for x in range(int(midx) + 1, bb[2]) if broad_skin(fr.getpixel((x, y)))]
        if pts:
            xr = max(p[0] for p in pts)
            near = [p for p in pts if p[0] >= xr - 3]
            best = (sum(p[0] for p in near) / len(near), sum(p[1] for p in near) / len(near), near)
    return best
