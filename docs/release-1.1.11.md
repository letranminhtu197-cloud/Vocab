# Ghi chú phát hành Vocab 1.1.11

Ngày 2026-10-23. Môi trường: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35.

## Bối cảnh

Người dùng gửi 3 video YouTube, 1 video TikTok và 3 bài blog mochidemy.com. Video/TikTok chỉ lấy được
tiêu đề qua oEmbed (không có phụ đề/khung hình), nên phân tích dựa trên 3 bài blog đọc được đầy đủ.
Chi tiết ở `docs/mochivocab-upgrade-1.1.11.md`.

## Đã thêm trong bản này

| Thay đổi | Nguồn cảm hứng (blog MochiVocab) |
|---|---|
| **Nút "Nghe chậm"** (TTS tốc độ 0.6×) trong thẻ bài học và chi tiết từ | Flashcard MochiVocab có "phát âm thường (hình loa) và phát âm chậm (hình ốc sên)" |
| **Biểu đồ phân bố 5 cấp ghi nhớ** ở tab Thống kê | Mục "5 cấp độ từ vựng" và "biểu đồ 5 cấp lý tưởng" (cấp 4–5 nhiều, cấp 1–3 ít) |
| Tỷ lệ "đã nhớ tốt" (cấp 4–5) | Cùng mục 5 cấp độ |

Phần còn lại của kế hoạch (dịch câu ví dụ, tạm dừng ôn từ, nhắc theo thời điểm vàng, gói khóa học JSON)
được ghi rõ trong tài liệu phân tích; các mục cần đổi schema sẽ làm theo đợt migration riêng.

## Kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **99 test, 0 failure** (17 lớp) — tăng từ 93 |
| Test mới | `LevelDistributionTest` (6 ca) |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK | `Vocab-1.1.11-debug.apk`, versionCode `121`, versionName `1.1.11`, 16.799.465 byte |
| SHA-256 | `F4C92827BF9DBCF625023A2B621CC3838831E29987D1F79A6DB5C53C535CD2F5` |
| Xác nhận DEX | Có "Phân bố cấp ghi nhớ", "Nghe chậm", "Đã nhớ tốt" |
| Quyền | Không có `android.permission.INTERNET` |
| Schema | Không đổi (Room version 3) |

## Giới hạn xác minh

- Không xem/phân tích được nội dung video YouTube và TikTok (chỉ có tiêu đề qua oEmbed).
- Chưa push GitHub (không có credential) → chưa có lượt CI mới; APK do máy cục bộ build.
- Chưa cài trên thiết bị thật.
