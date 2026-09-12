#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
create-new-pets.py — sáng tạo pet mới từ sprite mẫu gốc bằng:
  1. Hue-shift chọn vùng màu (lá→cà chua đỏ, xanh dương→tím trăng, đỏ→điện vàng...)
  2. Điểm xuyết chi tiết riêng (quả đỏ trên tán, tinh chấp sao, tia sét zigzag)
Giữ nguyên từng pixel gốc → nét = bản mẫu.
Output: design-demos/sprites/new/*.png + design-demos/new-pets.js
Chạy: python3 scripts/create-new-pets.py
"""
import os, json, math, random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = os.path.join(ROOT, "design-demos", "sprites", "ref")
OUT = os.path.join(ROOT, "design-demos", "sprites", "new")
os.makedirs(OUT, exist_ok=True)
random.seed(7)

def hsv_lut(rules):
    """rules: list of (h_lo, h_hi, t_lo, t_hi, s_mul, v_mul) — độ.
    Map dải hue [h_lo,h_hi] → [t_lo,t_hi] tuyến tính (wrap qua 360 được), S/V nhân."""
    lut_h = list(range(256)); lut_s = [255]*256; lut_v = [255]*256
    for h_lo, h_hi, t_lo, t_hi, s_mul, v_mul in rules:
        span = (h_hi - h_lo) % 360 or 360
        tspan = (t_hi - t_lo) % 360
        for h in range(256):
            v = (h * 360) / 255.0
            off = (v - h_lo) % 360
            if off <= span:
                new_v = (t_lo + off * (tspan / span)) % 360
                lut_h[h] = int(new_v * 255 / 360) % 256
                lut_s[h] = max(0, min(255, int(255 * s_mul)))
                lut_v[h] = max(0, min(255, int(255 * v_mul)))
    return lut_h, lut_s, lut_v

def recolor(src, dst, rules):
    im = Image.open(src).convert("RGBA")
    hsv = im.convert("HSV")
    h, s, v = hsv.split()
    lh, ls, lv = hsv_lut(rules)
    h = h.point(lh); s = s.point(ls); v = v.point(lv)
    out = Image.merge("HSV", (h, s, v)).convert("RGBA")
    out.putalpha(im.getchannel("A"))
    out.save(dst)
    return out

def on_body_points(im, n, y_frac=(0.05, 0.55), margin=8):
    """chọn n điểm ngẫu nhiên nằm trên vùng đục nửa trên sprite"""
    w, h = im.size
    pts = []
    tries = 0
    while len(pts) < n and tries < 4000:
        tries += 1
        x = random.randint(margin, w - margin - 1)
        y = random.randint(int(h * y_frac[0]), int(h * y_frac[1]))
        if im.getpixel((x, y))[3] > 200:
            pts.append((x, y))
    return pts

def add_dots(im, pts, color, r=3):
    """quả tròn nhỏ (cà chua trên tán)"""
    out = im.copy(); px = out.load()
    for (x, y) in pts:
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx*dx + dy*dy <= r*r:
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < out.width and 0 <= yy < out.height and px[xx, yy][3] > 200:
                        hi = dy < -r//2
                        px[xx, yy] = (color if not hi else tuple(min(255, c + 45) for c in color)) + (255,)
    return out

def add_sparkles(im, pts, color=(255, 255, 220)):
    """tinh chấp chữ thập 4 cánh"""
    out = im.copy(); px = out.load()
    for (x, y) in pts:
        for dx, dy in ((0,0),(1,0),(-1,0),(0,1),(0,-1)):
            xx, yy = x+dx, y+dy
            if 0 <= xx < out.width and 0 <= yy < out.height:
                px[xx, yy] = color + (255,)
    return out

def add_zigzag(im, start, length=22, amp=6, color=(255, 224, 90), thickness=2):
    out = im.copy(); px = out.load()
    x, y = start
    for i in range(length):
        yy = y + (amp if (i // 5) % 2 == 0 else -amp) + int(3 * math.sin(i / 3))
        for t in range(thickness):
            xx, yn = x + i, yy + t
            if 0 <= xx < out.width and 0 <= yn < out.height and px[xx, yn][3] >= 0:
                px[xx, yn] = color + (255,)
    return out

NEW_PETS = []

# ============ 1) DÒNG CÀ CHUA — pomodoro = cà chua 🍅 (biến thể hệ Thảo) ============
# lá xanh → đỏ cà chua; giữ vỏ thân nâu. Thêm quả đỏ trên tán/cây.
rules_tomato = [(78, 178, 348, 372, 1.15, 1.0)]   # xanh lá+spring → đỏ cà chua (wrap)
for src_key, key, ten, mốc, desc in [
    ("mam",        "chuaNhat",  "Chua Nhát",    1, "Hạt mầm cà chua khó chiều — nhưng ai cho ăn là dính"),
    ("bupXanh",    "caChot",    "Cà Chốt",      2, "Khủng long cà chua, nụ hoa hoá quả đầu tiên"),
    ("coThuLinh",  "coMocChua", "Cổ Mộc Chua",  3, "Cổ thụ sinh quả — tán đỏ đèn lồng"),
]:
    dst = os.path.join(OUT, f"{key}.png")
    im = recolor(os.path.join(REF, f"{src_key}.png"), dst, rules_tomato)
    if mốc >= 2:
        im = add_dots(im, on_body_points(im, 5 if mốc == 2 else 7, (0.08, 0.5)), (215, 40, 35), r=3 if mốc == 2 else 4)
        im.save(dst)
    NEW_PETS.append(dict(key=key, ten=ten, line="cachua", stage=mốc, file=f"sprites/new/{key}.png",
                         desc=desc, source=src_key))

# ============ 2) DÒNG NGUYỆT — trăng 🌙 (biến thể hệ Thủy) ============
# xanh dương → tím than/đêm + ánh bạc; thêm tinh chấp sao.
rules_moon = [(185, 255, 240, 285, 0.9, 0.95)]    # xanh dương → tím than đêm
for src_key, key, ten, mốc, desc in [
    ("giot",      "trangNon",  "Trăng Non",   1, "Giọt sương đựng ánh trăng — lấp lánh giá lạnh"),
    ("suoiVot",   "trieuNguyet","Triều Nguyệt",2, "Cá nước triều đêm, bơi trong bóng tối"),
    ("trieuLong", "nguyetLong","Nguyệt Long", 3, "Kình ngư nguyệt — sóng đêm cuộn trăng"),
]:
    dst = os.path.join(OUT, f"{key}.png")
    im = recolor(os.path.join(REF, f"{src_key}.png"), dst, rules_moon)
    im = add_sparkles(im, on_body_points(im, 6, (0.05, 0.6)))
    im.save(dst)
    NEW_PETS.append(dict(key=key, ten=ten, line="nguyet", stage=mốc, file=f"sprites/new/{key}.png",
                         desc=desc, source=src_key))

# ============ 3) THIÊN LÔI — lai Lửa×Thủy (spec §2.2) ============
# thân đỏ → vàng điện, lửa → tia điện; thêm zigzag sét.
im = recolor(os.path.join(REF, "tanLua.png"), os.path.join(OUT, "thienLoi.png"),
             [(335, 45, 42, 68, 1.25, 1.1), (45, 60, 55, 70, 1.15, 1.05)])  # đỏ+cam → vàng điện
im = add_zigzag(im, (im.width - 26, 8), length=20, amp=5)
im = add_zigzag(im, (im.width - 18, 26), length=16, amp=4)
im.save(os.path.join(OUT, "thienLoi.png"))
NEW_PETS.append(dict(key="thienLoi", ten="Thiên Lôi", line="lai", stage=0, file="sprites/new/thienLoi.png",
                     desc="Lai Lửa×Thủy — hồ ly sét, đuôi tia chớp (spec §2.2)", source="tanLua"))

# ============ 4) DIỄM HOA — lai Thảo×Lửa (spec §2.2) ============
# lá xanh → đỏ lửa; hoa hồng giữ, quả thành than hồng
recolor(os.path.join(REF, "bupXanh.png"), os.path.join(OUT, "diemHoa.png"),
        [(78, 178, 15, 45, 1.25, 1.0)])   # xanh lá+spring → cam-lửa, hoa giữ hồng
NEW_PETS.append(dict(key="diemHoa", ten="Diễm Hoa", line="lai", stage=0, file="sprites/new/diemHoa.png",
                     desc="Lai Thảo×Lửa — florasaur than hồng (spec §2.2)", source="bupXanh"))

# ============ 5) SEN MƯA — lai Thủy×Thảo (spec §2.2) ============
# xanh dương → xanh sen; vây vàng → cánh sen hồng
recolor(os.path.join(REF, "suoiVot.png"), os.path.join(OUT, "senMua.png"),
        [(172, 255, 140, 205, 1.0, 0.88), (40, 65, 310, 340, 1.1, 0.95)])  # xanh → xanh sen đậm, vây vàng → cánh sen hồng
NEW_PETS.append(dict(key="senMua", ten="Sen Mưa", line="lai", stage=0, file="sprites/new/senMua.png",
                     desc="Lai Thủy×Thảo — cá sen dặm mưa (spec §2.2)", source="suoiVot"))

with open(os.path.join(ROOT, "design-demos", "new-pets.js"), "w") as f:
    f.write("/* AUTO-GEN bởi scripts/create-new-pets.py — pet mới sáng tạo */\n"
            "globalThis.NEW_PETS = " + json.dumps(NEW_PETS, ensure_ascii=False, indent=2) + ";\n")
print(f"OK — {len(NEW_PETS)} pet mới → design-demos/sprites/new/")
