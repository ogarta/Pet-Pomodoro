#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gen-sprites.py — biên dịch định nghĩa sprite (segment-based) → design-demos/sprite-data.js
Row = chuỗi đầy đủ HOẶC list segment (col_start, text). Script tự:
  - compose row đủ width, báo lỗi overlap/tràn
  - kiểm tra ký tự có trong palette
  - kiểm tra grid vuông (size × size)
Output: design-demos/sprite-data.js (globalThis.SPRITE_DATA)
"""
import json, sys

W = {}  # name -> def

def S(name, line, stage, size, palette, rows):
    W[name] = dict(line=line, stage=stage, size=size, palette=palette, rows=rows)

def compose(size, row):
    """row: str hoặc list[(col, text)] → chuỗi dài `size`"""
    if isinstance(row, str):
        assert len(row) == size, f"row chuỗi sai độ dài {len(row)} != {size}: {row}"
        return row
    buf = ["."] * size
    for col, text in row:
        assert 0 <= col < size, f"col {col} ngoài range"
        for i, ch in enumerate(text):
            p = col + i
            assert p < size, f"segment '{text}' @{col} tràn phải ({p} >= {size})"
            assert buf[p] == ".", f"overlap @{p}: '{buf[p]}' vs '{ch}' (row segs {row})"
            buf[p] = ch
    return "".join(buf)

# ============================================================
# PALETTE (copy sang JS nguyên trạng)
# ============================================================
PALETTES = {
  "fire": {
    "o": ("#4A1410", "viền nâu đô"), "d": ("#7E1F16", "đỏ sâu"),
    "f": ("#B5382A", "đỏ thân"), "l": ("#E06038", "đỏ cam sáng"),
    "h": ("#F5A05A", "highlight cam"), "c": ("#F2E5C8", "kem mõm ngực"),
    "C": ("#D9BE9A", "kem bóng"), "v": ("#5A1810", "viền lửa"),
    "r": ("#D9452B", "lửa cạnh"), "y": ("#F28C28", "lửa giữa"),
    "Y": ("#F7C948", "lửa sáng"), "W": ("#FFF6D8", "lõi lửa"),
    "k": ("#143C3C", "mắt tối"), "t": ("#3FA8A0", "mống teal"),
    "w": ("#FFFFFF", "glint"),
  },
  "fireShiny": {
    "o": ("#232B36", "viền xám đậm"), "d": ("#46566E", "thép sâu"),
    "f": ("#8C9BB0", "thép thân"), "l": ("#B8C4D4", "thép sáng"),
    "h": ("#DDE6EE", "highlight bạc"), "c": ("#E8E9F2", "kem lạnh"),
    "C": ("#BFC2D4", "kem bóng lạnh"), "v": ("#1E3B3C", "viền lửa teal"),
    "r": ("#1F8F84", "teal cạnh"), "y": ("#2EC4B6", "teal giữa"),
    "Y": ("#7FE8DE", "teal sáng"), "W": ("#E8FFF9", "lõi teal"),
    "k": ("#143C3C", "mắt tối"), "t": ("#E8B84A", "mống vàng"),
    "w": ("#FFFFFF", "glint"),
  },
  "fireSick": {
    "o": ("#5A4438", "viền nhợt"), "d": ("#8A6A5C", "đỏ nhạt sâu"),
    "f": ("#B08A76", "đỏ nhạt thân"), "l": ("#C9A88E", "sáng nhợt"),
    "h": ("#E0C8AC", "highlight nhợt"), "c": ("#E5DCC8", "kem nhợt"),
    "C": ("#C2B49A", "kem bóng nhợt"), "g": ("#8C8C8C", "khói"),
    "G": ("#6E6E6E", "khói đậm"), "k": ("#4A4038", "mắt nhắm"),
    "t": ("#8CA8A0", "mống nhợt"), "w": ("#FFFFFF", "glint"),
  },
  "water": {
    "o": ("#16283E", "viền nave"), "d": ("#24365C", "navy sâu"),
    "f": ("#35507E", "navy thân"), "l": ("#5B7CB0", "navy sáng"),
    "h": ("#8FA8CE", "highlight"), "c": ("#C7D8EE", "bụng sáng"),
    "C": ("#9FB4D0", "bụng bóng"), "a": ("#E8C860", "vây vàng"),
    "A": ("#B89030", "vây vàng đậm"), "b": ("#A8D8F0", "bọt sáng"),
    "B": ("#5FA8CC", "bọt đậm"), "k": ("#101820", "mắt tối"),
    "t": ("#F0C040", "mống vàng"), "w": ("#FFFFFF", "glint"),
  },
  "grass": {
    "o": ("#1E3A1C", "viền rêu đậm"), "d": ("#3E7A34", "xanh sâu"),
    "f": ("#5FA84A", "xanh thân"), "l": ("#93D06E", "xanh sáng"),
    "h": ("#B8E88A", "lá non"), "c": ("#D8EFC0", "sáng nhạt"),
    "C": ("#A8C888", "bóng nhạt"), "u": ("#4E8A5E", "nụ/hạt"),
    "U": ("#37634A", "nụ đậm"), "p": ("#F0A0B8", "cánh hoa"),
    "P": ("#D06890", "cánh đậm"), "m": ("#8A6242", "thân cây"),
    "M": ("#63452E", "thân cây đậm"), "k": ("#142014", "mắt tối"),
    "w": ("#FFFFFF", "glint"),
    "R": ("#D93A2A", "mắt đỏ"),
    "e": ("#FF8A4A", "than hồng"),
  },
  "egg": {
    "o": ("#4A3C30", "viền nâu"), "c": ("#F7E7CE", "vỏ kem"),
    "C": ("#DEC29B", "vỏ bóng"), "e": ("#FFF8EA", "vỏ sáng"),
    "y": ("#F5B840", "vân vàng"), "Y": ("#FFDD7C", "vân sáng"),
    "W": ("#FFF7DB", "tâm vân"),
  },
}

# ============================================================
# SPRITES
# ============================================================

# ---------- 🥚 TRỨNG ----------
S("trungEgg", "chung", 0, 32, "egg", [
"................................",
"................................",
"................................",
"................................",
"................................",
"................................",
"................................",
"................................",
"...............oo...............",
".............ooccoo.............",
"............oeeeecco............",
"...........oeeecccccCo..........",
"..........oeeccccccccCo.........",
".........oecccyycccccCo.........",
".........oeccyYyyccccCo.........",
".........oeccyYWWyccCCo.........",
".........oeccyYYyccccCo.........",
".........oecccyycccccco.........",
".........oeccccccccccCo.........",
".........oeccccccccccCCo........",
"..........occcccccccCCo.........",
"..........occccccccCCCo.........",
"...........occcccCCCCo..........",
"............occcccCCo...........",
".............ocCCCo.............",
"..............oooo..............",
"................................",
"................................",
"................................",
"................................",
"................................",
"................................",
])

# ---------- 🔥 TÀN LỬA (stage 1, 32×32) ----------
# Hồ ly con 3/4 nhìn trái: đầu to 16px, tai 2 chuôi, mắt teal 4×4,
# mõm kem, ngực kem, đuôi lửa 4 lớp cao bằng đầu (cột 17-30).
S("tanLua", "lua", 1, 32, "fire", [
  [],                                       # 0
  [],                                       # 1
  [(6,"oo"),(16,"oo")],                     # 2  tai
  [(5,"oddo"),(15,"oddo")],                 # 3
  [(4,"oddo"),(16,"oddo")],                 # 4
  [(4,"oddo"),(8,"ooooooo"),(15,"oddo")],    # 5  gốc tai + đỉnh đầu
  [(4,"o"),(5,"ll"),(7,"fff"),(10,"ddd"),(13,"fffff"),(18,"dd"),(20,"o"),(26,"vrv")],   # 6 vết trán
  [(4,"o"),(5,"l"),(6,"ff"),(8,"f"),(9,"ddddd"),(14,"ffff"),(18,"d"),(19,"o"),(24,"vryYyrv")],# 7 vết trán
  [(4,"o"),(5,"dddd"),(9,"fff"),(12,"dddd"),(16,"fff"),(19,"o"),(23,"vryYYyrv")], # 8 mắt vành mềm
  [(4,"o"),(5,"wkkk"),(9,"fff"),(12,"wkkk"),(16,"fff"),(19,"o"),(22,"vryYWYyrv")],# 9 glint
  [(4,"o"),(5,"kttk"),(9,"fff"),(12,"kttk"),(16,"fff"),(19,"o"),(21,"vryYWWYyrv")],#10 iris
  [(4,"o"),(5,"kttk"),(9,"fff"),(12,"kttk"),(16,"fff"),(19,"o"),(21,"vryYWWYyrv")],#11
  [(4,"o"),(5,"kk"),(7,"ccc"),(10,"ff"),(12,"fffffff"),(19,"o"),(21,"vryYWWYyrv")],  # 12 mũi to + mõm
  [(4,"o"),(5,"ccoc"),(9,"fff"),(12,"fffffff"),(19,"o"),(21,"vryYWYyrv")],   # 13 miệng
  [(5,"o"),(6,"cfcfc"),(11,"ffffff"),(17,"d"),(18,"o"),(20,"vryYWYrv")],         # 14 má 3 răng cưa
  [(6,"o"),(7,"ffcccffffd"),(17,"o"),(19,"vryYWYrv")],                       # 15
  [(7,"o"),(8,"ccccfffd"),(16,"o"),(17,"ryYWYyrv")],                         # 16 ngực
  [(8,"o"),(9,"ccccfd"),(15,"o"),(16,"ryYWYyrv")],                           # 17
  [(8,"o"),(9,"ccccfd"),(15,"o"),(16,"ryYWYyrv")],                           # 18
  [(7,"o"),(8,"cccffffdd"),(17,"o"),(18,"yYWYyrv")],                         # 19 hông
  [(7,"o"),(8,"cccffffdd"),(17,"o"),(18,"yYWYyrv")],                         # 20
  [(8,"o"),(9,"cccfffd"),(16,"o"),(17,"ryYYrv")],                            # 21
  [(8,"o"),(9,"fffffdd"),(16,"o"),(17,"vrrv")],                              # 22 đáy lửa
  [(9,"o"),(10,"ffffdd"),(16,"o")],                                          # 23
  [(9,"offo"),(14,"offo")],                                                  # 24 chân
  [(9,"oddo"),(14,"oddo")],                                                  # 25
  [(9,"oooo"),(14,"oooo")],                                                  # 26
])

# ---------- 🔥 TÀN LỬA back view (32×32) ----------
# Từ phía sau: không mặt, bờm gáy, đuôi lửa to giữa phải, lưng răng cưa.
S("tanLuaBack", "lua", 1, 32, "fire", [
  [],
  [],
  [(6,"oo"),(16,"oo")],
  [(5,"oddo"),(15,"oddo")],
  [(4,"oddo"),(16,"oddo")],
  [(4,"oddo"),(8,"oooooooooooo")],
  [(4,"o"),(5,"dd"),(7,"f"*10),(17,"dd"),(19,"o"),(24,"vryYyrv")],
  [(4,"o"),(5,"d"),(6,"f"*11),(17,"dd"),(19,"o"),(23,"vryYWYrv")],
  [(4,"o"),(5,"f"*12),(17,"dd"),(19,"o"),(22,"vryYWYyrv")],
  [(4,"o"),(5,"f"*6),(11,"dd"),(13,"f"*4),(17,"dd"),(19,"o"),(22,"vryYWYyrv")],
  [(4,"o"),(5,"f"*12),(17,"dd"),(19,"o"),(21,"vryYWWYyrv")],
  [(4,"o"),(5,"f"*12),(17,"dd"),(19,"o"),(21,"vryYWWYyrv")],
  [(4,"o"),(5,"f"*5),(10,"dd"),(12,"f"*5),(17,"dd"),(19,"o"),(21,"vryYWYyrv")],
  [(4,"o"),(5,"f"*12),(17,"dd"),(19,"o"),(22,"vryYWYrv")],
  [(5,"o"),(6,"fdffdfdff"),(15,"d"),(16,"o"),(18,"f"),(19,"o"),(23,"vryYrv")],
  [(6,"o"),(7,"fdfdffdf"),(15,"d"),(16,"o"),(18,"yYWYrv")],
  [(7,"o"),(8,"fdffdfd"),(15,"o"),(16,"ryYWYrv")],
  [(8,"o"),(9,"fdfdfd"),(15,"o"),(16,"ryYWYrv")],
  [(8,"o"),(9,"ffffdd"),(15,"o"),(16,"ryYWYyrv")],
  [(7,"o"),(8,"fffddfdd"),(16,"o"),(17,"ryYWYyrv")],
  [(7,"o"),(8,"fffddfdd"),(16,"o"),(17,"ryYWYrv")],
  [(8,"o"),(9,"fffdd"),(14,"o"),(15,"yYWYrv")],
  [(8,"o"),(9,"ffffdd"),(15,"o"),(16,"yYWrv")],
  [(9,"o"),(10,"fffdd"),(15,"o")],
  [(9,"offo"),(14,"offo")],
  [(9,"oddo"),(14,"oddo")],
  [(9,"oooo"),(14,"oooo")],
])

# ---------- 🔥 TÀN LỬA ỐM (32×32) — silhouette vẽ lại §2.4 ----------
# Tai cụp, mắt nhắm, khói thay lửa, thân xìu thấp hơn, miệng wavy.
S("tanLuaOm", "lua", 1, 32, "fireSick", [
  [],
  [],
  [],
  [(7,"oo"),(15,"oo")],
  [(6,"oddo"),(14,"oddo")],
  [(5,"oddo"),(9,"oooooooo")],
  [(5,"o"),(6,"ll"),(8,"f"*8),(16,"dd"),(18,"o"),(24,"gGg")],
  [(5,"o"),(6,"l"),(7,"f"*9),(16,"dd"),(18,"o"),(23,"gGgGg")],
  [(5,"o"),(6,"kk"),(9,"fff"),(12,"kk"),(15,"fff"),(18,"o"),(22,"gGgGg")],
  [(5,"o"),(6,"kk"),(9,"fff"),(12,"kk"),(15,"fff"),(18,"o"),(23,"gGg")],
  [(5,"o"),(6,"cccc"),(10,"fff"),(13,"ffffff"),(19,"o")],
  [(5,"o"),(6,"coco"),(10,"fff"),(13,"ffffff"),(19,"o")],
  [(6,"o"),(7,"cffcffd"),(14,"d"),(15,"o")],
  [(7,"o"),(8,"fcccfffd"),(16,"o")],
  [(8,"o"),(9,"cccffd"),(15,"o")],
  [(8,"o"),(9,"cccffd"),(15,"o")],
  [(7,"o"),(8,"cccffdd"),(15,"o")],
  [(7,"o"),(8,"cccffdd"),(15,"o")],
  [(8,"o"),(9,"ccffd"),(14,"o")],
  [(8,"o"),(9,"ffffdd"),(15,"o")],
  [(9,"o"),(10,"fffdd"),(15,"o")],
  [(9,"offo"),(13,"offo")],
  [(9,"oddo"),(13,"oddo")],
  [(9,"oooo"),(13,"oooo")],
])


# ---------- 🔥 BỒNG BỘT (stage 2, 40×40) ----------
# Cao lớn hơn, đứng thẳng ngực ưỡn, bờm lửa sau gáy, mắt dữ có channếp mí,
# đuôi lửa to dài; vẫn mõm kem + ngực kem răng cưa.
S("bongBot", "lua", 2, 40, "fire", [
  [],
  [],
  [(11,"oo"),(22,"oo")],
  [(10,"oddo"),(21,"oddo")],
  [(9,"oddo"),(20,"oddo")],
  [(8,"oddo"),(19,"oddo")],
  [(8,"oddo"),(12,"o"*10),(22,"oddo")],
  [(7,"o"),(8,"l"),(9,"ff"),(11,"ddddd"),(16,"f"*10),(26,"o"),(31,"vrv")],
  [(7,"o"),(8,"l"),(9,"f"*15),(24,"dd"),(26,"o"),(29,"vryYyrv")],
  [(6,"o"),(7,"l"),(8,"f"*17),(25,"dd"),(27,"o"),(28,"vryYYyrv")],
  [(6,"o"),(7,"ddddd"),(12,"fff"),(15,"ddddd"),(20,"f"*7),(27,"o"),(28,"vryYWYrv")],
  [(6,"o"),(7,"wdddd"),(12,"fff"),(15,"wdddd"),(20,"f"*7),(27,"o"),(28,"vryYWYrv")],
  [(6,"o"),(7,"ktttk"),(12,"fff"),(15,"ktttk"),(20,"f"*7),(27,"o"),(28,"vryYWYrv")],
  [(6,"o"),(7,"ktttk"),(12,"fff"),(15,"ktttk"),(20,"f"*7),(27,"o"),(28,"vryYWYrv")],
  [(6,"o"),(7,"kkkkk"),(12,"fff"),(15,"kkkkk"),(20,"f"*7),(27,"o"),(28,"vryYWYrv")],
  [(6,"o"),(7,"kk"),(9,"ccccc"),(14,"f"*13),(27,"o"),(28,"vryYWYrv")],
  [(6,"o"),(7,"cccoc"),(12,"f"*14),(26,"o"),(28,"vryYWYyrv")],
  [(7,"o"),(8,"cfcfc"),(13,"f"*8),(21,"d"*6),(27,"o"),(28,"vryYWYyrv")],
  [(8,"o"),(9,"cccc"),(13,"f"*9),(22,"dddd"),(26,"o"),(28,"vryYWYyrv")],
  [(8,"o"),(9,"cccccc"),(15,"f"*7),(22,"ddddd"),(27,"o"),(28,"ryYWYyrv")],
  [(7,"o"),(8,"ccccc"),(13,"f"*8),(21,"d"*6),(27,"o"),(28,"ryYWYrv")],
  [(7,"o"),(8,"ccccc"),(13,"f"*8),(21,"d"*6),(27,"o"),(28,"ryYWYrv")],
  [(7,"o"),(8,"cccccc"),(14,"f"*7),(21,"d"*6),(27,"o"),(28,"ryYWYrv")],
  [(7,"o"),(8,"cccccc"),(14,"f"*7),(21,"d"*6),(27,"o"),(28,"ryYyrv")],
  [(7,"o"),(8,"cccccc"),(14,"f"*7),(21,"d"*6),(27,"o"),(28,"ryYyrv")],
  [(7,"o"),(8,"ccccff"),(14,"fffddd"),(21,"d"*6),(27,"o"),(28,"ryYrv")],
  [(7,"o"),(8,"ccccff"),(14,"fffddd"),(21,"d"*6),(27,"o"),(28,"ryrv")],
  [(8,"o"),(9,"cccff"),(14,"fffdd"),(19,"d"*7),(26,"o")],
  [(8,"o"),(9,"cccff"),(14,"ffddd"),(19,"d"*7),(26,"o")],
  [(9,"o"),(10,"ccffd"),(15,"ffdd"),(19,"d"*6),(25,"o")],
  [(10,"offo"),(20,"offo")],
  [(10,"oddo"),(20,"oddo")],
  [(10,"oooo"),(20,"oooo")],
])

# ---------- 🔥 DIỄM LONG (stage 3, 48×48) ----------
# Fox tứ chi uy nghi nhìn trái: bờm lửa streaming sau gáy, thân dài,
# 4 chân, ngực kem, đuôi lửa khổng lồ 2 nhánh, mắt vàng sắc.
S("diemLong", "lua", 3, 48, "fire", [
  [],
  [],[],[],[],
  [(14,"oo"),(24,"oo")],
  [(13,"oddo"),(23,"oddo")],
  [(12,"oddo"),(22,"oddo")],
  [(12,"oddo"),(21,"ooooooo"),(28,"o")],
  [(11,"o"),(12,"ll"),(14,"fff"),(17,"ddddd"),(22,"f"*6),(28,"dd"),(30,"o"),(36,"vrv")],
  [(11,"o"),(12,"l"),(13,"f"*16),(29,"dd"),(31,"o"),(35,"vryYyrv")],
  [(10,"o"),(11,"l"),(12,"f"*18),(30,"dd"),(32,"o"),(34,"vryYYyrv")],
  [(10,"o"),(11,"dddddd"),(17,"fff"),(20,"dddddd"),(26,"dddddd"),(32,"o"),(33,"vryYWYrv")],
  [(10,"o"),(11,"wddddd"),(17,"fff"),(20,"wddddd"),(26,"dddddd"),(32,"o"),(33,"vryYWYrv")],
  [(10,"o"),(11,"ktttkk"),(17,"fff"),(20,"ktttkk"),(26,"dddddd"),(32,"o"),(33,"vryYWYrv")],
  [(10,"o"),(11,"ktttkk"),(17,"fff"),(20,"ktttkk"),(26,"dddddd"),(32,"o"),(33,"vryYWYrv")],
  [(10,"o"),(11,"kkkkkk"),(17,"fff"),(20,"kkkkkk"),(26,"dddddd"),(32,"o"),(33,"vryYWYrv")],
  [(10,"o"),(11,"kk"),(13,"ccc"),(16,"ccccc"),(21,"f"*9),(30,"dd"),(32,"o"),(33,"vryYWYrv")],
  [(10,"o"),(11,"ccccc"),(16,"coc"),(19,"f"*11),(30,"dd"),(32,"o"),(33,"vryYWYyrv")],
  [(11,"o"),(12,"cfcfc"),(17,"f"*13),(30,"dd"),(32,"o"),(33,"vryYWYyrv")],
  [(12,"o"),(13,"cccc"),(17,"f"*12),(29,"ddd"),(32,"o"),(33,"vryYWYyrv")],
  [(13,"o"),(14,"cccccc"),(20,"f"*8),(28,"dddd"),(32,"o"),(33,"ryYWYyrv")],
  [(13,"o"),(14,"cccccc"),(20,"f"*8),(28,"dddd"),(32,"o"),(33,"ryYWYyrv")],
  [(12,"o"),(13,"ccccc"),(18,"f"*10),(28,"ddddd"),(33,"o"),(34,"ryYWYrv")],
  [(11,"o"),(12,"cccc"),(16,"f"*12),(28,"dddddd"),(34,"o"),(35,"ryYWYrv")],
  [(10,"o"),(11,"cccc"),(15,"f"*13),(28,"ddddddd"),(35,"o"),(36,"ryYWYrv")],
  [(10,"o"),(11,"cccc"),(15,"f"*13),(28,"ddddddd"),(35,"o"),(36,"ryYyrv")],
  [(10,"o"),(11,"ccccc"),(16,"f"*12),(28,"ddddddd"),(35,"o"),(36,"ryYyrv")],
  [(10,"o"),(11,"cccfff"),(17,"f"*11),(28,"ddddddd"),(35,"o"),(36,"ryYrv")],
  [(10,"o"),(11,"ccffff"),(17,"ffddd"),(24,"ddddddd"),(31,"dd"),(34,"o"),(36,"ryrv")],
  [(10,"o"),(11,"ccfffd"),(17,"ffdddd"),(25,"dddddd"),(31,"dd"),(33,"o"),(34,"yYrv")],
  [(11,"o"),(12,"ccffdd"),(18,"ffddd"),(24,"dddddd"),(30,"dd"),(32,"o"),(33,"yrv")],
  [(12,"o"),(13,"cffdd"),(18,"ffddd"),(24,"dddd"),(28,"dd"),(30,"o")],
  [(12,"o"),(13,"fffdd"),(18,"ffddd"),(24,"dddd"),(28,"dd"),(30,"o")],
  [(13,"o"),(14,"fffddd"),(21,"dddddd"),(27,"dd"),(29,"o")],
  [(15,"o"),(20,"o"),(26,"o")],
  [(14,"offo"),(20,"offo"),(26,"offfo")],
  [(14,"oddo"),(20,"oddo"),(26,"oddo")],
  [(14,"oooo"),(20,"oooo"),(26,"oooo")],
])

# ---------- 💧 GIỌT (stage 1, 32×32) ----------
# Cá con tròn mập nhìn trái: giọt nước trên đầu, vây ngực vàng, mắt to,
# đuôi sóng nhỏ, bụng sáng.
S("giot", "thuy", 1, 32, "water", [
  [],
  [],
  [],
  [(14,"b")],
  [(13,"bbb")],
  [(12,"bbBbb")],
  [(12,"bbBbb")],
  [(11,"obBBBbo")],
  [(10,"oBBBBBBo")],
  [(6,"oooooooooooooo")],
  [(5,"o"),(6,"d"*13),(19,"o")],
  [(4,"o"),(5,"d"*15),(20,"o")],
  [(4,"o"),(5,"d"),(6,"kkkk"),(10,"ffff"),(14,"d"*7),(21,"o")],
  [(4,"o"),(5,"d"),(6,"wkkk"),(10,"ffff"),(14,"d"*7),(21,"o")],
  [(4,"o"),(5,"d"),(6,"kttk"),(10,"ffff"),(14,"d"*7),(21,"o"),(22,"a")],
  [(4,"o"),(5,"d"),(6,"kkkk"),(10,"ffff"),(14,"d"*7),(21,"o"),(22,"aa")],
  [(4,"o"),(5,"cco"),(8,"cccc"),(12,"d"*9),(21,"o"),(22,"aAa")],
  [(4,"o"),(5,"ccccc"),(10,"aA"),(12,"cc"),(14,"d"*7),(21,"o"),(22,"aAAa")],
  [(3,"o"),(4,"c"*16),(20,"o"),(22,"aAAa")],
  [(3,"o"),(4,"c"*16),(20,"o"),(22,"aAAa")],
  [(3,"o"),(4,"c"*15),(19,"d"),(20,"o"),(21,"aAa")],
  [(4,"o"),(5,"c"*14),(19,"o"),(20,"aa")],
  [(4,"o"),(5,"c"*14),(19,"o"),(20,"a")],
  [(5,"o"),(6,"c"*12),(18,"o")],
  [(6,"o"),(7,"c"*10),(17,"o")],
  [(8,"ooooooooo")],
])

# ---------- 💧 SUỐI VỢT (stage 2, 40×40) ----------
# Cá ng.ReadOnly: thân thoi dài, vây lưng cao vàng, mắt dữ, miệng wavy,
# vây ngực, đuôi chéo; bọt nước bay sau đuôi.
S("suoiVot", "thuy", 2, 40, "water", [
  [],
  [],
  [(18,"a")],
  [(17,"aa"),(34,"bb")],
  [(17,"aAa"),(35,"b")],
  [(16,"aAAa"),(33,"bB")],
  [(16,"aAAa"),(34,"bB")],
  [(15,"aaAAa"),(35,"b")],
  [(15,"aaAAa"),(33,"bBb")],
  [(14,"aaaAAaa")],
  [(13,"aaaaAAaaaa")],
  [(12,"aaaaaAAaaaaa")],
  [(14,"o"*11)],
  [(10,"o"),(11,"d"*16),(27,"o")],
  [(7,"o"),(8,"d"*21),(29,"o"),(30,"a")],
  [(5,"o"),(6,"d"*24),(30,"o"),(31,"aa")],
  [(4,"o"),(5,"d"*7),(12,"kkkkk"),(17,"d"*13),(30,"o"),(31,"aAa")],
  [(3,"o"),(4,"d"*8),(12,"wkkkk"),(17,"d"*14),(31,"o"),(32,"aAAa")],
  [(3,"o"),(4,"d"*8),(12,"kttkk"),(17,"d"*14),(31,"o"),(32,"aAAa")],
  [(2,"o"),(3,"d"*9),(12,"kkkkk"),(17,"d"*14),(31,"o"),(32,"aAAa")],
  [(2,"o"),(3,"oco"),(6,"d"*25),(31,"o"),(32,"aaAa")],
  [(2,"o"),(3,"cccc"),(7,"ddd"),(10,"f"*21),(31,"o"),(32,"aAAa")],
  [(2,"o"),(3,"ccccc"),(8,"aA"),(10,"c"*3),(13,"f"*18),(31,"o"),(32,"aaAa")],
  [(3,"o"),(4,"cccccc"),(10,"f"*21),(31,"o"),(32,"aaAa")],
  [(3,"o"),(4,"cccccc"),(10,"Bfff"),(14,"Bff"),(17,"f"*14),(31,"o"),(32,"aAa")],
  [(3,"o"),(4,"ccccc"),(9,"ffffBf"),(15,"f"*16),(31,"o"),(32,"aa")],
  [(4,"o"),(5,"cccc"),(9,"f"*22),(31,"o"),(33,"a")],
  [(5,"o"),(6,"ccc"),(9,"f"*22),(31,"o")],
  [(6,"o"),(7,"ccc"),(10,"f"*20),(30,"o")],
  [(8,"o"),(9,"cc"),(11,"f"*18),(29,"o")],
  [(10,"o"),(11,"cc"),(13,"f"*15),(28,"o")],
  [(13,"o"),(14,"f"*12),(26,"o")],
  [(16,"oooooooo")],
])# ---------- 💧 TRIỀU LONG (stage 3, 48×48) ----------
# Quái vật triều: hàm rộng răng trắng, vây lưngkhổng lồ, mắt nhỏ dữ,
# thân khổng lồ có hoa văn sóng, vây đuôi to, xoáy nước quanh thân.
S("trieuLong", "thuy", 3, 48, "water", [
  [],
  [],[],
  [(20,"a"),(43,"bb")],
  [(19,"aa"),(42,"bBb")],
  [(19,"aAa"),(44,"b")],
  [(18,"aAAa"),(41,"bBb"),(45,"b")],
  [(18,"aAAa"),(40,"bB"),(46,"b")],
  [(17,"aaAAaa"),(43,"BbB")],
  [(16,"aaaAAaaa")],
  [(15,"aaaaAAaaaa"),(41,"bBb")],
  [(14,"aaaaaAAaaaaa")],
  [(13,"aaaaaaAAaaaaaa")],
  [(12,"aaaaaaaAAaaaaaaa")],
  [(4,"oooooooooooooooooooooooooooooooooooo")],
  [(3,"o"),(4,"d"*9),(13,"ddddd"),(18,"d"*22),(40,"o"),(41,"B"),(44,"bB")],
  [(3,"o"),(4,"d"*9),(13,"kkkkk"),(18,"d"*5),(23,"ffffffffffffffff"),(40,"o"),(43,"bBb")],
  [(3,"o"),(4,"d"*9),(13,"wkkkk"),(18,"d"*5),(23,"f"*17),(40,"o"),(42,"bb"),(46,"b")],
  [(3,"o"),(4,"d"*9),(13,"kttkk"),(18,"d"*5),(23,"f"*17),(40,"o"),(41,"b")],
  [(3,"o"),(4,"d"*9),(13,"kkkkk"),(18,"d"*5),(23,"f"*17),(40,"o")],
  [(3,"o"),(4,"d"*7),(11,"ww"),(13,"d"*5),(18,"ww"),(20,"d"*3),(23,"f"*17),(40,"o")],
  [(3,"o"),(4,"d"*7),(11,"cccc"),(15,"coccc"),(20,"d"*3),(23,"f"*17),(40,"o")],
  [(3,"o"),(4,"cccccccc"),(12,"cccc"),(16,"c"*7),(23,"f"*17),(40,"o")],
  [(3,"o"),(4,"c"*9),(13,"aA"),(15,"cc"),(17,"f"*8),(25,"BffB"),(29,"f"*11),(40,"o")],
  [(2,"o"),(4,"c"*9),(13,"aAAa"),(17,"cc"),(19,"f"*6),(25,"BfB"),(28,"f"*12),(40,"o")],
  [(2,"o"),(4,"c"*9),(13,"aAAa"),(17,"c"*2),(19,"f"*21),(40,"o")],
  [(2,"o"),(4,"c"*10),(14,"f"*26),(40,"o")],
  [(2,"o"),(4,"c"*10),(14,"f"*8),(22,"BfffBff"),(30,"f"*10),(40,"o")],
  [(2,"o"),(4,"c"*10),(14,"f"*8),(22,"ffffff"),(28,"f"*12),(40,"o")],
  [(2,"o"),(4,"c"*9),(13,"f"*27),(40,"o")],
  [(3,"o"),(4,"c"*9),(13,"f"*27),(40,"o")],
  [(3,"o"),(4,"c"*8),(12,"f"*28),(40,"o")],
  [(4,"o"),(5,"c"*8),(13,"f"*27),(40,"o")],
  [(5,"o"),(6,"c"*8),(14,"f"*26),(40,"o")],
  [(6,"o"),(7,"c"*8),(15,"f"*25),(40,"o")],
  [(7,"o"),(8,"c"*8),(16,"f"*24),(40,"o")],
  [(8,"o"),(9,"c"*7),(17,"f"*23),(40,"o")],
  [(10,"o"),(11,"c"*7),(18,"f"*22),(40,"o")],
  [(12,"o"),(13,"c"*6),(20,"f"*20),(40,"o")],
  [(14,"o"),(15,"cc"),(18,"f"*8),(27,"dddddddd"),(35,"f"*5),(40,"o")],
  [(16,"o"),(17,"c"),(19,"f"*6),(26,"dddddd"),(32,"f"*3),(36,"o")],
  [(18,"oooooooooooooooo")],
])

# ---------- 🌿 MẦM (stage 1, 32×32) ----------
# Hạt mầm: thân hạt tròn, cặp lá to đối xứng trên đầu, mắt to tròn,
# blush, 2 chân củ nhỏ — đúng chất Sproutling.
S("mam", "thao", 1, 32, "grass", [
  [],
  [],
  [(12,"oo"),(18,"oo")],
  [(11,"ohho"),(17,"ohho")],
  [(10,"ohhho"),(17,"ohhho")],
  [(10,"ohhho"),(17,"ohhho")],
  [(9,"ohhhho"),(17,"ohhhho")],
  [(9,"ohhho"),(18,"ohhho")],
  [(9,"ohho"),(15,"oo"),(19,"ohho")],
  [(10,"oho"),(15,"oo"),(19,"oho")],
  [(11,"oooooooooo")],
  [(10,"o"),(11,"d"),(12,"u"*8),(20,"U"),(21,"o")],
  [(9,"o"),(10,"dd"),(12,"u"*7),(19,"UU"),(21,"o")],
  [(8,"o"),(9,"dd"),(11,"u"*9),(20,"UU"),(22,"o")],
  [(8,"o"),(9,"d"),(10,"u"*10),(20,"UU"),(22,"o")],
  [(8,"o"),(9,"d"),(10,"u"),(11,"kkk"),(14,"uuu"),(17,"kkk"),(20,"UU"),(22,"o")],
  [(8,"o"),(9,"d"),(10,"u"),(11,"wkk"),(14,"uuu"),(17,"wkk"),(20,"UU"),(22,"o")],
  [(8,"o"),(9,"d"),(10,"u"),(11,"kkk"),(14,"uuu"),(17,"kkk"),(20,"UU"),(22,"o")],
  [(8,"o"),(9,"d"),(10,"u"),(11,"kkk"),(14,"uuu"),(17,"kkk"),(20,"UU"),(22,"o")],
  [(8,"o"),(9,"d"),(10,"pp"),(12,"uuu"),(15,"oo"),(17,"uu"),(19,"pp"),(21,"U"),(22,"o")],
  [(8,"o"),(9,"dd"),(11,"u"*9),(20,"UU"),(22,"o")],
  [(8,"o"),(9,"dd"),(11,"u"*9),(20,"UU"),(22,"o")],
  [(8,"o"),(9,"dd"),(11,"u"*9),(20,"UU"),(22,"o")],
  [(8,"o"),(9,"d"),(10,"u"*10),(20,"UU"),(22,"o")],
  [(9,"o"),(10,"d"),(11,"u"*8),(19,"UU"),(21,"o")],
  [(10,"o"),(11,"d"),(12,"u"*7),(19,"U"),(20,"o")],
  [(12,"oooooooo")],
  [(11,"offo"),(18,"offo")],
  [(11,"oddo"),(18,"oddo")],
  [(11,"oooo"),(18,"oooo")],
])

# ---------- 🌿 BÚP XANH (stage 2, 40×40) ----------
# Khủng long mầm 4 chân: đầu to há miệng hiền, nụ hoa hồng trên lưng,
# đuôi lá, bụng sáng — đúng chất Florasaur.
S("bupXanh", "thao", 2, 40, "grass", [
  [],
  [],
  [],
  [(26,"pp"),(29,"pp")],
  [(25,"pPPp"),(29,"p")],
  [(24,"pPPPPp"),(24+6,"o")],
  [(24,"pPPPPPPp")],
  [(23,"pPPpPPPPp"),(33,"o")],
  [(23,"pPpUUUUUpp"),(33,"o")],
  [(23,"oUUUUUUUUUo"),(34,"o")],
  [(22,"oUuUUUUUUUUo"),(34,"o")],
  [(22,"oUUuUUUUUUUo"),(34,"o")],
  [(22,"oUUUUuUUUUUo"),(34,"o")],
  [(22,"oUUUUUUuUUUo"),(35,"o")],
  [(5,"oooooooooooooo"),(22,"oUUUUUUUUUUo"),(35,"o")],
  [(4,"o"),(5,"dd"),(7,"ffff"),(11,"dd"),(13,"ffffffff"),(21,"o"),(22,"oUUUUUUUUUo"),(34,"o")],
  [(4,"o"),(5,"d"),(6,"fff"),(9,"fff"),(12,"dddd"),(16,"ffffffff"),(24,"o"),(25,"oUUUUUUUUo")],
  [(3,"o"),(4,"d"),(5,"fff"),(8,"ffff"),(12,"dddd"),(16,"ffffffff"),(24,"o"),(25,"oUUUUUUUo"),(34,"o")],
  [(3,"o"),(4,"dw"),(6,"kkk"),(10,"fff"),(13,"o"),(14,"cccccc"),(20,"dddd"),(25,"o"),(26,"oUUUUUUo"),(34,"o")],
  [(3,"o"),(4,"d"),(5,"wkk"),(8,"ffff"),(12,"o"),(13,"cccccc"),(19,"ddddd"),(25,"o"),(26,"oUUUUUo"),(34,"o"),(37,"o")],
  [(3,"o"),(4,"d"),(5,"kkk"),(8,"ffff"),(12,"o"),(13,"cc"),(15,"oo"),(18,"cc"),(20,"dddd"),(25,"o"),(27,"oUUUUo"),(34,"o"),(37,"oh")],
  [(3,"o"),(4,"d"),(5,"kkk"),(8,"fffff"),(13,"ccccccc"),(20,"dddd"),(25,"o"),(28,"oUUo"),(34,"o"),(36,"ohho")],
  [(3,"o"),(4,"dcc"),(7,"cccc"),(11,"o"),(12,"cccccccc"),(20,"ddddd"),(26,"o"),(30,"oUUo"),(35,"o"),(38,"ho")],
  [(3,"o"),(4,"cc"),(6,"cccccc"),(12,"cc"),(14,"cccccccc"),(22,"dddd"),(26,"o"),(30,"oUo"),(34,"o"),(38,"o")],
  [(3,"o"),(4,"cccccc"),(10,"c"),(11,"ccccccc"),(18,"cc"),(20,"cccc"),(24,"ddddd"),(30,"o"),(34,"o")],
  [(3,"o"),(4,"ccccccc"),(11,"cccccccc"),(19,"cccccc"),(25,"dddd"),(29,"o"),(34,"o")],
  [(3,"o"),(4,"dcccccc"),(11,"cc"),(13,"cccccccc"),(21,"cc"),(23,"cccc"),(27,"ddd"),(30,"o"),(34,"o")],
  [(3,"o"),(4,"ddcccc"),(10,"cccccccc"),(18,"cccccccc"),(26,"dddd"),(30,"o"),(34,"o")],
  [(3,"o"),(4,"ddcc"),(8,"cccccc"),(14,"cccccccc"),(22,"cccccc"),(28,"dd"),(30,"o"),(34,"o")],
  [(3,"o"),(4,"ddc"),(7,"cccccc"),(13,"c"),(14,"ccccccc"),(21,"cc"),(23,"cccc"),(27,"o"),(30,"oo"),(34,"o")],
  [(4,"o"),(5,"dccc"),(9,"cccccc"),(15,"cccccccc"),(23,"cccc"),(27,"o"),(31,"o"),(34,"o")],
  [(5,"o"),(6,"dcc"),(10,"cccccc"),(16,"cccccc"),(24,"oo"),(28,"o"),(31,"o"),(34,"o")],
  [(7,"oooooooooooooo"),(21,"o"),(24,"o"),(27,"o"),(30,"o"),(34,"o")],
  [(6,"o"),(8,"cc"),(10,"oo"),(21,"o"),(24,"o"),(27,"o"),(30,"o"),(34,"o")],
  [(6,"o"),(9,"o"),(20,"o"),(23,"oo"),(28,"o"),(31,"o")],
  [(5,"oo"),(9,"oo"),(20,"oo"),(24,"oo"),(28,"oo"),(31,"oo")],
])

# ---------- 🌿 CỔ THỤ LINH (stage 3, 48×48) ----------
# Cổ thụ thành tinh: tán lá khổng lồ điểm hoa hồng, mặt gỗ trầm ngâm,
# nhánh tay hai bên, rễ bám đất — Ancient-Oak.
# --- Cổ Thụ Linh v3: quái vật cây — tán lớn đắp vai, tay vuốt tách khối, chân có khe ---
def _gen_co_thu_linh():
    N = 48
    g = [["."] * N for _ in range(N)]
    def put(x, y, ch):
        if 0 <= x < N and 0 <= y < N: g[y][x] = ch
    def rect(x0, y0, x1, y1, ch):
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1): put(xx, yy, ch)

    # ===== THÂN + ĐẦU (một khối vỏ cây) =====
    rect(14, 13, 33, 34, "m")           # khối chính: đầu+thân
    rect(12, 20, 35, 34, "m")           # vai nông rộng hơn
    # vằn vỏ
    for yy in range(13, 35):
        for xx in (17, 21, 27, 31):
            if g[yy][xx] == "m" and yy % 3 != 0: g[yy][xx] = "M"
    # rêu vai
    rect(12, 20, 15, 23, "d"); rect(32, 20, 35, 23, "d")

    # ===== MẶT =====
    # lông mày chữ V sâu
    for i in range(7):
        put(21 - i * 7 // 6, 14 + i // 2, "M")
        put(26 + i * 7 // 6, 14 + i // 2, "M")
    for xx in range(20, 28): put(xx, 17, "M")
    # mắt đỏ
    rect(17, 19, 19, 20, "R"); rect(28, 19, 30, 20, "R")
    put(18, 19, "e"); put(29, 19, "e")
    # mũi hố
    put(23, 21, "M"); put(24, 21, "M")
    # miệng răng cưa há rộng
    rect(16, 24, 31, 29, "k")
    for i, xx in enumerate(range(16, 32)):
        put(xx, 24 + (1 if i % 3 else 0), "M")
        put(xx, 29 - (1 if i % 2 else 0), "M")
    for xx in (19, 23, 27): put(xx, 26, "e"); put(xx, 27, "R")

    # ===== TAY: dính vai, chúc xuống NGOÀI khối thân, có đường phân cách =====
    rect(6, 22, 14, 28, "m")            # tay trái ngang
    rect(6, 26, 11, 42, "m")            # cẳng tay trái dọc ngoài
    put(8, 30, "M"); put(7, 35, "M"); put(9, 39, "M")
    rect(33, 22, 41, 28, "m")           # tay phải
    rect(36, 26, 41, 42, "m")
    put(39, 30, "M"); put(40, 35, "M"); put(38, 39, "M")
    # đường phân cách tay/thân
    for yy in range(28, 43): put(12, yy, "o")
    for yy in range(28, 43): put(35, yy, "o")
    # vuốt 3 ngón xòe
    rect(3, 42, 5, 45, "m"); rect(7, 43, 9, 46, "m"); rect(11, 42, 13, 45, "m")
    rect(34, 42, 36, 45, "m"); rect(38, 43, 40, 46, "m"); rect(42, 42, 44, 45, "m")
    for x in (4, 8, 12, 35, 39, 43): put(x, 45 if x in (4,12,35,43) else 46, "M")

    # ===== CHÂN: tách khỏi tay, khe trong suốt giữa 2 chân =====
    rect(13, 34, 22, 44, "m"); rect(25, 34, 34, 44, "m")
    for yy in range(35, 44):
        if yy % 3: g[yy][17] = "M"; g[yy][31] = "M"
    # bàn chân rễ loe
    rect(10, 44, 23, 46, "m"); rect(24, 44, 37, 46, "m")
    for xx in range(10, 24):  put(xx, 47, "o" if xx % 5 == 0 else ("M" if xx % 3 else "m"))
    for xx in range(24, 38):  put(xx, 47, "o" if xx % 5 == 0 else ("M" if xx % 3 else "m"))
    # khe chân trong suốt: khoét rows 34-43 cột 23-24
    for yy in range(34, 44):
        g[yy][23] = "."; g[yy][24] = "."

    # ===== TÁN LÁ KHỔNG LỒ ĐẮP XUỐNG VAI =====
    blob = None
    def _blob(cx, cy, rx, ry, ch):
        for yy in range(N):
            for xx in range(N):
                if ((xx-cx)/rx)**2 + ((yy-cy)/ry)**2 <= 1: put(xx, yy, ch)
    _blob(24, 6, 21, 7, "f")
    _blob(11, 8, 7, 4, "f"); _blob(37, 8, 7, 4, "f")
    _blob(24, 4, 12, 5, "l")
    _blob(16, 3, 5, 3, "h"); _blob(31, 3, 5, 3, "h")
    _blob(24, 9, 15, 4, "d")            # lớp lá tối rủ trán
    rect(17, 11, 30, 12, "d")            # mép rủ trước mặt

    # ===== VIẾN SILHOUETTE (chỉ viền ngoài & khe hở) =====
    sil = {"m","M","f","l","h","d","R","e","k"}
    for yy in range(N):
        for xx in range(N):
            if g[yy][xx] == ".":
                for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
                    nx, ny = xx+dx, yy+dy
                    if 0 <= nx < N and 0 <= ny < N and g[ny][nx] in sil:
                        g[yy][xx] = "o"; break
    return ["".join(r) for r in g]

S("coThuLinh", "thao", 3, 48, "grass", _gen_co_thu_linh())

def main():
    grids = {}
    errors = []
    for name, d in W.items():
        size = d["size"]
        rows = []
        all_rows = list(d["rows"]) + [[] for _ in range(size - len(d["rows"]))]
        for idx, row in enumerate(all_rows):
            try:
                rows.append(compose(size, row))
            except AssertionError as e:
                errors.append(f"{name} row {idx}: {e}")
        grids[name] = dict(line=d["line"], stage=d["stage"], size=size,
                           palette=d["palette"], grid=rows)

    if errors:
        print("LỖI SEGMENT:"); print("\n".join(errors)); sys.exit(1)

    # kiểm tra ký tự ↔ palette
    for name, g in grids.items():
        pal = PALETTES[g["palette"]]
        for i, row in enumerate(g["grid"]):
            for ch in row:
                if ch != "." and ch not in pal:
                    print(f"LỖI: {name} row {i}: ký tự '{ch}' không có trong palette {g['palette']}")
                    sys.exit(1)

    # render JS
    def js_palette(p):
        out = []
        for ch, (hx, ten) in p.items():
            out.append(f'      {ch}: {{ hex: "{hx}", ten: "{ten}" }},')
        return "\n".join(out)

    lines_meta = {
        "lua": ("Lửa", ["Tàn Lửa", "Bồng Bột", "Diễm Long"]),
        "thuy": ("Thủy", ["Giọt", "Suối Vọt", "Triều Long"]),
        "thao": ("Thảo", ["Mầm", "Búp Xanh", "Cổ Thụ Linh"]),
    }
    js_meta = "    lines: {\n" + ",\n".join(
        f'      {k}: {{ ten: "{v[0]}", stage: {json.dumps(v[1], ensure_ascii=False)} }}'
        for k, v in lines_meta.items()) + "\n    },"

    js_grids = []
    for name, g in grids.items():
        rows_js = ",\n".join(f'          "{r}"' for r in g["grid"])
        js_grids.append(
            f'    {name}: {{ line: "{g["line"]}", stage: {g["stage"]}, size: {g["size"]}, '
            f'palette: "{g["palette"]}", grid: [\n{rows_js}\n    ] }}')
    js = f"""/* ============================================================
   SPRITE DATA — Pet Pomodoro (chuẩn skill pet-sprite-art)
   AUTO-GENERATED bởi scripts/gen-sprites.py — đừng sửa tay,
   sửa definition trong script rồi chạy lại: python3 scripts/gen-sprites.py
   Stage 1 = 32×32 · Stage 2 = 40×40 · Stage 3 = 48×48
   Shiny = cùng grid + palette thay thế (fireShiny).
   ============================================================ */

globalThis.SPRITE_DATA = {{
  meta: {{
{js_meta}
  }},
  palettes: {{
{chr(10).join(f'    {k}: {{' + chr(10) + js_palette(v) + chr(10) + '    },' for k, v in PALETTES.items())}
  }},
  grids: {{
{','.join(js_grids)}
  }},
}};
"""
    with open("design-demos/sprite-data.js", "w") as f:
        f.write(js)
    print(f"OK — {len(grids)} sprite → design-demos/sprite-data.js")

if __name__ == "__main__":
    main()
