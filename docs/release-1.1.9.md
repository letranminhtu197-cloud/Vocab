# Ghi chú phát hành Vocab 1.1.9

Ngày 2026-10-23. Môi trường: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35.

## Thiết kế lại theo phong cách app học ngoại ngữ phổ biến

Lấy cảm hứng từ *phong cách chung* của các app học từ vựng (Duolingo, Quizlet, MochiVocab…) — nút dày,
thanh tiến độ chia đoạn, màu tươi, nhãn chuỗi ngày — nhưng mọi thành phần đều là thiết kế riêng của Vocab,
không dùng hình ảnh, font, âm thanh hay bố cục cụ thể của ứng dụng nào.

| Thành phần | Mô tả |
|---|---|
| `VocabTheme` | Bảng màu mới: xanh lá tươi `#2FB344`, vàng `#FFC93C`, xanh dương `#3D8BFF`; nền trắng, thẻ viền mảnh 2px; hỗ trợ dark mode |
| `VocabButton` | Nút "dày" có cạnh dưới tối hơn 4dp, lún 3dp khi nhấn (animation bằng `animateDpAsState`); 4 màu trạng thái |
| `SegmentProgressBar` | Thanh tiến độ chia đoạn, mỗi đoạn tô dần theo tiến độ phiên |
| `ProgressRing`, `StreakPill`, `ConfettiBurst` | Vòng mục tiêu ngày, nhãn chuỗi ngày, giấy màu hoàn thành (đều vẽ bằng Canvas) |

Áp dụng:
- Trang chủ: nút CTA chính và nút "Bắt đầu thử thách" dùng `VocabButton`.
- Phiên học: thanh tiến độ chia đoạn theo số từ, 4 nút đánh giá Lại/Khó/Tốt/Dễ dùng `VocabButton`
  (đỏ/vàng/xanh lá/xanh dương), nút "Lật thẻ" dùng `VocabButton`.
- Trắc nghiệm: thanh tiến độ chia đoạn, nút "Câu tiếp theo / Hoàn thành phiên" dùng `VocabButton`.

## Kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **93 test, 0 failure** (16 lớp) — không đổi vì chỉ sửa UI |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK | `Vocab-1.1.9-debug.apk`, versionCode `119`, versionName `1.1.9`, 16.783.077 byte |
| SHA-256 | `9103F0511AE669FB5CC71848F55939E45A4393549A24B9D11638BD516B4E8BB6` |
| Xác nhận DEX | Có `VocabButton` và `SegmentProgressBar` (1.1.8 không có) |
| Quyền | Không có `android.permission.INTERNET` |
| Schema | Không đổi (Room version 3) |

## Không làm

- Không sao chép giao diện, mascot, icon, font, âm thanh, câu chữ hay nội dung khóa học của MochiVocab,
  Duolingo, Quizlet hay bất kỳ ứng dụng thương mại nào.
- Không bẻ khóa hay bỏ giới hạn trả phí của ứng dụng khác.

## Giới hạn xác minh

- Chưa push GitHub (không có credential) → chưa có lượt CI mới; APK do máy cục bộ build.
- Chưa cài trên thiết bị thật; cần xem trực tiếp để tinh chỉnh khoảng cách, màu và hiệu ứng.
