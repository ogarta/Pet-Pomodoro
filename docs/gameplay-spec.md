# Gameplay Spec v2 — Tiến hoá · Lai tạo · Sức mạnh

> **Đây là NGUỒN CHÂN LÝ DUY NHẤT** cho 3 demo kịch bản gameplay mới của PetPomodoro.
> Mọi agent làm demo phải đọc file này TRƯỚC, mọi con số/tên/điều kiện đều lấy từ đây, không tự chế.
> Demo cũ (huong-1/2/3) chỉ tham khảo **visual style**, gameplay cũ (Đói/Vui/Mochi) đã BỊ THAY THẾ.

---

## 0. Thay đổi so với gameplay cũ

| Cũ | Mới |
|---|---|
| 3 loài mèo/cún/thỏ (Mochi, Bánh Bao, Bún) | **3 line nguyên tố mới** kiểu Pokemon starter (mục 2) |
| 4 stage tiến hoá không định nghĩa | **Egg → Stage 1 → Stage 2 → Stage 3**, có **nhánh branch** + **form lai hiếm** |
| Meter Đói / Vui | **Sức mạnh** (power) + **Thể trạng** (condition) |
| Chỉ focus tăng XP | XP tăng bằng **cả focus lẫn cho ăn**; sức mạnh chỉ luyện bằng focus |
| Không phạt bỏ bê | **Ốm yếu**: bỏ lâu → level GIỮ NGUYÊN nhưng ngoại hình rũ xuống, sức mạnh sụp |

Yêu cầu gốc của user (không được vi phạm):
1. Hình dạng pet kiểu Pokemon (starter 3 nguyên tố, tiến hoá đổi hẳn dáng).
2. Tăng level nhờ cho ăn **hoặc** ngồi tập trung.
3. Tiến hoá 1–2 lần theo mốc; có case đặc biệt cần thêm điều kiện (sức mạnh X, duy trì thói quen Y ngày).
4. Sức mạnh phải duy trì mỗi ngày; học đều rồi bỏ lâu, quay lại thì **chỉ còn level** — ngoại hình ốm yếu.
5. **Kiến trúc mở**: dễ thêm hệ mới (module data-driven, không sửa core).

### Changelog điều chỉnh (sau khi spec được duyệt)

- **2026-09-12 — Stage 2 hạ từ Lv.8 → Lv.5** (đã duyệt): tiến hoá đầu đến sau ~25 phiên
  (~8 ngày theo mục tiêu 3 phiên/ngày) thay vì ~56 phiên (~19 ngày), nhằm giữ người mới.
  Stage 3 giữ Lv.16; mọi điều kiện nhánh/khác không đổi. Các bảng dưới ghi "Lv.8" giữ nguyên
  như bản gốc để đối chiếu lịch sử — **code là nguồn chân lý hiện hành**
  (`web/shared/game/catalog.ts` · `android/.../game/Catalog.kt: LEVEL_STAGE_2 = 5`).

---

## 1. Kiến trúc mở rộng (bắt buộc thể hiện trong code demo)

Mỗi demo NHÚNG một config hệ thống đúng cấu trúc dưới đây (JS object) và **render UI từ config**
(meter, cây tiến hoá, Pokédex, điều kiện… đều đọc từ data, không hard-code rải rác).
Đây là "bằng chứng sống" của kiến trúc — thêm hệ mới chỉ là thêm entry.

### 1.1 Core tối thiểu
```
core = { speciesId, stageId, level, xp, condition(0-100), coins, streak, todaySessions, todayMinutes }
```
Core chỉ có **event bus**: `session_complete(minutes)`, `feed`, `day_end`, `level_up`, `status_change`.
Core KHÔNG biết gì về hệ nào cả.

### 1.2 Module hệ (mỗi hệ = 1 khai báo)
```js
const SYSTEMS = [
  { id: 'level',     on: { session_complete: +XP(phút), feed: +5XP }, emits: ['level_up'] },
  { id: 'power',     on: { session_complete: +8 power }, decay: { perIdleDay: -6, perGoalDay: -2 }, ui: [{slot:'focus', type:'meter', label:'Sức mạnh'}] },
  { id: 'evolution', on: { level_up: checkEvolution }, rules: [...], ui: [{slot:'stats', type:'evo_tree'}] },
  { id: 'collection',ui: [{slot:'desktop', type:'pokedex'}] },
];
```
UI component đọc `SYSTEMS[].ui` để biết vẽ gì ở slot nào.

### 1.3 Registry điều kiện (dùng chung, combo AND/OR)
```js
COND = {
  level_at_least:  (s, v) => s.level  >= v,
  power_at_least:  (s, v) => s.power  >= v,
  streak_at_least: (s, v) => s.streak >= v,
  healthy_days_at_least: (s, v) => s.healthyDays >= v,
  sessions_today_at_least: (s, v) => s.todaySessions >= v,
  and: (s, of) => of.every(c => eval(c)),   or: (s, of) => of.some(c => eval(c)),
};
```
Case đặc biệt = tổ hợp loại điều kiện. Thêm loại mới = đăng ký 1 key, KHÔNG sửa hệ cũ.

### 1.4 Catalog sinh vật (data-driven, thêm loài = thêm entry)
```js
{ id:'flame', name:'Lửa', stages:[{atLevel:1,id:'tan-lua',name:'Tàn Lúa'...}, ...],
  branches:[{id:'hien', cond:{...}}, {id:'chien-binh', cond:{...}}],
  hybridsWith:['water','grass'] }
```

### 1.5 Firestore (mục tiêu tương lai, chỉ ghi trong spec)
- `users/{uid}/state/pet` — core (trên).
- `users/{uid}/systems/{systemId}` — mỗi hệ 1 doc (`power`, `evolution`, `collection`…).
Thêm hệ không đụng doc hệ cũ; delta update bằng `increment()`.

### 1.6 Ví dụ hệ tương lai (ghi demo trong UI hoặc comment code, chứng minh tính mở)
```js
// Ngày mai thêm hệ Nhiệm vụ hằng ngày — KHÔNG đụng core:
registerSystem({ id:'daily_quests', on:{ session_complete: quests.check, feed: quests.check },
                 ui:[{slot:'home_bottom', type:'quest_list'}] });
registerConditionType('total_sessions_at_least', (s,v) => s.stats.totalSessions >= v);
// Hệ Đấu trường: cond 'power_at_least' có sẵn → chỉ thêm config đối thủ.
```

---

## 2. Bộ sinh vật (dùng CHUNG cả 3 demo — tên phải khớp CHÍNH XÁC)

### 2.1 Ba line starter
| Nguyên tố | Stage 1 (Lv.1) | Stage 2 (Lv.8) | Stage 3 (Lv.16) |
|---|---|---|---|
| 🔥 Lửa | **Tàn Lửa** (hổ lửa con) | **Bồng Bột** (thiếu niên lửa, mọc cánh mầm) | **Diễm Long** (rồng lửa nhỏ) |
| 💧 Thủy | **Giọt** (gấu nước) | **Suối Vọt** (cá heo nước ngọt) | **Triều Long** (rồng sóng) |
| 🌿 Thảo | **Mầm** (hạt nảy mầm) | **Búp Xanh** (hươu lá) | **Cổ Thụ Linh** (linh thú cây cổ tích) |

### 2.2 Nhánh branch (chỉ ở Stage 3, chọn 1 trong 2 + mặc định)
| Branch | Điều kiện (registry) | Ý nghĩa |
|---|---|---|
| **Hiền** (aura dịu) | `and[level≥16, streak≥7]` | Phần thưởng thói quen đều đặn |
| **Chiến Binh** (giáp, uy phong) | `and[level≥16, power≥80]` | Phần thưởng sức mạnh cao |
| (mặc định) | chỉ `level≥16` | Form thường |

Tên form branch: `Diễm Long Hiền` / `Diễm Long Chiến Binh` (thủy, thảo tương tự).

### 2.3 Form lai hiếm (case đặc biệt, điều kiện KÉP)
| Form lai | Nguyên tố | Điều kiện |
|---|---|---|
| **Hơi Nước Bốc** | Lửa × Thủy | `and[power≥85, streak≥10]` |
| **Diễm Hoa** | Thảo × Lửa | `and[power≥85, streak≥10]` |
| **Sen Mưa** | Thủy × Thảo | `and[power≥85, streak≥10]` |

### 2.4 Biến thể ỐM YẾU — phải KHÁC BIỆT RÕ (silhouette vẽ RIÊNG, cấm chỉ đổi màu)
> Feedback người dùng: bản "phai màu + rũ 1px" chưa khác biệt đủ. Chuẩn mới dưới đây áp cho MỌI form khi thể trạng <30. Tiêu chí nghiệm thu: **nhìn mờ (blur mắt) vẫn phân biệt được khỏe vs ốm** — tức khác nhau từ silhouette, không phải từ màu.

1. **Silhouette riêng (grid vẽ lại, không phải patch màu)**: đầu cúi thấp 2–3px, lưng gù, tai/vây/cánh rũ sát thân, bụng hóp (viền lõm vào 1–2px), chân co lệch, tổng thể xìu xuống ~15% chiều cao, bộ phận nguyên tố (đuôi/mào lửa, vây, lá) chỉ còn xiêu vẹo.
2. **Nguyên tố "tắt"**: pet Lửa → đuôi/mào lửa thay bằng **vệt khói nhỏ** (2–3px, chekered, rung); Thủy → bong bóng xẹp còn vệt nước; Thảo → lá rủ xuống.
3. **Mặt**: mắt NHẮM HẲN (đường cong xuống) + lông mày chéo buồn + miệng wavy ~— không được dùng lại mặt mắt mở của bản khỏe.
4. **Màu**: nhợt/desaturate so với bản khoẻ (palette màu v3) — vẫn giữ outline đậm để không biến mất.
5. **Chuyển động**: idle đổi từ bounce khoẻ khoắn sang **shiver** (rung ±1px, ~400ms/lần) + 2 giọt mồ hôi nhỏ bay quanh đầu.
6. **Hiệu ứng phụ**: vũng bóng tối/mồ hôi dưới chân + bubble "…" phía trên đầu.
→ Vẫn data-driven: grid ốm là override data của form đó, bật khi `condition < 30` (band §3).

### 2.5 Sprite pixel-art — CHUẨN HIỆN TẠI (v3): Pokémon MÀU, phức tạp tăng theo stage
> Lịch sử: v1 chibi 16×16 nhiều màu → v2 Game Boy đơn sắc DMG (bỏ) → **v3 (theo feedback user): sprite CÓ MÀU, tham chiếu Pokemon thật, mỗi stage phức tạp hơn rõ rệt — nhìn vào thấy ngay là bản tiến hoá.**

**Quy tắc render (áp cho MỌI demo):**
- **THAM CHIẾU bộ starter Pokemon thật** cho ngôn ngữ hình (dáng + màu + độ phức tạp), nhưng GIỮ tên sinh vật Việt của mục 2.1 (không lấy tên Pokemon — tránh IP khi lên production):
  - 🔥 Lửa: **Charmander → Charmeleon → Charizard** (cam/đỏ, bụng kem, đuôi lửa; stage 3 mọc cánh)
  - 💧 Thủy: **Squirtle → Wartortle → Blastoise** (xanh dương, mai, đuôi xoắn; stage 3 có ống nước vai)
  - 🌿 Thảo: **Bulbasaur → Ivysaur → Venusaur** (xanh lá + bầu hạt/lá trên lưng; stage 3 nở hoa)
- **CÓ MÀU**: outline tối (nâu-đen gần đen), thân 2–3 tông + highlight, sprite pixel **≥24×24** (stage 3 khuyến nghị 28–32).
- **ĐỘ PHỨC TẠP TĂNG THEO STAGE (tiêu chí nghiệm thu)**: đặt 3 stage cạnh nhau phải "đọc" được ngay chiều tiến hoá —
  - Stage 1: tròn nhỏ, ngố, 1 đặc trưng nguyên tố (đuôi lửa nhỏ / bong bóng đầu / mầm lá trên lưng)
  - Stage 2: cao + dài hơn, thêm chi tiết (mào / cánh mầm / vây, đuôi phát triển, mắt dữ hơn)
  - Stage 3: **đổi hẳn dáng** (đứng thẳng / rồng), thêm đặc trưng huyền thoại (cánh lớn, sừng, hoa nở, ống nước hiểm)
- **Stage/podium**: BỎ khung màn DMG đơn sắc; pet đặt theo stage của từng style demo (Memphis: thẻ khung mực; Finch: podium pastel; Console: màn màu kiểu GBC — giữ bezel + lưới pixel nhưng nền sáng màu, không đơn sắc).
- **Battle HUD giữ nguyên cấu trúc** (tên + Lv + thanh HP kiểu Gen-1) nhưng thanh HP màu kiểu Pokemon: xanh lá → vàng → đỏ theo % thể trạng.
- **Biến thể ốm yếu**: theo §2.4 (silhouette vẽ riêng, nguyên tố tắt, mắt nhắm…) nhưng ở **palette nhợt/desaturate** của màu próp.

**Grid 16×16 bên dưới chỉ còn là tham chiếu dáng silhouette, không ràng buộc palette:**

**Grid tham khảo line Lửa — dáng gốc 16×16** (palette lửa cũ không dùng nữa, chỉ lấy dáng):

**Tàn Lửa** (palette lửa: f cam #E8663C, y vàng #F5B840, c kem #F7E7CE, p hồng #E88A9A):
```
"................",
"......o..o......",
".....oyo.oyo....",
"....oyfyooyfyo..",
"...ooffyyffoo...",
"..offccccccffo..",
".ofccccccccccfo.",
".ofcoccccccocfo.",
".ofccccccccccfo.",
".ofpccccccccpfo.",
".ofcccccaacccfo.",
"..offccccccffo..",
"..offffffffffo..",
"...offffffffo...",
"....ooffffoo....",
"................",
```
**Bồng Bột** (thêm mào lửa giữa đầu + cánh mầm `F`):
```
"................",
".......o........",
"......oyo.......",
".....oyyyo......",
"...ooyyyyyyoo...",
"..offffffffffo..",
".offccccccccffo.",
".ofcoccccccocfo.",
".ofccccccccccfo.",
".ofpccccccccpfo.",
".ofccccaaaaccfo.",
"..offffffffffo..",
".offFffffffFffo.",
"..ooffffffffoo..",
"...oo.oooo.oo...",
"................",
```
**Diễm Long** (sừng + đầu rồng + thân dày):
```
"..o..........o..",
"..oyo......oyo..",
"..oyyyo...oyyyo.",
"...oyffyoyffyo..",
"..ooffyyyyffoo..",
".offccccccccffo.",
".ofcoccccccocfo.",
".ofccccccccccfo.",
".ofpccccccccpfo.",
".ofcccccaacccfo.",
"..offffffffffo..",
"..offffffffffo..",
".ooffffffffffoo.",
"..ooffffffffoo..",
"...ooFFFFFFoo...",
"....oo....oo....",
```
- Line Thủy/Thảo: KHÔNG cần grid chi tiết — dựng theo mô tả (2.1) + cùng khung dáng 3 stage như line Lửa, đổi ngôn ngữ chi tiết (vây/bong bóng nước vs lá/chồi). Ở demo 3 (Pokédex) các form chưa mở chỉ cần **silhouette** (một màu tối) — rẻ và đúng chất sưu tầm.

---

## 3. Công thức số (CHUNG, không đổi giữa các kịch bản)

| Đại lượng | Công thức / giá trị |
|---|---|
| XP lên mức | `need(L) = 80 + 30×L` → Lv.4→5 cần 200 XP |
| XP focus | +1 XP / phút focus (giữ công thức cũ) |
| XP cho ăn | +5 XP / lần ăn |
| Sushi coin | +1 / phiên hoàn thành; cho ăn tốn 1 sushi |
| Cap Sức mạnh theo level | `cap(L) = min(100, 40 + 6×L)` → Lv.4 cap 64, Lv.8 cap 88 |
| Tăng Sức mạnh | +8 / phiên focus hoàn thành |
| Mục tiêu ngày | ≥ 3 phiên focus |
| Decay Sức mạnh | ngày không phiên: −6; ngày đủ mục tiêu: −2 |
| Thể trạng | +15 khi cho ăn; −10 mỗi ngày không ăn & không phiên |
| Trạng thái thể trạng | ≥70 **Khỏe mạnh**, 30–69 **Bình thường**, <30 **Ốm yếu** (sprite ốm) |
| Level khi bỏ bê | **GIỮ NGUYÊN** (XP không mất) — chỉ ngoại hình & sức mạnh sụp |

**State fixture dùng chung mọi demo** (để 3 demo so sánh được số liệu như nhau):
`Lv.4 · 120/200 XP · Sức mạnh 62/64 · Thể trạng 78 (Khỏe mạnh) · 12 sushi · Streak 7 ngày · Hôm nay 112 phút / 3 phiên (đủ mục tiêu) · phiên đang chạy 24:31 (phiên 2/4)`

---

## 4. Ba kịch bản demo

> ⚠️ **Contract kỹ thuật BẮT BUỘC (để pass `scripts/verify-demo.mjs <file> --pet "Tàn Lửa"`)**:
> 1. Không pageerror khi load (React 18.3.1 UMD + Babel pinned + SRI y hệt demo cũ).
> 2. Body có text `24:31` và `Tàn Lửa`.
> 3. Nút `Tạm dừng` → bấm xong phải xuất hiện `Tiếp tục`.
> 4. Tab bar nhãn chính xác: `Trứng`, `Tập`, `Ăn`, `Stats` (bấm chữ `Ăn` khớp chính xác). Tab Ăn có nút feed chứa `Cho ăn`/`cho ăn`/`Ăn 1 sushi`; bấm xong phải hiện đúng một trong: `thích lắm`, `còn 11`, `× 11`, `×11` (khởi đầu 12 sushi, ăn 1 lần → 11).
> 5. Tab `Trứng` phải có heading `Chọn quả trứng`.
> 6. Tab `Stats` phải có chữ `Streak` hoặc `Đồng bộ`.
> Bố cục trang: 4 AndroidFrame (nhãn ①②③④) + 1 BrowserWindow (⑤), mỗi phone 1 state machine độc lập — copy `AndroidFrame`/`BrowserWindow` verbatim từ demo cũ tương ứng.
> Nút tương tác NEW bắt buộc mỗi demo: **"Mô phỏng tiến hoá"** (animation lóe sáng → đổi form theo mốc), **"Mô phỏng bỏ bê 5 ngày"** (decay → sprite ốm + panel "Level giữ nguyên, ngoại hình ốm yếu").

### Kịch bản 1 — HÀNH TRÌNH TIẾN HOÁ (style Memphis — `huong-1-roulette-memphis.html`)
Điểm nhấn: khoảnh khắc tiến hoá + cây tiến hoá cổ điển.
- ① Trứng: `Chọn quả trứng` — 3 trứng nguyên tố (lửa/thủy/thảo), chọn → "Nhận Tàn Lửa về nhà".
- ② Tập: focus 24:31 + meter Sức mạnh 62/64 (vạch cap theo level!) + Thể trạng 78.
- ③ **Màn TIẾN HOÁ** (nút từ ②): timeline 4 form line Lửa render từ catalog; nút "Mô phỏng tiến hoá" → animation lóe + rung → Tàn Lửa → Bồng Bột → Diễm Long; panel branch: 2 thẻ điều kiện `Hiền (streak≥7)` vs `Chiến Binh (power≥80)` đọc từ registry.
- ④ Stats: streak 7 + hôm nay + history + sync Google (giữ chất demo cũ) + chip "Sắp tiến hoá: Lv.8".
- ⑤ Desktop: **CÂY TIẾN HOÁ** 3 line render từ catalog (form đã biết màu, chưa biết mờ/silhouette) + stats 2 cột.

### Kịch bản 2 — HUẤN LUYỆN VIỆN (style Finch pastel — `huong-2-thamchieu-finch.html`)
Điểm nhấn: sức mạnh là trung tâm + cửa ốm yếu chặn tiến hoá.
- ① Trứng (y hệt contract).
- ② Tập: thẻ "+8 Sức mạnh khi hoàn thành phiên · mục tiêu 3 phiên/ngày · tiến độ 2/4"; thanh Sức mạnh có vạch cap; chuỗi ngày luyện (dot 7 ngày: đủ/thiếu).
- ③ Ăn + **Chăm sóc**: cho ăn (−1 sushi, +15 thể trạng, +5 XP, pet "thích lắm"); thẻ "Mô phỏng bỏ bê 5 ngày" → Thể trạng 78→28 → **Ốm yếu**: sprite rũ + mờ màu, banner "Tàn Lửa vẫn Lv.4 nhưng ốm yếu — ngoại hình rũ xuống"; timeline hồi phục "Cho ăn + 1 phiên/ngày × 3 ngày → Khỏe lại".
- ④ Stats + CỬA TIẾN HOÁ: checklist điều kiện đọc từ registry — "Lv.8 ✓(4/8) · Khỏe mạnh 3 ngày liên tục (1/3) · Sức mạnh ≥70 (62/70)" → chưa đủ → nút mô phỏng bấm đủ điều kiện → tiến hoá.
- ⑤ Desktop: biểu đồ phút tuần + timeline sức mạnh/thể trạng 7 ngày (decay nhìn thấy được) + thẻ tiến hoá.

### Kịch bản 3 — LAI TẠO & POKÉDEX (style Console LCD — `huong-3-designer-console.html`)
Điểm nhấn: sưu tầm — chọn nhánh, săn form lai, Pokédex silhouette.
- ① Trứng: chọn trên LCD, chip "ĐANG CHỌN" như demo cũ.
- ② Tập: màn LCD với block-meter Sức mạnh/Thể trạng + LV.4 chip.
- ③ **LAI TẠO** (nút/tab phụ): chọn hướng nhánh ở Stage 2 (chọn 1/3 nguyên tố trời sinh thêm); panel form lai **Hơi Nước Bốc** với điều kiện kép `power≥85 ∧ streak≥10` + progress bar 62/85 · 7/10; nút "Mô phỏng đủ điều kiện" → lóe LCD → hiện form lai + tag "HIẾM".
- ④ Stats + nút "Mô phỏng nghỉ 2 tuần" → power 62→14, Thể trạng 78→18, sprite ốm, dòng LCD nhấp nháy "LV GIỮ NGUYÊN · NGOẠI HÌNH ỐM YẾU".
- ⑤ Desktop = **POKÉDEX**: lưới 19 form (3 line × [3 stage + 2 branch + 1 hybrid] + trứng) render từ catalog — đã mở: màu; chưa mở: silhouette "???"; click form → panel điều kiện (registry) + số liệu. Nút mô phỏng mở khóa 1 form bất kỳ.

---

## 5. Chống trùng lặp giữa 3 demo (bắt buộc)
- 3 demo cùng data nhưng **khác bố cục骨架**: Memphis = thẻ xoay nghiêng/sticker; Finch = thẻ tròn pastel + podium; Console = vỏ máy cầm tay + LCD + nút A/B. Không được 2 demo cùng một cấu trúc màn.
- Đúng DNA style demo cũ: copy nguyên tắc màu/font/chất liệu từ file `huong-*` tương ứng, KHÔNG trộn style.
- Tiếng Việt toàn bộ, sai khác ngôn ngữ tự nhiên của style (Console có thể viết hoa kiểu LCD "LV.4 · SỨC MẠNH 62/64").
