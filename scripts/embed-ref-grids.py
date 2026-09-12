#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""embed-ref-grids.py — hoán đổi grid pet tay vẽ (chuẩn DMG/v3 cũ) trong
design-demos/kich-ban-1-hanh-trinh-tien-hoa.html bằng grid game-ready
32/40/48 downscale từ sprite chuẩn tách ảnh mẫu user duyệt
(sprites/ref/*.png → gen-grids-from-ref.py) + grid ốm §2.4 suy ra từ Tàn Lửa
(scripts/make-sick-grid.py).

Chỉ đụng vùng dữ liệu sprite (giữa `const INK` và comment `/* §2.4 điểm 4`),
KHÔNG đụng layout/config/gameplay. Chạy: python3 scripts/embed-ref-grids.py
"""
import json, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HTML = os.path.join(ROOT, 'design-demos', 'kich-ban-1-hanh-trinh-tien-hoa.html')
REF = os.path.join(ROOT, 'design-demos', 'ref-grids.js')
SICK = os.path.join(ROOT, 'design-demos', 'grids-from-ref', 'tanLua-sick.json')

# palette hex từng hệ — đồng bộ scripts/gen-grids-from-ref.py
FIRE = {"o":"#4A1410","d":"#7E1F16","f":"#B5382A","l":"#E06038","h":"#F5A05A","c":"#F2E5C8","C":"#D9BE9A",
        "v":"#5A1810","r":"#D9452B","y":"#F28C28","Y":"#F7C948","W":"#FFF6D8","k":"#143C3C","t":"#3FA8A0","w":"#FFFFFF"}
WATER = {"o":"#16283E","d":"#24365C","f":"#35507E","l":"#5B7CB0","h":"#8FA8CE","c":"#C7D8EE","C":"#9FB4D0",
         "a":"#E8C860","A":"#B89030","b":"#A8D8F0","B":"#5FA8CC","k":"#101820","t":"#F0C040","w":"#FFFFFF"}
GRASS = {"o":"#1E3A1C","d":"#3E7A34","f":"#5FA84A","l":"#93D06E","h":"#B8E88A","c":"#D8EFC0","C":"#A8C888",
         "u":"#4E8A5E","U":"#37634A","p":"#F0A0B8","P":"#D06890","m":"#8A6242","M":"#63452E","k":"#142014","w":"#FFFFFF"}
# §2.4 điểm 4: palette NHỢT/desaturate của line Lửa — outline tối vẫn giữ
FIRE_SICK = {"o":"#3A322C","d":"#8A7364","f":"#B49B85","l":"#CBB8A4","h":"#DECDB8","c":"#EFE7D6","C":"#D6C8B2",
             "v":"#463A31","r":"#A98F7A","y":"#C9BBA6","Y":"#DDD3C0","W":"#F7F3EA","k":"#2E2823","t":"#9FB3AC",
             "w":"#FFFFFF","a":"#B8AFA2","e":"#8E8578"}

# key grid → tên const trong HTML (khớp CATALOG đang tham chiếu)
GRIDS = [
    ('TAN_LUA',     'tanLua',     FIRE, '24→32×32 — tròn nhỏ, ngố, tai to, đuôi lửa nhỏ (sprite mẫu: Petyre)'),
    ('BONG_BOT',    'bongBot',    FIRE, '40×40 — cao + gấu gầy, bờm lửa, mắt dữ, lửa to (sprite mẫu: Fyreox)'),
    ('DIEM_LONG',   'diemLong',   FIRE, '48×48 — rồng lửa oai, bờm lửa, dáng rồng (sprite mẫu: Infernyx)'),
    ('GIOT',        'giot',       WATER, '32×32 — cá nước con tròn ngố (sprite mẫu: Shock-Fringe)'),
    ('SUOI_VOT',    'suoiVot',    WATER, '40×40 — cá nha dữ hơn, vây vàng phát triển (sprite mẫu: Volt-Tide)'),
    ('TRIEU_LONG',  'trieuLong',  WATER, '48×48 — thuồng luồng sóng, vây lưng gai (sprite mẫu: Tide-Striker)'),
    ('MAM',         'mam',        GRASS, '32×32 — mầm lá tròn nhỏ (sprite mẫu: Sproutling)'),
    ('BUP_XANH',    'bupXanh',    GRASS, '40×40 — hươu lá nụ hồng (sprite mẫu: Florasaur)'),
    ('CO_THU_LINH', 'coThuLinh',  GRASS, '48×48 — linh thú cây cổ thụ nở tán (sprite mẫu: Ancient-Oak)'),
]

def js_palette(name, pal, comment):
    rows = ', '.join(f"{k}:'{v}'" for k, v in pal.items())
    return f"  {name}: {{ {rows} }}, // {comment}"

def main():
    src = open(REF).read()
    data = json.loads(src.split('=', 1)[1].rsplit(';', 1)[0])
    by_key = {d['key']: d['grid'] for d in data}
    sick_grid = json.load(open(SICK))['grid']

    def pad(grid):
        w = max(len(r) for r in grid)
        return [r.ljust(w, '.') for r in grid]

    by_key = {k: pad(v) for k, v in by_key.items()}
    sick_grid = pad(sick_grid)

    # ---- verify palette coverage trước khi embed ----
    problems = []
    for const, key, pal, _ in GRIDS:
        used = set(''.join(by_key[key])) - {'.'}
        missing = used - set(pal)
        if missing:
            problems.append((key, sorted(missing)))
    used = set(''.join(sick_grid)) - {'.'}
    missing = used - set(FIRE_SICK)
    if missing:
        problems.append(('tanLua-sick', sorted(missing)))
    if problems:
        raise SystemExit(f'palette missing chars: {problems}')

    out = []
    out.append('/* ================= PALETTE 3 HỆ (đồng bộ gen-grids-from-ref.py) =================')
    out.append('   Outline "o" = nâu-đen gần đen · thân 2–3 tông + highlight · bụng kem ·')
    out.append('   ký tự khói a/e cho biến thể ốm (nguyên tố tắt theo §2.4). */')
    out.append('const FIRE_PAL  = ' + json.dumps(FIRE, ensure_ascii=False) + ';')
    out.append('const WATER_PAL = ' + json.dumps(WATER, ensure_ascii=False) + ';')
    out.append('const GRASS_PAL = ' + json.dumps(GRASS, ensure_ascii=False) + ';')
    out.append('const FIRE_SICK_PAL = ' + json.dumps(FIRE_SICK, ensure_ascii=False) + ';')
    out.append('')
    out.append('/* ================= GRID SPRITE — GAME-READY 32/40/48 =================')
    out.append('   Nguồn: downscale từ sprite chuẩn tách ảnh mẫu user duyệt (direction-approved.md,')
    out.append('   sprites/ref/*.png qua scripts/gen-grids-from-ref.py) — THAM CHIẾU Pokemon thật:')
    out.append('   Lửa Charmander→Charmeleon→Charizard · Thủy Squirtle→Wartortle→Blastoise ·')
    out.append('   Thảo Bulbasaur→Ivysaur→Venusaur. Giữ tên sinh vật Việt (spec §2.1).')
    out.append('   ĐỘ PHỨC TẠP TĂNG THEO STAGE: 32 (tròn nhỏ ngố) → 40 (cao, chi tiết hơn, mắt dữ)')
    out.append('   → 48 (đổi hẳn dáng huyền thoại: rồng / thuồng luồng / cổ thụ). */')

    for const, key, pal, comment in GRIDS:
        rows = by_key[key]
        out.append(f'const {const} = [ // {comment}')
        for r in rows:
            out.append('  ' + json.dumps(r) + ',')
        out.append('];')
        out.append('')

    out.append('/* ================= ỐM YẾU — Tàn Lửa (silhouette riêng §2.4) =================')
    out.append('   Suy ra từ grid khỏe bởi scripts/make-sick-grid.py: đầu cúi +3px, tai gập rũ,')
    out.append('   mắt nhắm hẳn (cong xuống) + lông mày chéo buồn + miệng wavy, bụng hóp,')
    out.append('   chân co ngắn, đuôi lửa TẮT → đuôi trụi rủ + vệt khói chekered rung (a/e). */')
    out.append('const TAN_LUA_SICK = [')
    for r in sick_grid:
        out.append('  ' + json.dumps(r) + ',')
    out.append('];')
    out.append('')

    out.append('/* PALS — map palette theo form (CATALOG tham chiếu qua key này) */')
    out.append('const PALS = {')
    out.append(js_palette('TAN_LUA', FIRE, 'Lửa s1'))
    out.append(js_palette('BONG_BOT', FIRE, 'Lửa s2'))
    out.append(js_palette('DIEM_LONG', FIRE, 'Lửa s3'))
    out.append(js_palette('TAN_LUA_SICK', FIRE_SICK, 'ốm: nhợt/desaturate, outline giữ đậm'))
    out.append(js_palette('GIOT', WATER, 'Thủy s1'))
    out.append(js_palette('SUOI_VOT', WATER, 'Thủy s2'))
    out.append(js_palette('TRIEU_LONG', WATER, 'Thủy s3'))
    out.append(js_palette('MAM', GRASS, 'Thảo s1'))
    out.append(js_palette('BUP_XANH', GRASS, 'Thảo s2'))
    out.append(js_palette('CO_THU_LINH', GRASS, 'Thảo s3'))
    out.append('};')

    block = '\n'.join(out) + '\n'

    html = open(HTML).read()
    start_candidates = [
        "/* ================= LINE LỬA — Charmander → Charmeleon → Charizard ================= */",
        "/* ================= PALETTE 3 HỆ (đồng bộ gen-grids-from-ref.py) =================",
    ]
    i0 = next(html.index(m) for m in start_candidates if m in html)
    end_marker = "/* §2.4 điểm 4: biến thể ốm = palette NHỢT/desaturate"
    i1 = html.index(end_marker)
    new = html[:i0] + block + '\n' + html[i1:]
    open(HTML, 'w').write(new)

    n_grids = len(re.findall(r'^const [A-Z_]+ = \[', block, re.M))
    print(f'✓ embedded {n_grids} grids + 4 palettes ({len(block)} chars) — region [{i0}:{i1}] → [{i0}:{i0+len(block)}]')

if __name__ == '__main__':
    main()
