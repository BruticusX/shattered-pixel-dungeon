"""Render every frame of a redrawn hero the way HeldWeapon.drawHD will (for checking poses).
    python3 tools/heroes/sim_class.py <cls> <out.png>"""
import re, sys
from PIL import Image
sys.path.insert(0, 'tools/hd'); import slots
CLS, OUT = sys.argv[1], sys.argv[2]
J = open(f'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/HeroHD{CLS.capitalize()}.java').read()
FS = int(re.search(r'Layout\(\s*(\d+),', J).group(1))
arrs = re.findall(r'new (?:float|boolean)\[\]\{([^}]*)\}', J)
HX, HY, AN = [[float(x.rstrip('f')) for x in a.split(', ')] for a in arrs[:3]]
BE = [x == 'true' for x in arrs[3].split(', ')]
G = {int(m[0]): (int(m[1]), int(m[2]), float(m[3]), float(m[4]), float(m[5])) for m in re.findall(r'r\((\d+), (\d+), (\d+), ([\d.]+)f, ([\d.]+)f, ([\d.-]+)f\)', open('core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/ItemSpriteSheetHD.java').read())}
sheet = Image.open(f'core/src/main/assets/sprites/hero_hd_{CLS}_steel.png').convert('RGBA')
hands = Image.open(f'core/src/main/assets/sprites/hero_hd_{CLS}_hands.png').convert('RGBA')
items = Image.open('core/src/main/assets/sprites/items_hd_steel.png').convert('RGBA')
v = slots.load()
def wep(n):
    w, h, gx, gy, ax = G[n]; c, r = n % 16, n // 16
    return items.crop((c * 32, r * 32, c * 32 + w, r * 32 + h)), gx, gy, ax
def render(f, tier, n):
    body = sheet.crop((f * FS, tier * FS, f * FS + FS, tier * FS + FS)); hand = hands.crop((f * FS, 0, f * FS + FS, FS))
    W = FS * 2; off = FS // 2
    spr, gx, gy, ax = wep(n)
    layer = Image.new('RGBA', (W, FS))
    if HX[f] >= 0:
        pad = Image.new('RGBA', (120, 120)); pad.paste(spr, (round(60 - gx), round(60 - gy)), spr)
        rot = pad.rotate(-(AN[f] - ax), center=(60, 60), resample=Image.NEAREST)
        layer.paste(rot, (round(off + HX[f] - 60), round(HY[f] - 60)), rot)
    b = Image.new('RGBA', (W, FS)); b.paste(body, (off, 0), body)
    h = Image.new('RGBA', (W, FS)); h.paste(hand, (off, 0), hand)
    if HX[f] >= 0 and BE[f]: return Image.alpha_composite(layer, b)
    return Image.alpha_composite(Image.alpha_composite(b, layer), h)
n = len(HX); cols = 10
S = 2
out = Image.new('RGBA', (cols * FS * 2 * S, ((n + cols - 1) // cols) * FS * S), (52, 52, 60, 255))
for f in range(n):
    r = render(f, [0, 3, 5, 2][f % 4], v['SWORD'] if f % 2 else v['GREATAXE']).resize((FS * 2 * S, FS * S), Image.NEAREST)
    out.paste(r, ((f % cols) * FS * 2 * S, (f // cols) * FS * S), r)
out.save(OUT)
