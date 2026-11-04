"""Find the weapon hand (the front fist) in each frame of a SpriteCook hero animation sheet."""
import colorsys
from PIL import Image

FS = 62


def is_skin(p):
    r, g, b, a = p
    if a < 200:
        return False
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return 0.015 < h < 0.11 and 0.2 < s < 0.75 and v > 0.6


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
        if best is None or cx > best[0]:
            best = (cx, cy, comp)
    return best
