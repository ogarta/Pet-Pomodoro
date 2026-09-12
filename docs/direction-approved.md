# Direction Approved — Pet Sprite Art

## Lịch sử chọn hướng

### Lần 1 (2026-09-11 sáng) — workshop 3 hướng grid tay
- 3 hướng: A · GBA cổ điển / B · Ruby nhiều tông / C · Chibi max-cute
  (`design-demos/sprite-workshop.html`)
- **User chọn: A (GBA cổ điển)** — kèm feedback "vẫn còn xấu".

### Lần 2 (2026-09-11) — user đưa 3 ảnh mẫu mới ⇒ HƯỚNG CUỐI CÙNG
- User cung cấp 3 ảnh chuẩn chất lượng (image-gen): dòng fox lửa, chart đa hệ + lai tạo,
  dòng cá 3 mốc "Mốc 1/2/3". Yêu cầu: "demo dựa vào hình ảnh trên để tạo pet và distill
  skill phù hợp vẽ pet dự án".
- **Hướng chốt: đúng chất 3 ảnh mẫu.** Hai lớp bàn giao:
  1. **Sprite chuẩn** = tách trực tiếp từ ảnh mẫu (crop + xóa nền flood-fill + lọc debris)
     → `design-demos/sprites/ref/*.png` (25 sprite, manifest `ref-sprites.js`)
  2. **Skill `pet-sprite-art`** = distill quy trình (style DNA + prompt recipe sinh pet mới
     + pipeline tách + palette 3 hệ + QC) → `~/.agents/skills/pet-sprite-art/`
- Grid pixel tay (sprite-data.js) hạ cấp thành "thử nghiệm game-ready", không phải deliverable chính.

## Mapping tên (spec §2.1) ↔ sprite mẫu
- 🔥 Tàn Lửa/Bồng Bột/Diễn Long ← Petyre/Fyreox/Infernyx (front + back + shiny)
- 💧 Giọt/Suối Vọt/Triều Long ← Shock-Fringe/Volt-Tide/Tide-Striker
- 🌿 Mầm/Búp Xanh/Cổ Thụ Linh ← Sproutling/Florasaur/Ancient-Oak
- Hệ phụ + lai (Aquawoof/Tidewolf/Giga-Hydro, Zap-Pup/Volt-Fox/Thundros, Aqua-Sprout,
  Inferno-Oak, Sprout-Zap) ← chart "Hệ Khác & Lai Tạo"

### Lần 3 (2026-09-11 tối) — user chọn KỊCH BẢN GAMEPLAY: KB1
- 3 demo kịch bản gameplay v2 đã trình: KB1 Hành Trình Tiến Hoá (Memphis) /
  KB2 Huấn Luyện Viện (Finch) / KB3 Lai Tạo & Pokédex (Console) —
  `design-demos/kich-ban-{1,2,3}-*.html` + `.png`, đều 7/7 click-test.
- **User chọn (nguyên văn): "KB1 tôi chọn"** →
  - **Gameplay**: KB1 Hành Trình Tiến Hoá — tiến hoá tuyến tính Lv.8 / Lv.16, nhánh
    Stage 3 (Hiền: streak ≥ 7 · Chiến Binh: sức mạnh ≥ 80), sức mạnh decay khi bỏ bê,
    biến thể ốm yếu (spec `docs/gameplay-spec.md` §2.4/§3/§4-KB1).
  - **Visual**: style Memphis Maximalism (demo 1) + sprite v3 từ ảnh mẫu user (mục Lần 2).
- Khi build thật, sprite line Lửa/Thủy/Thảo lấy từ `ref-sprites.js`/`ref-grids.js`
  (grid tay trong demo KB1 chỉ là fallback). Hệ phụ/Zap (chart mẫu) thêm sau qua
  catalog — đúng kiến trúc data-driven spec §1.
