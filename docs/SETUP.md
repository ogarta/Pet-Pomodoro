# SETUP — Tạo Firebase cho Pet Pomodoro

> Bạn tự làm phần này 1 lần (~10 phút), xong gửi lại 1 đoạn config cho tôi.
> Gói free (Spark) là đủ cho app cá nhân. Trong lúc bạn làm, tôi build M1 (web offline, chưa cần Firebase).

## 1. Tạo project

1. Mở https://console.firebase.google.com → **Add project** (hoặc Create a project).
2. Tên project: `pet-pomodoro` (tuỳ ý).
3. **Google Analytics**: tắt (không cần) → Create project.

## 2. Bật Google Sign-In (Auth)

1. Menu trái → **Build → Authentication → Get started**.
2. Tab **Sign-in method** → chọn **Google** → Enable.
3. *Project support email*: chọn email của bạn → Save.

## 3. Tạo Firestore

1. **Build → Firestore Database → Create database**.
2. Vị trí: `asia-southeast1` (Singapore — gần VN nhất) → Next.
3. Chế độ **Start in production mode** (mặc định khoá, sẽ thêm rules ở bước 5) → Enable.

## 4. Lấy config Web (đoạn cần gửi lại cho tôi)

1. Console → ⚙️ **Project settings** (bên cạnh Project Overview) → tab **General**.
2. Mục **Your apps** → biểu tượng **</>** (Web) → tên `petpomodoro-web` → Register app.
3. Copy khối `firebaseConfig` hiện ra (gồm apiKey, authDomain, projectId, storageBucket, messagingSenderId, appId) → **gửi nguyên khối này cho tôi**.

## 5. Firestore Rules (paste vào Firestore → Rules → Publish)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```
(Chỉ chủ tài khoản đọc/ghi data của chính mình — đây là bản tối thiểu, đã đủ an toàn vì data nằm trong `users/{uid}/...`.)

## 6. (Tùy sau) Android

Khi web xong sẽ làm tiếp: Project settings → **Add app → Android** (package name tôi sẽ cung cấp, dạng `vn.petpomodoro.app`) → tải `google-services.json` đặt vào thư mục Android. Chưa cần làm bây giờ.

## Checklist gửi lại cho tôi

- [ ] Khối `firebaseConfig` (bước 4)
- [ ] Đã paste Rules (bước 5)
- [ ] Đã bật Google Sign-In (bước 2)
