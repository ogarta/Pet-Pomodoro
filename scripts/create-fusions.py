#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
create-fusions.py — ghép ĐẦU sprite này lên THÂN sprite khác (cùng tier, cùng mật độ
pixel, cùng hướng nhìn) ở độ phân giải gốc ⇒ sinh vật hoàn toàn mới nhưng giữ nét mẫu.
Xử lý: cắt đầu theo hộp %, giãn canvas, xoá đầu cũ của thân (ngoài phủ đầu mới),
viền phân cách, thêm 1 chi tiết đặc trưng (sét/tinh chấp).
Output: design-demos/sprites/new/*.png + cập nhật new-pets.js (line="fusion")
Chạy: python3 scripts/create-fusions.py
"""
import os, json, math, random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = os.path.join(ROOT, "design-demos", "sprites", "ref")
OUT = os.path.join(ROOT, "design-demos", "sprites", "new")
random.seed(11)

def load(k):
    return Image.open(os.path.join(REF, k + ".png")).convert("RGBA")

def frac_crop(im, fx0, fy0, fx1, fy1):
    w, h = im.size
    return im.crop((int(w*fx0), int(h*fy0), int(w*fx1), int(h*fy1)))

def dilate_mask(im, r=2):
    """nới vùng alpha>0 thêm r px (trả về mask set)"""
    w, h = im.size
    px = im.load()
    m = set()
    for y in range(h):
        for x in range(w):
            if px[x, y][3] > 0:
                for dy in range(-r, r+1):
                    for dx in range(-r, r+1):
                        m.add((x+dx, y+dy))
    return m

def transplant(body, head, head_box, anchor, erase_above=True):
    """body/head: RGBA. head_box: (fx0,fy0,fx1,fy1) vùng đầu trên head sprite.
    anchor: (fx, fy) điểm ĐÁY GIỮA đầu mới trên toạ độ thân."""
    bw, bh = body.size
    head_c = frac_crop(head, *head_box)
    hw, hh = head_c.size
    ax, ay = int(bw * anchor[0]), int(bh * anchor[1])
    top = ay - hh
    left = ax - hw // 2
    # giãn canvas lên trên/trái/phải nếu cần
    pad_top = max(0, -top); pad_left = max(0, -left)
    pad_right = max(0, left + hw - bw)
    nW, nH = bw + pad_left + pad_right, bh + pad_top
    canvas = Image.new("RGBA", (nW, nH), (0, 0, 0, 0))
    canvas.paste(body, (pad_left, pad_top))
    # xoá đầu cũ của thân: phần thân đục nằm TRÊN đáy đầu mới & ngoài phủ đầu mới
    if erase_above:
        mask = dilate_mask(head_c, 3)
        cpx = canvas.load()
        cut_y = pad_top + ay + 3
        for y in range(pad_top, min(cut_y, nH)):
            for x in range(nW):
                pxv = cpx[x, y]
                if pxv[3] > 0 and (x - left, y - top) not in mask:
                    cpx[x, y] = (0, 0, 0, 0)
    canvas.alpha_composite(head_c, (left + pad_left, top + pad_top))
    return canvas

def add_zigzag(im, start, length=20, amp=5, color=(255, 224, 90), thickness=2):
    out = im.copy(); px = out.load()
    x, y = start
    for i in range(length):
        yy = y + (amp if (i // 5) % 2 == 0 else -amp) + int(2 * math.sin(i / 3))
        for t in range(thickness):
            xx, yn = x + i, yy + t
            if 0 <= xx < out.width and 0 <= yn < out.height:
                px[xx, yn] = color + (255,)
    return out

def add_sparkles(im, pts, color=(230, 245, 255)):
    out = im.copy(); px = out.load()
    for (x, y) in pts:
        for dx, dy in ((0,0),(1,0),(-1,0),(0,1),(0,-1)):
            xx, yy = x+dx, y+dy
            if 0 <= xx < out.width and 0 <= yy < out.height:
                px[xx, yy] = color + (255,)
    return out

FUSIONS = [
    dict(
        key="loiMoc", ten="Lôi Mộc", spec="Thundros (đầu sừng sét) + Inferno-Oak (thân gỗ thú)",
        body="infernoOak", head="thundros",
        head_box=(0.00, 0.02, 0.60, 0.46), anchor=(0.48, 0.44),
        feature="zigzag", desc="Quái cây gỗ sét — đầu Hung Thú sừng vàng, thân cổ thụ cuồng phong"),
    dict(
        key="bangViem", ten="Băng Viêm", spec="Giga-Hydro (đầu băng) + Bồng Bột (thân hồ ly lửa)",
        body="bongBot", head="gigaHydro",
        head_box=(0.00, 0.00, 0.58, 0.48), anchor=(0.40, 0.36),
        feature="sparkle", desc="Hồ ly băng-hỏa — đầu sói băng giá, thân lửa Ports"),
    dict(
        key="phongLoi", ten="Phong Lôi", spec="Tidewolf (đầu bờm gió) + Volt-Fox (thân hồ ly sét)",
        body="voltFox", head="tidewolf",
        head_box=(0.00, 0.00, 0.62, 0.44), anchor=(0.38, 0.34),
        feature="zigzag", desc="Sói bão — bờm gió xanh, thân thần sét"),
]

def main():
    entries = []
    for f in FUSIONS:
        body = load(f["body"]); head = load(f["head"])
        im = transplant(body, head, f["head_box"], f["anchor"])
        if f["feature"] == "zigzag":
            im = add_zigzag(im, (int(im.width*0.80), int(im.height*0.05)), length=16, amp=4)
        elif f["feature"] == "sparkle":
            im = add_sparkles(im, [(int(im.width*0.15), int(im.height*0.18)),
                                   (int(im.width*0.82), int(im.height*0.12)),
                                   (int(im.width*0.7), int(im.height*0.45))])
        out = os.path.join(OUT, f["key"] + ".png")
        im.save(out)
        entries.append(dict(key=f["key"], ten=f["ten"], line="fusion", stage=0,
                            file=f"sprites/new/{f['key']}.png", desc=f["desc"], spec=f["spec"]))
        print(f"✓ {f['key']}.png {im.size}")

    # ghép vào new-pets.js (thay các entry line=fusion cũ, giữ các entry khác)
    mf = os.path.join(ROOT, "design-demos", "new-pets.js")
    txt = open(mf).read() if os.path.exists(mf) else ""
    try:
        arr = json.loads(txt.split("= ", 1)[1].rstrip().rstrip(";"))
    except Exception:
        arr = []
    arr = [e for e in arr if e.get("line") != "fusion"] + entries
    with open(mf, "w") as f:
        f.write("/* AUTO-GEN: create-new-pets.py + create-fusions.py */\n"
                "globalThis.NEW_PETS = " + json.dumps(arr, ensure_ascii=False, indent=2) + ";\n")
    print(f"manifest: {len(arr)} pet mới (bao gồm {len(entries)} fusion)")

if __name__ == "__main__":
    main()
