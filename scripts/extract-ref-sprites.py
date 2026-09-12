#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
extract-ref-sprites.py (v2) — tách sprite từ ảnh mẫu bằng hộp cắt định sẵn,
xóa nền flood-fill từ mép hộp, trim, xuất PNG trong suốt + manifest JS.
Chạy: python3 scripts/extract-ref-sprites.py
"""
import os, json
from PIL import Image
from collections import deque

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = os.path.join(ROOT, "design-demos", "reference")
OUT = os.path.join(ROOT, "design-demos", "sprites", "ref")
os.makedirs(OUT, exist_ok=True)

# (file, box(x0,y0,x1,y1), key, tên Việt, line, stage, view)
CROPS = [
    # 🔥 dòng hồ ly — thu-lua-fire-fox.png
    ("thu-lua-fire-fox.png", (40, 62, 168, 222),   "tanLua",        "Tàn Lửa",      "lua", 1, "front"),
    ("thu-lua-fire-fox.png", (172, 58, 292, 226),  "tanLua-back",   "Tàn Lửa",      "lua", 1, "back"),
    ("thu-lua-fire-fox.png", (312, 58, 432, 226),  "tanLua-shiny",  "Tàn Lửa shiny","lua", 1, "front"),
    ("thu-lua-fire-fox.png", (512, 52, 640, 225),  "bongBot",       "Bồng Bột",     "lua", 2, "front"),
    ("thu-lua-fire-fox.png", (660, 52, 785, 225),  "bongBot-back",  "Bồng Bột",     "lua", 2, "back"),
    ("thu-lua-fire-fox.png", (820, 52, 965, 225),  "bongBot-shiny", "Bồng Bột shiny","lua", 2, "front"),
    ("thu-lua-fire-fox.png", (448, 298, 645, 497), "diemLong",      "Diễm Long",    "lua", 3, "front"),
    ("thu-lua-fire-fox.png", (650, 298, 795, 485), "diemLong-back", "Diễm Long",    "lua", 3, "back"),
    ("thu-lua-fire-fox.png", (818, 298, 1005, 495),"diemLong-shiny","Diễm Long shiny","lua", 3, "front"),
    ("thu-lua-fire-fox.png", (32, 318, 160, 492),  "bongBot-alt",   "Bồng Bột (pose 2)", "lua", 2, "front"),

    # 💧 dòng cá — moc-tien-hoa-fish.jpg
    ("moc-tien-hoa-fish.jpg", (85, 452, 330, 670),   "giot",      "Giọt",      "thuy", 1, "front"),
    ("moc-tien-hoa-fish.jpg", (455, 425, 815, 700),  "suoiVot",   "Suối Vọt",  "thuy", 2, "front"),
    ("moc-tien-hoa-fish.jpg", (865, 215, 1265, 725), "trieuLong", "Triều Long","thuy", 3, "front"),

    # 🌿 dòng mầm + hệ khác — he-khac-lai-tao.png
    ("he-khac-lai-tao.png", (362, 118, 428, 228),  "mam",        "Mầm",        "thao", 1, "front"),
    ("he-khac-lai-tao.png", (428, 50, 542, 228),   "bupXanh",    "Búp Xanh",   "thao", 2, "front"),
    ("he-khac-lai-tao.png", (542, 55, 682, 228),   "coThuLinh",  "Cổ Thụ Linh","thao", 3, "front"),
    # hệ phụ (thư giãn)
    ("he-khac-lai-tao.png", (30, 88, 132, 222),    "aquawoof",   "Aquawoof",   "nuoccho", 1, "front"),
    ("he-khac-lai-tao.png", (138, 52, 238, 222),   "tidewolf",   "Tidewolf",   "nuoccho", 2, "front"),
    ("he-khac-lai-tao.png", (238, 42, 352, 225),   "gigaHydro",  "Giga-Hydro", "nuoccho", 3, "front"),
    ("he-khac-lai-tao.png", (682, 100, 762, 228),  "zapPup",     "Zap-Pup",    "dien", 1, "front"),
    ("he-khac-lai-tao.png", (762, 50, 882, 222),   "voltFox",    "Volt-Fox",   "dien", 2, "front"),
    ("he-khac-lai-tao.png", (882, 40, 1005, 228),  "thundros",   "Thundros",   "dien", 3, "front"),
    ("he-khac-lai-tao.png", (38, 362, 195, 522),   "aquaSprout", "Aqua-Sprout","lai", 0, "front"),
    ("he-khac-lai-tao.png", (650, 325, 805, 505),  "infernoOak", "Inferno-Oak","lai", 0, "front"),
    ("he-khac-lai-tao.png", (840, 380, 1005, 515), "sproutZap",  "Sprout-Zap", "lai", 0, "front"),
]

def remove_bg(im, tol=32):
    """flood-fill nền từ mép crop → trong suốt"""
    im = im.convert("RGBA")
    W, H = im.size
    px = im.load()
    # nền = pixel SÁNG NHẤT trên vòng biên (nền sheet luôn sáng; biên có thể
    # chạm vạch phân cách/nhãn màu xám đậm — lấy max luminance để khỏi sample nhầm)
    cand = ([px[x, 0][:3] for x in range(W)] + [px[x, H-1][:3] for x in range(W)] +
            [px[0, y][:3] for y in range(H)] + [px[W-1, y][:3] for y in range(H)])
    bg = max(cand, key=lambda c: c[0] + c[1] + c[2])
    def near(x, y):
        r, g, b, _ = px[x, y]
        return abs(r-bg[0]) + abs(g-bg[1]) + abs(b-bg[2]) <= tol * 3
    seen = [[False]*W for _ in range(H)]
    dq = deque()
    for x in range(W):
        for y in (0, H-1):
            if near(x, y) and not seen[y][x]: seen[y][x] = True; dq.append((x, y))
    for y in range(H):
        for x in (0, W-1):
            if near(x, y) and not seen[y][x]: seen[y][x] = True; dq.append((x, y))
    while dq:
        x, y = dq.popleft()
        px[x, y] = (px[x, y][0], px[x, y][1], px[x, y][2], 0)
        for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
            nx, ny = x+dx, y+dy
            if 0 <= nx < W and 0 <= ny < H and not seen[ny][nx] and near(nx, ny):
                seen[ny][nx] = True; dq.append((nx, ny))
    return im

def drop_debris(im, min_px=800):
    """Xóa các mảnh mờ nhỏ (debris từ graphic khác dính vào hộp crop)"""
    W, H = im.size
    px = im.load()
    seen = [[False]*W for _ in range(H)]
    comps = []
    for y0 in range(H):
        for x0 in range(W):
            if px[x0, y0][3] > 0 and not seen[y0][x0]:
                q = deque([(x0, y0)]); seen[y0][x0] = True
                cells = []
                while q:
                    x, y = q.popleft(); cells.append((x, y))
                    for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                        nx, ny = x+dx, y+dy
                        if 0 <= nx < W and 0 <= ny < H and not seen[ny][nx] and px[nx, ny][3] > 0:
                            seen[ny][nx] = True; q.append((nx, ny))
                comps.append(cells)
    if not comps:
        return im
    biggest = max(len(c) for c in comps)
    for cells in comps:
        if len(cells) < max(min_px, biggest * 0.03):
            for x, y in cells: px[x, y] = (0, 0, 0, 0)
    return im

def trim(im):
    bbox = im.getbbox()
    return im.crop(bbox) if bbox else im

def main():
    manifest = []
    cache = {}
    for fname, box, key, ten, line, stage, view in CROPS:
        if fname not in cache:
            cache[fname] = Image.open(os.path.join(REF, fname)).convert("RGB")
        im = cache[fname].crop(box)
        im = remove_bg(im, tol=40 if fname.lower().endswith(".jpg") else 32)
        im = drop_debris(im)
        im = trim(im)
        out = os.path.join(OUT, f"{key}.png")
        im.save(out)
        w, h = im.size
        # bản 64px cao nhất của từng mốc (grid game dự kiến)
        target = {1: 48, 2: 64, 3: 80}.get(stage, 64)
        sc = target / max(w, h)
        if sc < 1:
            im.resize((max(1,int(w*sc)), max(1,int(h*sc))), Image.LANCZOS).save(
                os.path.join(OUT, f"{key}-{target}.png"))
        manifest.append(dict(key=key, file=f"sprites/ref/{key}.png", ten=ten,
                             line=line, stage=stage, view=view, w=w, h=h))
        print(f"✓ {key}.png {w}×{h}")
    with open(os.path.join(ROOT, "design-demos", "ref-sprites.js"), "w") as f:
        f.write("/* AUTO-GEN bởi scripts/extract-ref-sprites.py */\n"
                "globalThis.REF_SPRITES = " + json.dumps(manifest, ensure_ascii=False, indent=2) + ";\n")
    print(f"\n{len(manifest)} sprite → {OUT} · manifest: design-demos/ref-sprites.js")

if __name__ == "__main__":
    main()
