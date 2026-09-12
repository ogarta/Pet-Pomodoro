# PetPomodoro — Hành Trình Tiến Hoá 🐣

Ứng dụng Pomodoro nơi thú cưng ảo **tiến hoá** theo mỗi phiên tập trung hoàn thành. Ngôn ngữ tiếng Việt, phong cách hình ảnh **Memphis Maximalism** với sprite pixel. Hướng gameplay đã duyệt: **KB1 "Hành Trình Tiến Hoá"** — tiến hoá tuyến tính tại Lv.8 / Lv.16 với nhánh phân kì ở Stage 3, pet yếu dần nếu bị bỏ bê.

## Cấu trúc repo (monorepo)

| Thư mục | Nội dung |
|---|---|
| `web/` | Ứng dụng web **Nuxt 4 + Vue 3 + TypeScript** (milestone M1 — offline core, Firebase dự kiến M2). Logic game thuần TS nằm ở `web/shared/game/`. |
| `android/` | Ứng dụng Android native **Kotlin + Jetpack Compose** (namespace `vn.petpomodoro.app`, minSdk 26). |
| `docs/` | Tài liệu thiết kế, setup, deploy, biên bản handoff. |
| `scripts/` | Script sinh asset sprite (Python) và script chụp ảnh/xác minh UI (Node + Playwright). |
| `brand/` | Asset thương hiệu: PWA manifest, favicon, icon. |
| `design-demos/` | Demo thiết kế HTML, sprite tham khảo và dữ liệu nguồn cho pipeline sinh asset. |

Cấu hình Firebase đặt ở root: `firebase.json`, `.firebaserc`, `firestore.rules`, `firestore.indexes.json`.

## Chạy thử

### Web (Nuxt 4)

```bash
cd web
npm install
npm run dev
```

Các lệnh khác: `npm run build`, `npm run generate`, `npm run preview`, `npm run typecheck`.

### Android

Mở thư mục `android/` bằng Android Studio (hoặc `./gradlew installDebug` với SDK đã cấu hình). Bản hiện tại: `0.3.0-m3prep`.

### Firebase

Firestore chỉ cho phép mỗi user đọc/ghi dữ liệu của chính mình (xem `firestore.rules`). Hướng dẫn deploy chi tiết nằm ở [`docs/DEPLOY.md`](docs/DEPLOY.md).

## Tài liệu

- [`docs/gameplay-spec.md`](docs/gameplay-spec.md) — spec gameplay chi tiết
- [`docs/direction-approved.md`](docs/direction-approved.md) — các quyết định hướng thiết kế/gameplay đã duyệt
- [`docs/SETUP.md`](docs/SETUP.md) — thiết lập môi trường
- [`docs/DEPLOY.md`](docs/DEPLOY.md) — deploy Firebase Hosting
- [`docs/SESSION-HANDOFF.md`](docs/SESSION-HANDOFF.md) — ghi chú bàn giao giữa các phiên làm việc

## Pipeline asset

Sprite pixel được sinh bằng script Python trong `scripts/` (ví dụ `gen-sprites.py`), đọc dữ liệu tham khảo từ `design-demos/` (`sprites/`, `grids-from-ref/`, `reference/`). Kết quả cuối nằm ở `web/public/pets/` và `android/app/src/main/res/drawable-nodpi/`.
