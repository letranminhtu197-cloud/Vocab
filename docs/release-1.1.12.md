# Ghi chú phát hành Vocab 1.1.12

Ngày 2026-10-23. Môi trường: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35.

## "Thời điểm vàng" theo lịch ôn thật

Đây là bước 1 trong kế hoạch nâng cấp đã thống nhất (`docs/mochivocab-upgrade-1.1.11.md`).

| Hạng mục | Trước | Sau |
|---|---|---|
| Thời điểm nhắc | Cố định 19:00 mỗi ngày | Tính từ **lịch ôn của từng từ**: nhắc ở mốc gần nhất sắp tới |
| Nội dung thông báo | Câu cố định | "Có N từ đang đến hạn…" (đếm từ thực tế trong sổ) |
| Trang chủ | Không có | Thẻ **⏰ Thời điểm vàng** hiển thị từ nên ôn + thời gian, kèm nút **Ôn ngay** |
| Sau khi mở thông báo | Không đặt lại lịch | Tự tính và đặt mốc kế tiếp |
| Sau khi khởi động máy | Đặt lại 19:00 | Đọc sổ từ và đặt lại theo mốc gần nhất |

Chi tiết kỹ thuật:
- `GoldenTimeLogic.kt` (mới): đếm từ đến hạn, chọn từ nên ôn sớm nhất, tính mốc nhắc kế tiếp,
  nhãn "hôm nay/mai/dd/MM", nội dung thông báo. Thuần Kotlin, có 6 unit test.
- `StudyReminder`: thêm `scheduleGoldenTime` dùng `setAndAllowWhileIdle` (không cần quyền alarm đặc biệt),
  dùng chung request code với lịch dự phòng nên không bắn trùng; receiver dùng `goAsync()` + Room để
  lấy dữ liệu mà không chặn main thread.
- `MainActivity`: tính nhãn thời điểm vàng và đặt lại alarm mỗi khi sổ từ thay đổi.
- `VocabDashboard`: thẻ thời điểm vàng phía trên thử thách ngày.

Không đổi schema, không thêm quyền, vẫn 100% offline. Giới hạn: `setAndAllowWhileIdle` có thể bị Android
trì hoãn vài phút khi tiết kiệm pin (ghi rõ trong app để không hứa chính xác tuyệt đối).

## Kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **105 test, 0 failure** (18 lớp) — tăng từ 99 |
| Test mới | `GoldenTimeLogicTest` (6 ca) |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK | `Vocab-1.1.12-debug.apk`, versionCode `122`, versionName `1.1.12`, 16.815.845 byte |
| SHA-256 | `9EA31DE5F62EE685C5C609EDBE006D752B0DA9F53AC140DE504CBA268108C8FF` |
| Xác nhận DEX | Có "Thời điểm vàng", "nên ôn", "đang đến hạn" |
| Quyền | Không có `android.permission.INTERNET` (chỉ dùng các quyền cũ) |
| Schema | Không đổi (Room version 3) |

## Giới hạn xác minh

- Chưa push GitHub (không có credential) → chưa có lượt CI mới.
- Chưa cài trên thiết bị thật nên chưa đo được độ trễ thực tế của alarm trên máy.
