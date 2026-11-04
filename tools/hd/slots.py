"""Resolve ItemSpriteSheet constant names to sprite indices by parsing the Java source."""
import re

SRC = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/ItemSpriteSheet.java"


def load():
    text = open(SRC).read()
    text = text[:text.index("public static class Icons")]
    vals = {}
    for name, expr in re.findall(r"(?:public|private) static final int (\w+)\s*=\s*([^;]+);", text):
        expr = expr.strip()
        m = re.match(r"xy\(\s*(\d+)\s*,\s*(\d+)\s*\)", expr)
        if m:
            vals[name] = (int(m.group(1)) - 1) + 16 * (int(m.group(2)) - 1)
            continue
        m = re.match(r"(\w+)\s*\+\s*(\d+)", expr)
        if m and m.group(1) in vals:
            vals[name] = vals[m.group(1)] + int(m.group(2))
            continue
        if expr.isdigit():
            vals[name] = int(expr)
    return vals
