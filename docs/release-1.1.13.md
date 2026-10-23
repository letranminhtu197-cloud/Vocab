# Ghi chú phát hành Vocab 1.1.13

Ngày 2026-10-23. Môi trường: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35.

## Bước 2 + 3 trong kế hoạch nâng cấp

### 1. Tạm dừng ôn một từ (giống "bỏ tick" trong sổ tay MochiVocab)

- Thêm cờ `paused` vào `WordEntity`; từ bị tạm dừng **vẫn nằm trong sổ và giữ nguyên cấp SRS**, nhưng
  không xuất hiện trong: hàng đợi flashcard, phiên học, quiz (mọi chế độ), thử thách hằng ngày và nhắc
  "thời điểm vàng".
- Bật/tắt ở 2 nơi: công tắc trong dialog sửa từ, và nút **"Tạm dừng ôn từ này" / "Ôn lại từ này"** ở
  chi tiết từ.
- Danh sách từ có nhãn "Tạm dừng ôn" để dễ nhận biết.

### 2. Dịch nghĩa câu ví dụ

- Thêm trường `exampleMeaning` (dịch câu ví dụ) — đúng cặp "câu ví dụ + dịch nghĩa" trong flashcard MochiVocab.
- Hiển thị ở thẻ bài học ("Dịch: …"), chi tiết từ, và trong dialog sửa từ.
- CSV/backup giữ trường mới; nhập CSV nhận cả tiêu đề tiếng Anh (`exampleMeaning`) và tiếng Việt
  (`nghĩacâuvídụ`, `tạmdừng`), giá trị tạm dừng nhận `1/true/yes/có`.

### 3. Migration Room 3 → 4

```sql
ALTER TABLE words ADD COLUMN exampleMeaning TEXT NOT NULL DEFAULT '';
ALTER TABLE words ADD COLUMN paused INTEGER NOT NULL DEFAULT 0;
```

- Dữ liệu cũ được giữ nguyên; từ đã lưu nhận giá trị mặc định (chuỗi rỗng, chưa tạm dừng).
- Đăng ký qua `addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`.
- **Chưa có migration test chạy trên thiết bị/emulator** (không có Robolectric trong dự án và chưa cài
  APK lên máy). Vì vậy nên **sao lưu ZIP trước khi cập nhật**; nếu có lỗi mở database, báo lại ngay.

## Kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **111 test, 0 failure** (19 lớp) — tăng từ 105 |
| Test mới | `WordCsvFieldsTest` (3 ca), +1 ca paused cho `StudyQueueLogicTest`, `DailyChallengeLogicTest`, `GoldenTimeLogicTest` |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK | `Vocab-1.1.13-debug.apk`, versionCode `123`, versionName `1.1.13`, 16.815.849 byte |
| SHA-256 | `F4B3709A223FB6FDB5A13750AC57810B5B3D4EE973A099AE52A7ED3A3D6237A1` |
| Xác nhận DEX | Có "Tạm dừng ôn", "Dịch nghĩa câu ví dụ", "Ôn lại từ này" |
| Quyền | Không có `android.permission.INTERNET` |
| Schema | **4** (thêm 2 cột) |

## Giới hạn xác minh

- Chưa push GitHub (không có credential) → chưa có lượt CI mới.
- Chưa cài trên thiết bị thật nên migration 3 → 4 chưa được kiểm chứng runtime; khuyến nghị sao lưu trước.
