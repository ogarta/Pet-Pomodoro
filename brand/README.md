# brand/ — Bộ nhận diện & icon PWA của Pet Pomodoro

Thư mục này chứa bộ icon PWA + web manifest của app, sinh ra từ sprite hero
**Tàn Lửa (Petyre)** — `design-demos/sprites/ref/tanLua.png` (112x114, front).

## Danh sách file

| File | Kích thước | Dùng làm |
|---|---|---|
| `icon-192.png` | 192x192 | Icon PWA cỡ nhỏ (manifest `icons`) |
| `icon-512.png` | 512x512 | Icon PWA cỡ lớn — bản master (manifest `icons`) |
| `maskable-512.png` | 512x512 | Icon Android adaptive/maskable (manifest `purpose: "maskable"`) |
| `favicon-32.png` | 32x32 | Favicon trình duyệt (`<link rel="icon">`) |
| `manifest.webmanifest` | — | Web app manifest |

Tất cả icon đều là sprite Tàn Lửa đặt **giữa canvas vuông màu kem `#F2E9D8`**,
scale nguyên lần bằng nearest-neighbor (giữ nét pixel-art):
- `icon-512`: sprite x4 (448x456) trên canvas 512.
- `maskable-512`: sprite x2 (224x228) trên canvas 512 — chừa viền đủ rộng để
  nằm gọn trong safe-zone tròn của Android (nửa đường chéo sprite ~160px <
  bán kính safe-zone ~205px).
- `icon-192` / `favicon-32`: resize mượt từ bản master 512 bằng `sips`.

## Copy sang `web/public/` ở M2

> **Chưa copy bây giờ.** Khi M2 wire PWA/Nuxt config, copy toàn bộ file trong
> thư mục này vào `web/public/`:

```
brand/icon-192.png      → web/public/icon-192.png
brand/icon-512.png      → web/public/icon-512.png
brand/maskable-512.png  → web/public/maskable-512.png
brand/favicon-32.png    → web/public/favicon-32.png
brand/manifest.webmanifest → web/public/manifest.webmanifest
```

Và trong `web/app` (hoặc qua `nuxt.config.ts` head) tham chiếu:

```html
<link rel="icon" type="image/png" href="/favicon-32.png">
<link rel="manifest" href="/manifest.webmanifest">
<meta name="theme-color" content="#F2E9D8">
```

`manifest.webmanifest` đã ghi sẵn path `/icon-192.png`, `/icon-512.png`,
`/maskable-512.png` (tính từ gốc domain) — khớp với vị trí trong `web/public/`.
