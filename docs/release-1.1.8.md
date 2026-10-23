# Ghi chú phát hành Vocab 1.1.8

Ngày 2026-10-23. Môi trường: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35.

## Làm mới giao diện theo hướng "gamified" (thiết kế riêng của Vocab)

Bản này thay lớp giao diện Material mặc định bằng bộ thiết kế riêng, lấy cảm hứng từ *phong cách* ứng dụng
học từ vựng (thẻ lớn, màu pastel, tiến độ trực quan, phần thưởng khi hoàn thành) nhưng **không sao chép**
hình ảnh, mascot, font, âm thanh, câu chữ hay bố cục cụ thể của ứng dụng nào.

| Hạng mục | Nội dung |
|---|---|
| Bảng màu & hình khối | `AppTheme.kt`: mint + vàng + hồng, nền sáng, thẻ bo tròn 20–34dp, hỗ trợ dark mode |
| Trang chủ | Vòng tiến độ mục tiêu ngày vẽ bằng `Canvas` (`ProgressRing`), nhãn chuỗi ngày `StreakPill` |
| Phiên bài học | Dãy chấm tiến độ theo từng từ, nút đánh giá có màu riêng (Lại/Khó/Tốt/Dễ), giữ khoảng thời gian ôn |
| Hoàn thành phiên | Hiệu ứng giấy màu (`ConfettiBurst` vẽ bằng `Canvas`, không dùng Lottie/asset) |
| Trắc nghiệm | Mỗi lựa chọn có nhãn A/B/C/D, màu đúng/sai theo bảng màu |

Tất cả hình vẽ đều bằng Compose `Canvas`/`Surface`; không thêm thư viện, không thêm ảnh, không thêm quyền.

## Kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **93 test, 0 failure** (16 lớp) — giữ nguyên vì bản này chỉ đổi UI |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK | `Vocab-1.1.8-debug.apk`, versionCode `118`, versionName `1.1.8`, 16.783.077 byte |
| SHA-256 | `926BC5A2FEC652FB4A7F75406507EB4064AB6468BA7405001AC3927DD66D7460` |
| Quyền | Không có `android.permission.INTERNET` |
| Schema | Không đổi (Room version 3) |
| Tệp mới | `AppTheme.kt` (theme + ProgressRing + StreakPill + ConfettiBurst) |

## Điều không làm

- Không sao chép giao diện, mascot, icon, font, âm thanh, câu chữ hay nội dung khóa học của MochiVocab
  hoặc bất kỳ ứng dụng thương mại nào.
- Không bẻ khóa, không bỏ giới hạn trả phí của ứng dụng khác.

## Giới hạn xác minh

- Chưa push GitHub (không có credential) → chưa có lượt CI mới; APK do máy cục bộ build.
- Chưa cài trên thiết bị thật; giao diện mới cần được xem trực tiếp để tinh chỉnh khoảng cách/màu.
