"""Sure Shot Dungeon mod: fit a SpriteCook-redrawn UI sheet back onto the game's exact layout.

The game reads every UI piece from fixed pixel rectangles, but AI redraws drift a few
pixels. This splits the ORIGINAL sheet into its separate pieces (connected opaque
regions), finds each piece's counterpart in the redraw, stretches it to the exact
original box and re-applies the original silhouette. Output: a 1x drop-in sheet plus
a 2x master for a later high-resolution switch.

    python3 tools/ui_refresh.py prep <original.png> <upload.png> [scale]
    python3 tools/ui_refresh.py fit  <original.png> <redraw.png> <out_1x.png> <out_2x.png>
    python3 tools/ui_refresh.py merge <raw_rgb.png> <unused> <out_rgba.png>

`merge` cuts the flat background out of SpriteCook's full-resolution raw image.
"""
import sys
from PIL import Image


def components(alpha):
    w, h = alpha.size
    px = alpha.load()
    seen = [[False] * h for _ in range(w)]
    boxes = []
    for y in range(h):
        for x in range(w):
            if px[x, y] == 0 or seen[x][y]:
                continue
            stack, x0, y0, x1, y1 = [(x, y)], x, y, x, y
            seen[x][y] = True
            while stack:
                cx, cy = stack.pop()
                x0, y0, x1, y1 = min(x0, cx), min(y0, cy), max(x1, cx), max(y1, cy)
                for dx in (-1, 0, 1):
                    for dy in (-1, 0, 1):
                        nx, ny = cx + dx, cy + dy
                        if 0 <= nx < w and 0 <= ny < h and not seen[nx][ny] and px[nx, ny] > 0:
                            seen[nx][ny] = True
                            stack.append((nx, ny))
            boxes.append((x0, y0, x1 + 1, y1 + 1))
    return boxes


def bleed(im, rounds=6):
    """Spread colour from opaque pixels into transparent ones so edges never turn black."""
    from PIL import ImageFilter
    im = im.convert("RGBA")
    r, g, b, a = im.split()
    fill = im.copy()
    for _ in range(rounds):
        prem = Image.merge("RGBA", (r, g, b, a))
        blur = prem.filter(ImageFilter.BoxBlur(1))
        a_blur = a.filter(ImageFilter.BoxBlur(1))
        bp, ap = blur.load(), a_blur.load()
        fp = fill.load()
        w, h = im.size
        for y in range(h):
            for x in range(w):
                if fp[x, y][3] < 128 and ap[x, y] > 0:
                    c = bp[x, y]
                    fp[x, y] = (c[0], c[1], c[2], 255)
        r, g, b, a = fill.split()
    out = fill.copy()
    out.putalpha(im.split()[3])
    return out


def fit(orig_path, gen_path, out1, out2, keep_points=(), pad=6):
    orig = Image.open(orig_path).convert("RGBA")
    W, H = orig.size
    gen_src = Image.open(gen_path).convert("RGBA")
    # the redraw may sit inside a larger (letterboxed) canvas: map its content box onto
    # the original sheet's content box first, then fit piece by piece
    gb = gen_src.split()[3].point(lambda v: 255 if v > 96 else 0).getbbox()
    ob = orig.getbbox()
    gen = Image.new("RGBA", (W * 2, H * 2), (0, 0, 0, 0))
    body = gen_src.crop(gb).resize(((ob[2] - ob[0]) * 2, (ob[3] - ob[1]) * 2), Image.LANCZOS)
    gen.paste(body, (ob[0] * 2, ob[1] * 2), body)
    boxes = components(orig.split()[3])
    out = Image.new("RGBA", (W * 2, H * 2), (0, 0, 0, 0))
    kept = []
    for i, (x0, y0, x1, y1) in enumerate(boxes):
        X0, Y0, X1, Y1 = x0 * 2, y0 * 2, x1 * 2, y1 * 2
        wx0, wy0 = max(0, X0 - pad), max(0, Y0 - pad)
        wx1, wy1 = min(W * 2, X1 + pad), min(H * 2, Y1 + pad)
        win = gen.crop((wx0, wy0, wx1, wy1))
        # hide neighbouring pieces so they are not mistaken for this one
        mask = Image.new("L", win.size, 255)
        for j, (a0, b0, a1, b1) in enumerate(boxes):
            if j == i:
                continue
            ox0, oy0 = a0 * 2 - wx0, b0 * 2 - wy0
            ox1, oy1 = a1 * 2 - wx0, b1 * 2 - wy0
            if ox1 > 0 and oy1 > 0 and ox0 < win.width and oy0 < win.height:
                mask.paste(0, (max(0, ox0), max(0, oy0), min(win.width, ox1), min(win.height, oy1)))
        # keep this piece's own box visible even if it overlaps a neighbour's box
        mask.paste(255, (X0 - wx0, Y0 - wy0, X1 - wx0, Y1 - wy0))
        a = Image.composite(win.split()[3], Image.new("L", win.size, 0), mask)
        bb = a.point(lambda v: 255 if v > 96 else 0).getbbox() or (X0 - wx0, Y0 - wy0, X1 - wx0, Y1 - wy0)
        sil = orig.crop((x0, y0, x1, y1)).resize((X1 - X0, Y1 - Y0), Image.NEAREST).split()[3]
        piece = bleed(win.crop(bb).resize((X1 - X0, Y1 - Y0), Image.LANCZOS))
        # if the redraw lost this piece (e.g. a white icon erased with a white background),
        # keep the original art for it rather than inventing pixels
        got = sum(1 for v in piece.split()[3].get_flattened_data() if v > 96)
        want = sum(1 for v in sil.get_flattened_data() if v > 0)
        manual = any(x0 <= kx < x1 and y0 <= ky < y1 for kx, ky in keep_points)
        if manual or (want and got < 0.7 * want):
            piece = orig.crop((x0, y0, x1, y1)).resize((X1 - X0, Y1 - Y0), Image.NEAREST)
            kept.append((x0, y0, x1, y1))
        piece.putalpha(sil)
        out.paste(piece, (X0, Y0), piece)
    out.save(out2)
    low = out.resize((W, H), Image.BOX)
    low.putalpha(orig.split()[3])  # exact original silhouette and translucency
    low.save(out1)
    print(f"{len(boxes)} pieces fitted, {len(kept)} kept original: {kept}")


if __name__ == "__main__":
    if sys.argv[1] == "merge":
        # cut the flat background out of the full-res raw image by flood-filling from the
        # border, so light-coloured pieces inside the art (scrolls, tags, icons) survive
        from collections import deque
        raw = Image.open(sys.argv[2]).convert("RGB")
        w, h = raw.size
        px = raw.load()
        corners = [px[0, 0], px[w - 1, 0], px[0, h - 1], px[w - 1, h - 1]]
        bg = tuple(sorted(c[i] for c in corners)[1] for i in range(3))
        near = lambda c: sum(abs(c[i] - bg[i]) for i in range(3)) < 60
        alpha = Image.new("L", (w, h), 255)
        ap = alpha.load()
        q = deque((x, y) for x in range(w) for y in (0, h - 1))
        q.extend((x, y) for y in range(h) for x in (0, w - 1))
        while q:
            x, y = q.popleft()
            if ap[x, y] == 0 or not near(px[x, y]):
                continue
            ap[x, y] = 0
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if 0 <= nx < w and 0 <= ny < h and ap[nx, ny]:
                    q.append((nx, ny))
        out = raw.convert("RGBA")
        out.putalpha(alpha)
        out.save(sys.argv[4])
    elif sys.argv[1] == "prep":
        im = Image.open(sys.argv[2]).convert("RGBA")
        s = int(sys.argv[4]) if len(sys.argv) > 4 else 4
        im.resize((im.width * s, im.height * s), Image.NEAREST).save(sys.argv[3])
    else:
        # optional extra args "x,y": keep the original art for the piece covering that pixel
        pts = [tuple(int(v) for v in a.split(",")) for a in sys.argv[6:]]
        fit(*sys.argv[2:6], keep_points=pts)
