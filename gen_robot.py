#!/usr/bin/env python3
"""Generate a clean, anti-aliased Android robot bitmap for the ICS easter egg.

Drawn at 4x supersample then downscaled with LANCZOS so edges are crisp.
Follows the official Android robot proportions (head / body / arms / legs /
antennae) so it is a proper drawable asset rather than runtime Canvas art.
"""
import os
import math
from PIL import Image, ImageDraw

OUT_DIR = "c:/Users/30332/WorkBuddy/2026-08-10-08-41-43/4.0settingUI/app/src/main/res"
S = 2048  # supersample canvas

GREEN = (164, 198, 57, 255)    # #A4C639 Android green
GREEN_D = (122, 150, 40, 255)  # subtle shade for depth
DARK = (38, 38, 38, 255)       # eyes / antenna tips

img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

k = S / 512.0  # design space is 512x512


def rr(x0, y0, x1, y1, r):
    d.rounded_rectangle([x0 * k, y0 * k, x1 * k, y1 * k], radius=r * k, fill=GREEN)


def ellipse(cx, cy, r, fill):
    d.ellipse([(cx - r) * k, (cy - r) * k, (cx + r) * k, (cy + r) * k], fill=fill)


# ---- head (rounded rect) ----
rr(156, 80, 356, 230, 36)

# ---- eyes (dark circles) ----
eye_r = 22
eye_y = 138
for dx in (-46, 46):
    ellipse(256 + dx, eye_y, eye_r, DARK)

# ---- antennae (green stalk + dark tip) ----
ant_top = 80
for dx in (-56, 56):
    bx = 256 + dx
    tx = bx + (18 if dx > 0 else -18)
    ty = ant_top - 62
    d.line([(bx * k, ant_top * k), (tx * k, ty * k)], fill=GREEN, width=int(11 * k))
    ellipse(tx, ty, 14, DARK)

# ---- body (rounded rect, overlaps head slightly) ----
rr(136, 215, 376, 415, 50)

# ---- arms (rounded rects on the sides) ----
rr(106, 245, 166, 395, 26)   # left arm
rr(346, 245, 406, 395, 26)   # right arm

# ---- legs (rounded rects at the bottom) ----
rr(166, 395, 236, 505, 30)   # left leg
rr(276, 395, 346, 505, 30)   # right leg

# crop transparent padding then downscale
bbox = img.getbbox()
img = img.crop(bbox)
w, h = img.size

sizes = {
    "drawable-mdpi": 160,
    "drawable-hdpi": 240,
    "drawable-xhdpi": 320,
}
for folder, target in sizes.items():
    scale = target / max(w, h)
    tw, th = max(1, int(w * scale)), max(1, int(h * scale))
    small = img.resize((tw, th), Image.LANCZOS)
    path = os.path.join(OUT_DIR, folder, "android_robot.png")
    small.save(path)
    print("wrote", path, small.size)

print("done")
