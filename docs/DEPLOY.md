# DEPLOY — Triển khai Pet Pomodoro lên Firebase Hosting

> Làm sau khi xong `docs/SETUP.md` (đã có Firebase project + khối `firebaseConfig`).
> Toàn bộ file cấu hình deploy đã nằm sẵn ở repo root: `firebase.json`, `.firebaserc`, `firestore.rules`, `firestore.indexes.json`. Bạn chỉ cần sửa **một chỗ** (projectId) rồi chạy lệnh deploy.

---

## 0. Firebase.json cấu hình gì?

```json
{
  "hosting": {
    "source": "web",                        // ← Nuxt app trong thư mục web/, Firebase tự build & deploy (Web Frameworks support)
    "ignore": ["firebase.json", "**/.*", "**/node_modules/**"]
  },
  "firestore": {
    "rules": "firestore.rules",             // ← đúng bản rules ở SETUP.md bước 5
    "indexes": "firestore.indexes.json"     // ← hiện đang rỗng, thêm index phức tạp sau nếu cần
  },
  "emulators": {
    "auth":     { "port": 9099 },           // port mặc định của Firebase
    "hosting":  { "port": 5000 },
    "firestore": { "port": 8080 },
    "ui":       { "enabled": true, "port": 4000 },
    "singleProjectMode": true
  }
}
```

Đừng sửa trừ khi biết rõ mình đang làm gì — schema này đúng chuẩn Firebase (Xem: https://firebase.google.com/docs/cli/#firebasejson_reference).

---

## 1. Cài Firebase CLI

```bash
npm install -g firebase-tools
firebase --version   # kiểm tra, cần >= 13
```

## 2. Đăng nhập

```bash
firebase login
```

Trình duyệt mở ra → đăng nhập bằng **cùng Google account** đã tạo project ở SETUP.md.

## 3. THAY projectId trong `.firebaserc`  ← bắt buộc

File `.firebaserc` hiện đang trỏ tới placeholder:

```json
{
  "projects": {
    "default": "pet-pomodoro"   // ← thay bằng projectId THẬT của bạn
  }
}
```

Lấy projectId thật: Console Firebase → ⚙️ Project settings → tab General → mục **Project ID**
(cũng chính là giá trị `projectId` trong khối `firebaseConfig` bạn nhận ở SETUP.md bước 4).

Sửa xong lưu lại. Đây là chỗ duy nhất bạn phải tay vào config.

## 4. Deploy

```bash
firebase deploy --only hosting,firestore
```

Lần đầu chạy, nếu CLI hỏi/báo cần bật thử nghiệm, chạy thêm:

```bash
firebase experiments:enable webframeworks
```

và chạy lại lệnh deploy. (Lưu ý: từ firebase-tools v13 trở đi, **Web Frameworks support đã GA** — đa số máy không cần lệnh trên nữa, chỉ chạy khi được CLI nhắc.)

Lần deploy đầu có thể hơi lâu vì CLI phải cài build environment cho Nuxt. Xong sẽ in ra URL dạng:
`https://<projectId>.web.app` — mở lên kiểm tra.

## 5. `firebaseConfig` đặt ở đâu?

Khối config từ SETUP.md bước 4 sẽ nằm trong biến môi trường của Nuxt. Tạo file **`web/.env`** (không commit) với các key:

```env
NUXT_PUBLIC_FIREBASE_API_KEY=...
NUXT_PUBLIC_FIREBASE_AUTH_DOMAIN=...
NUXT_PUBLIC_FIREBASE_PROJECT_ID=...
NUXT_PUBLIC_FIREBASE_STORAGE_BUCKET=...
NUXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=...
NUXT_PUBLIC_FIREBASE_APP_ID=...
```

> ⚠️ Hiện tại (M1) app chưa đọc các key này — **M2** sẽ wire vào `nuxt.config.ts` qua `runtimeConfig.public` và bỏ qua bước env khi chạy offline. File `.env` chỉ cần sẵn từ bây giờ để M2 + deploy dùng ngay.

Với Hosting, khi deploy CLI sẽ build `web/` — đảm bảo `web/.env` có mặt trên máy build (chính là máy bạn), hoặc dùng `NUXT_PUBLIC_FIREBASE_*` như biến môi trường shell trước khi deploy.

## 6. Chạy emulator ở local

```bash
firebase emulators:start
```

- Hosting (app): http://localhost:5000
- Emulator UI:    http://localhost:4000
- Auth: :9099, Firestore: :8080

Lưu ý: emulator dùng lại **projectId thật** trong `.firebaserc`, nhưng data nằm local, không đụng production. Từ tab khác, chạy Nuxt dev trỏ về emulator (Auth + Firestore) để test đăng nhập không tốn quota — M2 sẽ hỗ trợ flag `NUXT_PUBLIC_FIREBASE_USE_EMULATOR=1`.

Lần đầu chạy emulator, nếu được hỏi tạo data export dir, chọn `y` và Enter là được (dir export chỉ dùng khi cần seed/di chuyển data).

## 7. Rollback cơ bản

- **Hosting**: vào Console → Hosting → tab **Release history** → nút **⋯** bên cạnh bản cũ → **Rollback**. Xong trong 10 giây.
- **Firestore rules**: Console → Firestore Database → tab **Rules** → mục **History** → xem/chọn bản cũ → **Publish**.
- Cách khác bằng CLI: mỗi lần deploy CLI in ra "Release version"; vào Console copy release id rồi rollback trên UI là nhanh nhất.

## 8. Golive checklist

- [ ] `.firebaserc` đã thay bằng **projectId thật** (bước 3)
- [ ] Firestore **Rules đã Publish** đúng nội dung `firestore.rules` (SETUP.md bước 5 — chỉ chủ `{uid}` đọc/ghi `users/{uid}/...`)
- [ ] **Google Sign-In đã bật** trong Authentication (SETUP.md bước 2)
- [ ] `firebase deploy --only hosting,firestore` chạy **thành công**, không đỏ
- [ ] Mở `https://<projectId>.web.app` → **smoke test**:
  - [ ] Đăng nhập Google OK
  - [ ] Chạy 1 phiên pomodoro → data sync lên Firestore (tab `users/{uid}/...` trong Console hiện document)
- [ ] (Tuỳ) Custom domain trong Hosting nếu muốn dùng domain riêng

Xong hết các mục trên = golive.
