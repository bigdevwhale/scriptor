"""Draws the launcher icon (an illuminated S: cinnabar field, white filigree, gold frame, on lapis) and docs/logo.png.

python tools/icon.py
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
FONT = RES / "font/garamond_semibold.ttf"
LAPIS = (43, 71, 133)
CINNABAR = (176, 55, 42)
GOLD = (205, 163, 62)
GOLD_LIGHT = (246, 226, 160)
GOLD_DEEP = (140, 104, 30)
VELLUM = (251, 246, 233)
S = 8  # supersampling


def gold_fill(size):
    """Burnished gold: a diagonal sheen over the leaf."""
    g = Image.new("RGBA", (size, size))
    px = g.load()
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * size)
            k = 0.5 + 0.5 * math.cos((t - 0.35) * math.pi * 2.2)
            c = tuple(int(GOLD_DEEP[i] + (GOLD_LIGHT[i] - GOLD_DEEP[i]) * k) for i in range(3))
            px[x, y] = c + (255,)
    return g


def initial(size: int) -> Image.Image:
    """The initial alone on a transparent size x size canvas (108 dp units scaled)."""
    W = size * S
    u = W / 108
    im = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    side = 56 * u
    x0 = (W - side) / 2
    y0 = (W - side) / 2
    # Soft shadow.
    sh = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((x0 + 1.5 * u, y0 + 3 * u, x0 + side + 1.5 * u, y0 + side + 3 * u), radius=4 * u, fill=(0, 0, 0, 110))
    im = Image.alpha_composite(im, sh.filter(ImageFilter.GaussianBlur(3 * u)))
    # Gold frame.
    frame = gold_fill(int(side))
    m = Image.new("L", frame.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, frame.size[0] - 1, frame.size[1] - 1), radius=int(4 * u), fill=255)
    im.paste(frame, (int(x0), int(y0)), m)
    d = ImageDraw.Draw(im)
    inset = 4.6 * u
    fx0, fy0, fx1, fy1 = x0 + inset, y0 + inset, x0 + side - inset, y0 + side - inset
    d.rounded_rectangle((fx0, fy0, fx1, fy1), radius=2 * u, fill=CINNABAR + (255,))
    # White filigree tendrils from each corner.
    fil = (255, 255, 255, 150)
    w = max(1, int(0.9 * u))
    cx, cy = (fx0 + fx1) / 2, (fy0 + fy1) / 2
    for k in range(4):
        sx = -1 if k in (0, 3) else 1
        sy = -1 if k in (0, 1) else 1
        ox, oy = cx + sx * (side / 2 - inset - 3 * u), cy + sy * (side / 2 - inset - 3 * u)
        pts = []
        for i in range(30):
            t = i / 29
            r = 10 * u * (1 - t * 0.7)
            a = t * 3.6
            pts.append((ox - sx * (t * 9 * u) + sx * -r * 0.0 + -sx * math.sin(a) * 2.2 * u, oy - sy * (t * 3 * u) - sy * (1 - math.cos(a)) * 2.4 * u))
        d.line(pts, fill=fil, width=w)
        pts = [(ox - sx * (t / 29 * 3 * u) - sx * (1 - math.cos(t / 29 * 3.6)) * 2.4 * u, oy - sy * (t / 29 * 9 * u) - sy * math.sin(t / 29 * 3.6) * 2.2 * u) for t in range(30)]
        d.line(pts, fill=fil, width=w)
        d.ellipse((ox - 1.6 * u, oy - 1.6 * u, ox + 1.6 * u, oy + 1.6 * u), fill=GOLD + (255,))
    # The letter, in vellum with a thin gold edge.
    font = ImageFont.truetype(str(FONT), int(46 * u))
    bbox = d.textbbox((0, 0), "S", font=font)
    tx = cx - (bbox[0] + bbox[2]) / 2
    ty = cy - (bbox[1] + bbox[3]) / 2
    d.text((tx, ty), "S", font=font, fill=VELLUM + (255,), stroke_width=int(0.9 * u), stroke_fill=GOLD + (255,))
    return im.resize((size, size), Image.LANCZOS)


def main():
    dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    for name, k in dens.items():
        d = RES / f"mipmap-{name}"
        d.mkdir(parents=True, exist_ok=True)
        fg = initial(int(108 * k))
        fg.save(d / "ic_launcher_foreground.png")
        n = int(48 * k)
        full = Image.alpha_composite(Image.new("RGBA", fg.size, LAPIS + (255,)), fg)
        crop = int(fg.size[0] * 18 / 108)
        flat = full.crop((crop, crop, fg.size[0] - crop, fg.size[0] - crop)).resize((n, n), Image.LANCZOS)
        for shape, fname in (("sq", "ic_launcher.png"), ("round", "ic_launcher_round.png")):
            mk = Image.new("L", (n * 4, n * 4), 0)
            if shape == "sq":
                ImageDraw.Draw(mk).rounded_rectangle((0, 0, n * 4 - 1, n * 4 - 1), radius=n * 4 // 5, fill=255)
            else:
                ImageDraw.Draw(mk).ellipse((0, 0, n * 4 - 1, n * 4 - 1), fill=255)
            out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
            out.paste(flat, (0, 0), mk.resize((n, n), Image.LANCZOS))
            out.save(d / fname)
    big = 512
    fg = initial(int(big * 108 / 72))
    full = Image.alpha_composite(Image.new("RGBA", fg.size, LAPIS + (255,)), fg)
    c = (fg.size[0] - big) // 2
    flat = full.crop((c, c, c + big, c + big))
    m = Image.new("L", (big * 4, big * 4), 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, big * 4 - 1, big * 4 - 1), radius=big * 4 // 5, fill=255)
    logo = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    logo.paste(flat, (0, 0), m.resize((big, big), Image.LANCZOS))
    (ROOT / "docs").mkdir(exist_ok=True)
    logo.save(ROOT / "docs/logo.png")
    print("ok")


if __name__ == "__main__":
    main()
