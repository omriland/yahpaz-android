#!/usr/bin/env python3
"""Generate launcher mipmaps and Play Console graphics from the iOS App Icon."""

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
IOS_ICON = ROOT.parent / "yahpaz-ios/App/Assets.xcassets/AppIcon.appiconset/AppIcon.png"
NAVY = (0x1D, 0x4E, 0x89, 255)

MIPMAPS = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def rounded(image: Image.Image, radius_ratio: float = 0.22) -> Image.Image:
    image = image.convert("RGBA")
    mask = Image.new("L", image.size, 0)
    draw = ImageDraw.Draw(mask)
    radius = int(min(image.size) * radius_ratio)
    draw.rounded_rectangle((0, 0, image.width, image.height), radius=radius, fill=255)
    image.putalpha(mask)
    return image


def main() -> None:
    source = Image.open(IOS_ICON).convert("RGBA")
    res = ROOT / "app/src/main/res"
    for folder, size in MIPMAPS.items():
        dest_dir = res / folder
        dest_dir.mkdir(parents=True, exist_ok=True)
        launcher = source.resize((size, size), Image.Resampling.LANCZOS)
        launcher.save(dest_dir / "ic_launcher.png")
        launcher.save(dest_dir / "ic_launcher_round.png")

    foreground_dir = res / "drawable"
    foreground_dir.mkdir(parents=True, exist_ok=True)
    source.resize((432, 432), Image.Resampling.LANCZOS).save(foreground_dir / "ic_launcher_foreground.png")

    play = ROOT / "play/assets"
    play.mkdir(parents=True, exist_ok=True)
    source.resize((512, 512), Image.Resampling.LANCZOS).save(play / "icon-512.png")

    feature = Image.new("RGBA", (1024, 500), NAVY)
    icon = rounded(source.resize((320, 320), Image.Resampling.LANCZOS))
    feature.paste(icon, ((1024 - 320) // 2, (500 - 320) // 2), icon)
    feature.convert("RGB").save(play / "feature-graphic-1024x500.png")
    print(f"Wrote launcher mipmaps and {play}")


if __name__ == "__main__":
    main()
