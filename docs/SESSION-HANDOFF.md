# Pet Pomodoro — Bàn giao session (handoff)

> File này cho phép MỌI session ZCode mới mở tại project này lấy lại toàn bộ ngữ cảnh.
> Session gốc: `sess_d335d73b-0efa-4a98-9ded-3627c0cd64e7` (mở tại workspace default).
> Trong session mới, có thể dùng `ReadSessionContext` với ID trên để truy vấn thêm chi tiết.

## Dự án là gì
**Pet Pomodoro** — app Pomodoro nuôi thú ảo pixel-art, 2 nền tảng + đồng bộ:
- **Web**: Nuxt 4 + Vue 3 + TS + Firebase JS SDK
- **Android**: Kotlin + Jetpack Compose + Firestore Android SDK
- **Backend**: Firebase free (Spark) — Firestore + Auth Google Sign-In
- **Đăng nhập**: Google Sign-In (Android: Credential Manager one-tap; web: popup)
- UI tiếng Việt, 3 loài pet (mèo Mochi / cún Bánh Bao / thỏ Bún), 4 stage tiến hóa,
  XP +1/phút focus, sushi coin +1/phiên, meter Đói/Vui decay theo thời gian, streak ngày.

## Trạng thái hiện tại (2026-09-12 — M1.5 fix thực dụng XONG)
- ✅ **M1.5 — FIX THỰC DỤNG (web + Android)**, kế thừa toàn bộ M1/M3-prep:
  - **Báo hết giờ**: web có chuông Web Audio (không cần file) + Web Notification (chỉ khi tab ẩn,
    xin quyền lúc bấm Bắt đầu) + `document.title` đếm ngược/flash `🔔 Hết giờ rồi!`;
    Android có 4 WAV sinh bởi `scripts/gen-sounds.py` (SoundPool trong app + MediaPlayer trong
    TimerReceiver khi process chết) + NotificationChannel "timer" + AlarmManager
    `setAndAllowWhileIdle`. Tuỳ chọn `alerts {sound, notify}` persist 2 nền tảng
    (web: `state/meta.alerts`, Android: DataStore `settings.*`), mặc định bật.
  - **Timer sống qua process death (Android)**: snapshot `{phase, running, endAtWallMs,
    remainingMs, totalMs}` vào DataStore; mở lại app vẫn tính phiên nếu hết giờ lúc vắng
    (ngang hàng `restoreTimer` web).
  - **Hủy phiên 2 bước** (web + TrainScreen Android): "Hủy" → "Chắc hủy?" (3s revert) → mới hủy.
  - **Backup dùng chéo web ↔ Android**: JSON shape `SerializedDocs` (Firestore docs).
    Web: Xuất/Nhập ở Cài đặt (validate qua docsToState, import 2 bước). Android: SAF
    CreateDocument/OpenDocument + `game/Backup.kt` (map loài flame↔lua, date ↔ epochDay).
  - **PWA (web)**: `@vite-pwa/nuxt` (autoUpdate, manifest inline trong nuxt.config, devOptions off) +
    4 icon từ `brand/` vào `web/public/` + head (favicon-32, manifest link, theme-color,
    apple-touch-icon). Verify: `web/scripts/pwa-verify.mjs` (chạy trên build/preview, ALL PASS —
    dev không inject manifest/SW là hành vi đúng của vite-pwa).
  - **Dọn copy dev** khỏi UI: bỏ "M1 · KB1", "KỊCH BẢN KB1 · M1", slot chips, "(REGISTRY COND)",
    "CAP"→"TỐI ĐA", toast level-up hết công thức `need(L)=…`, joiner `∧/∨` → "và/hoặc".
  - **Nhịp tiến hoá: Stage 2 hạ Lv.8 → Lv.5** (Stage 3 giữ Lv.16) — tiến hoá đầu sau ~25 phiên
    (~8 ngày). Sửa cả `Catalog.kt` + test Android. Ghi chú trong `docs/gameplay-spec.md` §0.
- ✅ Verify 2026-09-12: web `typecheck` 0 lỗi · `build` OK · e2e `web/scripts/m1-verify.mjs` ALL PASS
  (29 assert: decay/persist/ốm + confirm hủy + title + toggle alerts + export/import + icon) ·
  `pwa-verify.mjs` ALL PASS (12) · screenshots refresh · Android `testDebugUnitTest` 18/18 ·
  `assembleDebug` OK.
- Ghi chú kỹ thuật: cài `@vite-pwa/nuxt` từng làm vỡ build ("Cannot find native binding" — bug
  optional deps của npm); đã xử lý bằng `npm i @rolldown/binding-darwin-arm64 --no-save`. Nếu
  `npm ci` lại gặp lỗi tương tự thì cài binding đó.

## Trạng thái trước đó (2026-09-11 tối)
- ✅ **GATE ĐÃ MỞ — mọi quyết định đã chốt**, chi tiết tại `direction-approved.md` (root):
  - **Gameplay v2**: kịch bản **KB1 Hành Trình Tiến Hoá** (Memphis) — spec đầy đủ tại
    `docs/gameplay-spec.md` (kiến trúc module data-driven §1, catalog §2, công thức §3, KB1 §4).
  - **Sprite**: v3 màu, tách từ 3 ảnh mẫu user → `design-demos/ref-sprites.js` / `ref-grids.js`
    (grid tay trong demo chỉ là fallback). Skill vẽ pet: `~/.agents/skills/pet-sprite-art/`.
  - Demo kịch bản: `design-demos/kich-ban-{1,2,3}-*.html` (đều 7/7; KB1 là chuẩn tham chiếu UI).
- ✅ 3 demo UI cũ (huong-1/2/3) vẫn pass 7/7 — `scripts/verify-demo.mjs` giờ hỗ trợ `--pet "Tên"`.
- ✅ **M1 WEB XONG** (`web/`): Nuxt 4.4.5 — gameplay core TS ở `shared/game/` (event bus +
  SYSTEMS + COND registry + catalog, persist adapterFirestore-shaped), 4 màn KB1
  (Trứng/Tập/Tiến hoá/Stats), timer 25/5 thật, decay/streak thật, EvoOverlay,
  dev-QA `?dev=1`. `npm run build` 0 lỗi, e2e 17/17 (`web/scripts/m1-verify.mjs`),
  screenshots `web/docs/m1-screens/`.
- ✅ **M3-PREP ANDROID XONG** (`android/`): Kotlin + Compose offline core, engine + 18 unit
  test pass, APK debug `android/app/build/outputs/apk/debug/app-debug.apk`, emulator smoke OK.
- ✅ **INFRA XONG**: `firebase.json` (hosting source=web) + `.firebaserc` (placeholder
  `pet-pomodoro`) + `firestore.rules` + `docs/DEPLOY.md` + `brand/` (PWA icons + manifest).
- ⏳ **M2 — CHỜ USER**: làm `docs/SETUP.md` → gửi firebaseConfig → thay projectId trong
  `.firebaserc` + tạo `web/.env` (NUXT_PUBLIC_FIREBASE_*) → gắn Auth Google + Firestore sync
  → copy `brand/` vào `web/public/` → `firebase deploy`.
- ⬜ M4: Android gắn Firebase (google-services.json) + test sync 2 nền tảng.

## Quy ước quan trọng
- **Thư mục dự án**: `/Users/be_gt/Projects/persional/PetPomodoro` (dùng đường dẫn tuyệt đối)
- Quy trình design theo skill `huashu-design` (3 hướng → duyệt → build); demo là single-file
  HTML React inline, mở bằng double-click.
- Pixel pet + công thức XP/decay/coin là dữ liệu dùng chung 2 nền tảng (sẽ tách `sprites.ts` / `Sprites.kt`).
- Firestore: `users/{uid}/state/pet`, `users/{uid}/state/stats`, `users/{uid}/sessions/{id}`;
  delta update bằng `increment()`, last-write-wins theo `updatedAt`.
