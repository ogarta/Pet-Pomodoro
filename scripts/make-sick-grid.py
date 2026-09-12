#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""make-sick-grid.py — derive the §2.4 sick Tàn Lửa grid from the healthy ref grid.

Order of operations (face surgery happens BEFORE the head shift, in source coords):
  1. erase open eyes -> closed lid arcs curving down, sad slanted brows, wavy mouth
  2. bow the head: head block shifts DOWN +3, ear tips dropped (droopy short ears)
  3. legs: drop bottom 3 rows (bent, slumped stance)
  4. tail: bare drooping tail, flame replaced by 2px checker smoke wisp
  5. belly tucked 1px
Run: python3 scripts/make-sick-grid.py   → prints grid + writes preview PNGs
"""
import json, os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
W, H = 32, 32

def load_healthy():
    src = open(os.path.join(ROOT, 'design-demos', 'ref-grids.js')).read()
    data = json.loads(src.split('=', 1)[1].rsplit(';', 1)[0])
    g = next(x for x in data if x['key'] == 'tanLua')['grid']
    rows = [list(r.ljust(W, '.')) for r in g]
    while len(rows) < H:
        rows.append(['.'] * W)
    return rows[:H]

PAL = {
    'o': '#4A1410', 'd': '#7E1F16', 'f': '#B5382A', 'l': '#E06038', 'h': '#F5A05A',
    'c': '#F2E5C8', 'C': '#D9BE9A', 'v': '#5A1810', 'r': '#D9452B', 'y': '#F28C28',
    'Y': '#F7C948', 'W': '#FFF6D8', 'k': '#143C3C', 't': '#3FA8A0', 'w': '#FFFFFF',
    'a': '#B8AFA2', 'e': '#8E8578',
}

def render(grid, path, px=6):
    im = Image.new('RGBA', (W * px, H * px), (0, 0, 0, 0))
    for y in range(H):
        for x in range(W):
            ch = grid[y][x]
            if ch in PAL:
                c = tuple(int(PAL[ch][i:i+2], 16) for i in (1, 3, 5)) + (255,)
                for yy in range(px):
                    for xx in range(px):
                        im.putpixel((x * px + xx, y * px + yy), c)
    im.save(path)
    return im

def show(grid):
    print('    ' + ''.join(str(i // 10) for i in range(W)))
    print('    ' + ''.join(str(i % 10) for i in range(W)))
    for y, row in enumerate(grid):
        print(f'{y:2d}  ' + ''.join(row).rstrip('.'))

def main():
    src = load_healthy()
    g = [row[:] for row in src]

    # ---- 1. FACE surgery (source coords) ----
    # wipe old eyes + dark mask INSIDE the face (col 0 outline stays intact)
    for y in range(12, 18):
        for x in range(1, 14):
            if g[y][x] in 'tkwvd':
                g[y][x] = 'f'
    # BIG closed lids: downward arcs (ends up, middle sagging) — readable at a glance
    lids = {
        (14, 1): 'k', (14, 4): 'k', (15, 2): 'k', (15, 3): 'k',             # left  ⌣
        (14, 8): 'k', (14, 13): 'k', (15, 9): 'k', (15, 10): 'k',
        (15, 11): 'k', (15, 12): 'k',                                        # right ⌣⌣
    }
    # sad brows: slants above lids, inner ends lower (worried)
    brows = {
        (12, 1): 'k', (12, 2): 'k', (13, 3): 'k',     # left brow  \
        (13, 10): 'k', (12, 11): 'k', (12, 12): 'k',  # right brow /
    }
    # wavy mouth on the muzzle — erase old smile pixels first, then draw ~
    for (y, x) in [(16, 3), (16, 4), (16, 5), (17, 3), (17, 4), (17, 5), (17, 6)]:
        if g[y][x] == 'k':
            g[y][x] = 'C'
    mouth = {(16, 3): 'k', (16, 5): 'k', (16, 7): 'k', (17, 4): 'k', (17, 6): 'k'}
    for m in (lids, brows, mouth):
        for (y, x), ch in m.items():
            g[y][x] = ch

    # ---- 2. compose sick sprite on blank canvas ----
    out = [['.'] * W for _ in range(H)]
    # BODY + upper legs (rows 16..28), cols 0..18
    for y in range(16, 29):
        for x in range(0, 19):
            out[y][x] = src[y][x]
    # HEAD bowed: rows 2..17 shift +3 (ear tips rows 0..1 dropped -> droopy ears)
    for y in range(2, 18):
        for x in range(0, 19):
            ch = g[y][x]
            if ch != '.':
                out[y + 3][x] = ch

    # ---- 3. TAIL: bare drooping tail + checker smoke wisp ----
    pts = {
        (16, 17): 'f', (16, 18): 'd',
        (17, 18): 'f', (17, 19): 'd',
        (18, 19): 'f', (18, 20): 'd',
        (19, 20): 'f', (19, 21): 'd',
        (20, 21): 'f', (20, 22): 'd',
        (21, 22): 'd', (21, 23): 'o',
        # smoke wisp rising from the drooped tail tip (checker, wavering)
        (20, 24): 'a', (20, 25): 'e',
        (19, 25): 'e',
        (18, 25): 'a', (18, 26): 'e',
        (17, 26): 'e',
        (16, 26): 'a', (16, 27): 'e',
        (15, 27): 'e',
        (14, 27): 'a', (14, 28): 'e',
        (13, 28): 'e',
        (12, 28): 'a', (12, 29): 'e',
    }
    for (y, x), ch in pts.items():
        out[y][x] = ch

    # ---- 4. belly tuck: pull lower-left outline in 1px ----
    for y in range(21, 26):
        if out[y][1] != '.':
            out[y][1] = '.'

    # ---- 5. bottom-align content (healthy feet reach the last row) ----
    rows = [''.join(r) for r in out]
    last = max(i for i, r in enumerate(rows) if r.strip('.'))
    shift = (H - 1) - last
    if shift:
        out = [['.'] * W for _ in range(shift)] + [r[:] for r in out[:H - shift]]

    show(out)

    out_dir = os.path.join(ROOT, 'design-demos', 'grids-from-ref')
    render(out, os.path.join(out_dir, 'tanLua-sick-preview.png'))
    pair = Image.new('RGBA', (W * 6 * 2 + 24, H * 6), (245, 233, 216, 255))
    pair.paste(render(src, os.path.join(out_dir, '_tmp_healthy.png')), (0, 0))
    pair.paste(render(out, os.path.join(out_dir, '_tmp_sick.png')), (W * 6 + 24, 0))
    pair.save(os.path.join(out_dir, 'tanLua-sick-vs-healthy.png'))

    js = ['  "' + ''.join(r).rstrip('.') + '",' for r in out]
    print('\nJS GRID:\n[\n' + '\n'.join(js) + '\n]')

    # JSON dump for embed-ref-grids.py
    with open(os.path.join(out_dir, 'tanLua-sick.json'), 'w') as f:
        json.dump({'grid': [''.join(r).rstrip('.') for r in out]}, f, indent=1)

if __name__ == '__main__':
    main()
