from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter


ASSETS_DIR = Path(__file__).resolve().parent.parent / "assets"
ASSETS_DIR.mkdir(parents=True, exist_ok=True)


PALETTE = {
    "bg_top": (7, 15, 32),
    "bg_bottom": (3, 7, 18),
    "cyan": (32, 222, 255),
    "cyan_soft": (76, 237, 255),
    "white": (240, 250, 255),
    "track": (18, 42, 71),
}


def vertical_gradient(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        t = y / (size - 1)
        r = int(PALETTE["bg_top"][0] * (1 - t) + PALETTE["bg_bottom"][0] * t)
        g = int(PALETTE["bg_top"][1] * (1 - t) + PALETTE["bg_bottom"][1] * t)
        b = int(PALETTE["bg_top"][2] * (1 - t) + PALETTE["bg_bottom"][2] * t)
        for x in range(size):
            px[x, y] = (r, g, b, 255)
    return img


def draw_speed_pin(size: int, with_bg: bool = True) -> Image.Image:
    if with_bg:
        icon = vertical_gradient(size)
    else:
        icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    draw = ImageDraw.Draw(icon, "RGBA")
    c = size // 2

    # Subtle racing lanes in the background
    lane_w = max(2, size // 56)
    for i in range(4):
        y = int(size * (0.68 + i * 0.07))
        draw.line(
            [(int(size * 0.18), y), (int(size * 0.82), y - int(size * 0.12))],
            fill=PALETTE["track"] + (110,),
            width=lane_w,
        )

    # Pin body
    pin_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    pin = ImageDraw.Draw(pin_layer, "RGBA")
    pin_top_r = int(size * 0.24)
    pin_center = (c, int(size * 0.38))
    pin.ellipse(
        [
            pin_center[0] - pin_top_r,
            pin_center[1] - pin_top_r,
            pin_center[0] + pin_top_r,
            pin_center[1] + pin_top_r,
        ],
        fill=PALETTE["cyan"] + (255,),
    )
    pin.polygon(
        [
            (c - int(size * 0.18), int(size * 0.41)),
            (c + int(size * 0.18), int(size * 0.41)),
            (c, int(size * 0.83)),
        ],
        fill=PALETTE["cyan"] + (255,),
    )

    # Inner speedometer ring
    ring_r = int(size * 0.135)
    ring_box = [
        c - ring_r,
        pin_center[1] - ring_r,
        c + ring_r,
        pin_center[1] + ring_r,
    ]
    pin.ellipse(ring_box, fill=PALETTE["bg_bottom"] + (255,))
    pin.ellipse(
        [
            ring_box[0] + int(size * 0.018),
            ring_box[1] + int(size * 0.018),
            ring_box[2] - int(size * 0.018),
            ring_box[3] - int(size * 0.018),
        ],
        outline=PALETTE["cyan_soft"] + (255,),
        width=max(2, size // 40),
    )

    # Speed needle
    needle_end = (c + int(size * 0.09), pin_center[1] - int(size * 0.07))
    pin.line(
        [pin_center, needle_end],
        fill=PALETTE["white"] + (255,),
        width=max(3, size // 46),
    )
    pin.ellipse(
        [
            c - int(size * 0.015),
            pin_center[1] - int(size * 0.015),
            c + int(size * 0.015),
            pin_center[1] + int(size * 0.015),
        ],
        fill=PALETTE["white"] + (255,),
    )

    # Glow and merge
    glow = pin_layer.filter(ImageFilter.GaussianBlur(radius=max(2, size // 36)))
    icon.alpha_composite(glow)
    icon.alpha_composite(pin_layer)

    # Motion streak
    streak = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(streak, "RGBA")
    sdraw.polygon(
        [
            (int(size * 0.55), int(size * 0.73)),
            (int(size * 0.9), int(size * 0.58)),
            (int(size * 0.9), int(size * 0.66)),
            (int(size * 0.59), int(size * 0.78)),
        ],
        fill=PALETTE["cyan_soft"] + (95,),
    )
    icon.alpha_composite(streak.filter(ImageFilter.GaussianBlur(radius=max(2, size // 48))))

    return icon


def save_square(name: str, size: int, with_bg: bool = True) -> None:
    out = draw_speed_pin(size, with_bg=with_bg)
    out.save(ASSETS_DIR / name, "PNG")


if __name__ == "__main__":
    save_square("icon.png", 1024, with_bg=True)
    save_square("adaptive-icon.png", 1024, with_bg=False)
    save_square("splash-icon.png", 1024, with_bg=True)
    save_square("favicon.png", 256, with_bg=True)
    print(f"Generated icons in {ASSETS_DIR}")
