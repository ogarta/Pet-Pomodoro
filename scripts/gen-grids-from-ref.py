#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gen-grids-from-ref.py — sinh grid game-ready (32/40/48) bằng cách downscale
sprite chuẩn (sprites/ref/*.png) + map màu về palette từng hệ.
Output:
  design-demos/grids-from-ref/<key>-<N>.png  (grid pixel N×N)
  design-demos/ref-grids.js                  (globalThis.REF_GRIDS — data strings cho sprites.ts)
Chạy: python3 scripts/gen-grids-from-ref.py
"""
import os, json
from PIL import Image
from collections import deque

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = os.path.join(ROOT, "design-demos", "sprites", "ref")
OUT = os.path.join(ROOT, "design-demos", "grids-from-ref")
os.makedirs(OUT, exist_ok=True)

# palette hex từng hệ (đồng bộ sprite-data.js) — dùng để map màu → ký tự
PALETTES = {
  "fire": {"o":"#4A1410","d":"#7E1F16","f":"#B5382A","l":"#E06038","h":"#F5A05A","c":"#F2E5C8","C":"#D9BE9A",
           "v":"#5A1810","r":"#D9452B","y":"#F28C28","Y":"#F7C948","W":"#FFF6D8","k":"#143C3C","t":"#3FA8A0","w":"#FFFFFF"},
  "fireShiny": {"o":"#232B36","d":"#46566E","f":"#8C9BB0","l":"#B8C4D4","h":"#DDE6EE","c":"#E8E9F2","C":"#BFC2D4",
                "v":"#1E3B3C","r":"#1F8F84","y":"#2EC4B6","Y":"#7FE8DE","W":"#E8FFF9","k":"#143C3C","t":"#E8B84A","w":"#FFFFFF"},
  "water": {"o":"#16283E","d":"#24365C","f":"#35507E","l":"#5B7CB0","h":"#8FA8CE","c":"#C7D8EE","C":"#9FB4D0",
            "a":"#E8C860","A":"#B89030","b":"#A8D8F0","B":"#5FA8CC","k":"#101820","t":"#F0C040","w":"#FFFFFF"},
  "grass": {"o":"#1E3A1C","d":"#3E7A34","f":"#5FA84A","l":"#93D06E","h":"#B8E88A","c":"#D8EFC0","C":"#A8C888",
            "u":"#4E8A5E","U":"#37634A","p":"#F0A0B8","P":"#D06890","m":"#8A6242","M":"#63452E","k":"#142014","w":"#FFFFFF"},
}

# key → (size, palette)
TARGETS = {
  "tanLua": (32, "fire"),        "tanLua-back": (32, "fire"),   "tanLua-shiny": (32, "fireShiny"),
  "bongBot": (40, "fire"),       "bongBot-back": (40, "fire"),  "bongBot-shiny": (40, "fireShiny"),
  "bongBot-alt": (40, "fire"),
  "diemLong": (48, "fire"),      "diemLong-back": (48, "fire"), "diemLong-shiny": (48, "fireShiny"),
  "giot": (32, "water"),         "suoiVot": (40, "water"),      "trieuLong": (48, "water"),
  "mam": (32, "grass"),          "bupXanh": (40, "grass"),      "coThuLinh": (48, "grass"),
}

def hex2rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))

def nearest_char(rgb, pal_rgb):
    best, bd = ".", 10**9
    for ch, c in pal_rgb.items():
        d = sum((a-b)**2 for a, b in zip(rgb, c))
        if d < bd:
            bd, best = d, ch
    return best

def make_grid(src, size, pal_name, alpha_cut=110):
    im = Image.open(src).convert("RGBA")
    # DEFRINGE: pixel trong suốt mang màu nền cũ → khi resize sẽ loang ra rìa.
    # Tô RGB của chúng = màu viền để rìa hoà về outline (chuẩn pixel art).
    oc = hex2rgb(PALETTES[pal_name].get("o", "#000000"))
    p0 = im.load()
    for yy in range(im.height):
        for xx in range(im.width):
            if p0[xx, yy][3] == 0:
                p0[xx, yy] = (oc[0], oc[1], oc[2], 0)
    w, h = im.size
    sc = size / max(w, h)
    nw, nh = max(1, round(w*sc)), max(1, round(h*sc))
    # tách alpha để tránh halo khi thu nhỏ
    r, g, b, a = im.split()
    rgb = Image.merge("RGB", (r, g, b)).resize((nw, nh), Image.LANCZOS)
    a2 = a.resize((nw, nh), Image.LANCZOS)
    # alpha ngưỡng → silhouette gắt
    a2 = a2.point(lambda v: 255 if v >= alpha_cut else 0)
    rgb.putalpha(a2)

    # giới hạn màu: quantize vùng đục để bớt nhiễu rồi map về palette
    pal = PALETTES[pal_name]
    pal_rgb = {ch: hex2rgb(hx) for ch, hx in pal.items()}

    px = rgb.load()
    rows = []
    png = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out_px = png.load()
    ox, oy = (size - nw) // 2, (size - nh) // 2   # căn giữa canvas
    for y in range(nh):
        row = []
        for x in range(nw):
            rr, gg, bb, aa = px[x, y]
            if aa == 0:
                row.append(".")
                continue
            ch = nearest_char((rr, gg, bb), pal_rgb)
            row.append(ch)
            out_px[ox + x, oy + y] = hex2rgb(pal[ch]) + (255,)
        rows.append("".join(row))
    # pad đủ N hàng, căn giữa dọc
    pad_top = (size - len(rows)) // 2
    rows = ["." * size] * pad_top + rows + ["." * size] * (size - len(rows) - pad_top)
    return png, rows

def main():
    manifest = []
    for key, (size, pal_name) in TARGETS.items():
        src = os.path.join(REF, f"{key}.png")
        png, rows = make_grid(src, size, pal_name)
        out = os.path.join(OUT, f"{key}-{size}.png")
        png.save(out)
        line = {"tanLua": "lua", "tanLua-back": "lua", "tanLua-shiny": "lua",
                "bongBot": "lua", "bongBot-back": "lua", "bongBot-shiny": "lua", "bongBot-alt": "lua",
                "diemLong": "lua", "diemLong-back": "lua", "diemLong-shiny": "lua",
                "giot": "thuy", "suoiVot": "thuy", "trieuLong": "thuy",
                "mam": "thao", "bupXanh": "thao", "coThuLinh": "thao"}[key]
        stage = {32: 1, 40: 2, 48: 3}[size]
        manifest.append(dict(key=key, line=line, stage=stage, size=size, palette=pal_name, grid=rows))
        png.resize((size*6, size*6), Image.NEAREST).save(out.replace(".png", "-x6.png"))
        print(f"✓ {key}-{size}.png")
    with open(os.path.join(ROOT, "design-demos", "ref-grids.js"), "w") as f:
        f.write("/* AUTO-GEN bởi scripts/gen-grids-from-ref.py — grid game downscale từ ảnh mẫu */\n"
                "globalThis.REF_GRIDS = " + json.dumps(manifest, ensure_ascii=False, indent=2) + ";\n")
    print(f"\n{len(manifest)} grid → design-demos/grids-from-ref/ · design-demos/ref-grids.js")

if __name__ == "__main__":
    main()
