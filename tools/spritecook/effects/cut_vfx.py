"""Pack the two SpriteCook enchantment effect sheets into effects/enchant_vfx.png: 8 rows (fire, frost,
lightning, poison, shadow smoke, light glint, blood, arcane) x 4 frames, 48px cells. Each frame keeps
its place in its cell, so drips fall and flames rise between frames."""
import numpy as np
from PIL import Image
C = 48
out = Image.new("RGBA", (C * 4, C * 8))
for s, name in enumerate(("vfx1_raw.png", "vfx2_raw.png")):
    raw = Image.open("tools/spritecook/effects/" + name).convert("RGBA")
    R = raw.width // 4
    for r in range(4):
        for c in range(4):
            cell = raw.crop((c * R, r * R, c * R + R, r * R + R))
            a = np.array(cell).astype(float)
            a[..., :3] *= a[..., 3:4] / 255
            pm = np.array(Image.fromarray(a.astype(np.uint8)).resize((C, C), Image.BOX)).astype(float)
            al = np.array(cell.getchannel("A").resize((C, C), Image.BOX)).astype(float)
            rgb = np.where(al[..., None] > 0, pm[..., :3] * 255 / np.maximum(al[..., None], 1), 0)
            px = Image.fromarray(np.dstack([np.clip(rgb, 0, 255), al]).astype(np.uint8))
            out.paste(px, (c * C, (s * 4 + r) * C))
out.save("core/src/main/assets/effects/enchant_vfx.png", optimize=True)
print("enchant_vfx", out.size)
