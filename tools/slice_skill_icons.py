"""Sure Shot Dungeon mod: slice a SpriteCook skill-icon sheet (a 3x2 grid of bronze-framed tiles)
into small icons, and build the skill icon atlas.

    python3 tools/slice_skill_icons.py out.png size sheet1.png sheet2.png ...

Frame edges are long straight bronze lines, so they show up as the strongest peaks in the
column and row profiles of a bronze-colour mask. The 6 strongest column peaks are the left and
right edges of the 3 columns, the 4 strongest row peaks the top and bottom of the 2 rows.
Each sheet becomes one row of 5 icons (the 6th tile is left empty) in the atlas."""
import sys
from PIL import Image
import numpy as np


def _unused_peaks(profile, n, min_gap):
    order = np.argsort(profile)[::-1]
    chosen = []
    for i in order:
        if all(abs(i - c) >= min_gap for c in chosen):
            chosen.append(int(i))
        if len(chosen) == n:
            break
    return sorted(chosen)


def longest_runs(mask, axis):
    """for each column (axis=0) or row (axis=1), the longest unbroken run of mask pixels"""
    m = mask if axis == 0 else mask.T
    best = np.zeros(m.shape[1], dtype=int)
    cur = np.zeros(m.shape[1], dtype=int)
    for row in m:
        cur = np.where(row > 0, cur + 1, 0)
        best = np.maximum(best, cur)
    return best


def edges(runs, n, min_len, tol=3):
    idx = [i for i, v in enumerate(runs) if v >= min_len]
    groups = []
    for i in idx:
        if groups and i - groups[-1][-1] <= tol:
            groups[-1].append(i)
        else:
            groups.append([i])
    # outermost pixel of each edge group: left/top edges take the min, right/bottom the max
    return groups


def grid(path, ncols=3, nrows=2):
    """columns come from the frames' vertical edges; tiles are square, so the rows follow from the
    first top edge plus the same tile size and gap"""
    a = np.array(Image.open(path).convert('RGB')).astype(int)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    mask = ((r > 80) & (r - b > 35) & (g > 40) & (g < 175)).astype(int)
    h, w = mask.shape
    # find tile spans: pairs of long vertical edges about a quarter of the width apart
    xs = []
    for frac in (0.3, 0.25, 0.2, 0.15, 0.12, 0.1):
        cg = edges(longest_runs(mask, 0), ncols * 2, int(h * frac))
        cs = [(min(c), max(c)) for c in cg]
        spans = []
        for i in range(len(cs)):
            for j in range(i + 1, len(cs)):
                wdt = cs[j][1] - cs[i][0]
                if w * 0.18 <= wdt <= w * 0.36:
                    spans.append((cs[i][0], cs[j][1]))
        if spans:
            tile = sorted(sp[1] - sp[0] for sp in spans)[len(spans) // 2]
            spans = [sp for sp in spans if abs((sp[1] - sp[0]) - tile) <= 3]
            spans = sorted(set(spans))
            #drop overlapping spans
            clean = []
            for sp in spans:
                if not clean or sp[0] > clean[-1][1]:
                    clean.append(sp)
            if len(clean) >= 2:
                xs = clean
                break
    if len(xs) < 2:
        raise SystemExit(f'{path}: could not find the tile columns')
    step = round(sum(xs[i + 1][0] - xs[i][0] for i in range(len(xs) - 1)) / (len(xs) - 1))
    # if a step skips a column, halve it
    tile_w = round(sum(x1 - x0 for x0, x1 in xs) / len(xs))
    while step > tile_w * 1.6:
        step //= 2
    starts = sorted({xs[0][0] + k * step for k in range(-ncols, ncols * 2)})
    starts = [x for x in starts if x >= -2 and x + tile_w <= w + 2]
    # keep the run of ncols starts that covers the detected spans
    best = max(range(max(1, len(starts) - ncols + 1)),
               key=lambda i: sum(1 for x0, x1 in xs if any(abs(x0 - st) <= 3 for st in starts[i:i + ncols])))
    xs = [(max(0, st), max(0, st) + tile_w) for st in starts[best:best + ncols]]
    tile = tile_w
    gap = step - tile_w
    # the top of row 1 is the offset where the horizontal frame edges of every tile line up best
    def score(y):
        total = 0
        for j in range(nrows):
            for yy in (y + j * (tile + gap), y + j * (tile + gap) + tile):
                if 0 <= yy < h:
                    for x0, x1 in xs:
                        total += mask[yy, x0:x0 + tile].sum()
        return total
    top = max(range(0, max(1, h - nrows * tile - (nrows - 1) * gap)), key=score)
    ys = [(top + j * (tile + gap), top + j * (tile + gap) + tile) for j in range(nrows)]
    return [(x0, y0, x0 + tile, y0 + tile) for (y0, y1) in ys for (x0, x1) in xs]


def pick(groups, n):
    """choose n (left, right) pairs of edge groups whose widths are most alike"""
    import itertools
    best, score = None, None
    centers = [sum(g) / len(g) for g in groups]
    for combo in itertools.combinations(range(len(groups)), n * 2):
        widths = [centers[combo[2 * i + 1]] - centers[combo[2 * i]] for i in range(n)]
        gaps = [centers[combo[2 * i + 2]] - centers[combo[2 * i + 1]] for i in range(n - 1)]
        if min(widths) <= 0 or (gaps and min(gaps) < 0):
            continue
        sc = (max(widths) - min(widths)) * 3 + (max(gaps) - min(gaps) if gaps else 0) - sum(widths) * 0.5
        if score is None or sc < score:
            best, score = combo, sc
    return [groups[i] for i in best]


def main():
    out, size, sheets = sys.argv[1], int(sys.argv[2]), sys.argv[3:]
    atlas = Image.new('RGBA', (5 * size, len(sheets) * size), (0, 0, 0, 0))
    for row, path in enumerate(sheets):
        im = Image.open(path).convert('RGBA')
        for c, (x0, y0, x1, y1) in enumerate(grid(path)[:5]):
            atlas.paste(im.crop((x0, y0, x1 + 1, y1 + 1)).resize((size, size), Image.BOX), (c * size, row * size))
    atlas.save(out)


if __name__ == '__main__':
    main()
