"""Edge clean-up for the 32px gear sprites (weapons, shields, armor), run after they are shrunk and
outlined. It is a pixel artist's pass, not a blur: every pixel stays fully opaque or fully clear.
  1. Notches: a clear (or outline) pixel mostly surrounded by the item becomes part of it, using the
     average of its neighbours, so edges don't have one-pixel bites taken out of them.
  2. Spurs: a lone item pixel sticking out with only one neighbour is dropped (blade tips are kept,
     because a tip continues a line of pixels).
  3. Outline: redrawn as a clean one-pixel line around the item.
  4. Stair steps: where the outline makes a staircase, the inner corner pixel of each step gets a
     tone halfway between the outline and the item, which reads as a smooth diagonal at game size.
  5. A light rim on the top-left edges of metal, the side the light comes from."""
from PIL import Image

OUTLINE = (22, 18, 24, 255)
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
N8 = N4 + ((1, 1), (1, -1), (-1, 1), (-1, -1))


def smooth(spr):
    w, h = spr.size
    pad = Image.new("RGBA", (w + 2, h + 2), (0, 0, 0, 0))
    pad.paste(spr, (1, 1))
    W, H = pad.size
    src = pad.load()
    inside = {(x, y) for y in range(H) for x in range(W) if src[x, y][3] and src[x, y] != OUTLINE}
    col = {p: src[p] for p in inside}

    def n_in(p, ns):
        return sum((p[0] + dx, p[1] + dy) in inside for dx, dy in ns)

    # 1. notches
    add = {}
    for y in range(1, H - 1):
        for x in range(1, W - 1):
            p = (x, y)
            if p in inside:
                continue
            if n_in(p, N4) >= 3 or (n_in(p, N4) >= 2 and n_in(p, N8) >= 6):
                ns = [col[(x + dx, y + dy)] for dx, dy in N8 if (x + dx, y + dy) in inside]
                add[p] = tuple(sum(c[i] for c in ns) // len(ns) for i in range(3)) + (255,)
    for p, c in add.items():
        inside.add(p); col[p] = c
    # 2. spurs: one 8-neighbour and that neighbour is itself a thin end (not a line's tip)
    drop = []
    for p in inside:
        if n_in(p, N8) == 0:
            drop.append(p)
        elif n_in(p, N8) == 1:
            q = next((p[0] + dx, p[1] + dy) for dx, dy in N8 if (p[0] + dx, p[1] + dy) in inside)
            if n_in(q, N8) >= 5:      # sticks out of a solid area: a spur, not a tip
                drop.append(p)
    for p in drop:
        inside.discard(p)
    # 3. outline
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    op = out.load()
    for p in inside:
        op[p] = col[p]
    ring = set()
    for y in range(H):
        for x in range(W):
            if (x, y) not in inside and n_in((x, y), N4):
                ring.add((x, y))
                op[x, y] = OUTLINE
    # 4. stair steps: an outline pixel touching the item on two sides that meet at a corner
    for (x, y) in ring:
        a = [((x + dx, y + dy) in inside) for dx, dy in N4]
        corner = (a[0] or a[1]) and (a[2] or a[3]) and sum(a) == 2
        if corner:
            ns = [col[(x + dx, y + dy)] for dx, dy in N4 if (x + dx, y + dy) in inside]
            avg = tuple(sum(c[i] for c in ns) / len(ns) for i in range(3))
            op[x, y] = tuple(round(OUTLINE[i] * 0.55 + avg[i] * 0.45) for i in range(3)) + (255,)
    # 5. light rim on metal: item pixels whose top or left neighbour is outside the item
    for (x, y) in inside:
        r, g, b, _ = col[(x, y)]
        grey = max(r, g, b) - min(r, g, b) <= 24 and (r + g + b) / 3 > 90
        if grey and ((x, y - 1) not in inside or (x - 1, y) not in inside):
            op[x, y] = (min(255, r + 18), min(255, g + 18), min(255, b + 20), 255)
    return out.crop(out.getchannel("A").getbbox())
