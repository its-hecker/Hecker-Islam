#!/usr/bin/env python3
"""Rasterize the app's existing Android VectorDrawable for APK icon previews.

Android launchers can render VectorDrawable XML, but some APK file browsers
expect bitmap mipmap entries. The output is derived from the same committed
vector artwork and needs no design assets or private signing credentials.
Requires CairoSVG; run before Gradle resource compilation.
"""
from __future__ import annotations

from html import escape
from pathlib import Path
import xml.etree.ElementTree as ET

import cairosvg

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/drawable/ic_azaan.xml"
RES = ROOT / "app/src/main/res"
ANDROID = "{http://schemas.android.com/apk/res/android}"
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def svg_color(color: str, name: str) -> str:
    if color.startswith("#") and len(color) == 9:
        alpha = int(color[1:3], 16) / 255
        return f'{name}="#{color[3:]}" {name}-opacity="{alpha:.4f}"'
    return f'{name}="{escape(color, quote=True)}"'


def main() -> None:
    root = ET.parse(SOURCE).getroot()
    width = float(root.attrib[ANDROID + "viewportWidth"])
    height = float(root.attrib[ANDROID + "viewportHeight"])
    paths = []
    for path in root:
        d = path.attrib.get(ANDROID + "pathData")
        if not d:
            continue
        styles = [f'd="{escape(d, quote=True)}"']
        fill = path.attrib.get(ANDROID + "fillColor", "#00000000")
        styles.append(svg_color(fill, "fill"))
        stroke = path.attrib.get(ANDROID + "strokeColor")
        if stroke:
            styles.append(svg_color(stroke, "stroke"))
            styles.append(f'stroke-width="{path.attrib.get(ANDROID + "strokeWidth", "1")}"')
            styles.append(f'stroke-linejoin="{path.attrib.get(ANDROID + "strokeLineJoin", "miter")}"')
        paths.append("<path " + " ".join(styles) + " />")

    content = "\n".join(paths)
    def svg(round_icon: bool) -> bytes:
        if round_icon:
            drawing = '<defs><clipPath id="circle"><circle cx="54" cy="54" r="54"/></clipPath></defs><g clip-path="url(#circle)">' + content + "</g>"
        else:
            drawing = content
        return (
            '<svg xmlns="http://www.w3.org/2000/svg" '
            f'width="{width}" height="{height}" viewBox="0 0 {width} {height}">'
            + drawing + "</svg>"
        ).encode("utf-8")

    for density, size in DENSITIES.items():
        folder = RES / ("mipmap-" + density)
        folder.mkdir(parents=True, exist_ok=True)
        for name, rounded in (("ic_launcher.png", False), ("ic_launcher_round.png", True)):
            target = folder / name
            cairosvg.svg2png(
                bytestring=svg(rounded),
                write_to=str(target),
                output_width=size,
                output_height=size,
            )
            if not target.exists() or target.stat().st_size < 500:
                raise RuntimeError("Icon generation failed: " + str(target))
            print(f"Created {target.relative_to(ROOT)} ({size}x{size})")


if __name__ == "__main__":
    main()
