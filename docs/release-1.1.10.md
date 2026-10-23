# Ghi chú phát hành Vocab 1.1.10

Ngày 2026-10-23. Môi trường: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35.

## Sắp xếp lại giao diện và luồng thao tác

| Màn hình | Trước | Sau |
|---|---|---|
| Trang chủ | Nhắc học chiếm vị trí đầu tiên | Bắt đầu bằng hành động học (tổng quan, mục tiêu, thử thách); nhắc học chuyển xuống cuối |
| Sổ từ | 3 hàng chip + 2 nút nhập/xuất + 2 nút sao lưu luôn hiển thị | Một thanh công cụ gọn: số từ + menu **Sắp xếp** + nút **Dữ liệu** (mở mới thấy nhập/xuất/sao lưu) |
| Luyện tập | 14 chip chế độ trải ngang, phải cuộn | 4 chế độ dùng nhiều nhất hiển thị sẵn; phần **"Tất cả chế độ luyện tập"** mở rộng khi cần |
| Thông tin | Ghi chú CSV luôn chiếm chỗ | Thu gọn trong mục Dữ liệu, giữ nguyên khả năng nhập CSV/TSV/TXT và sao lưu ZIP |

Chi tiết kỹ thuật:
- `MainActivity`: thêm trạng thái `showDataTools`, thanh công cụ dùng `DropdownMenu` cho sắp xếp; đổi thứ tự `LazyColumn` ở tab Trang chủ.
- `QuizScreen`: thêm trạng thái `showAllModes`, nhóm chip theo mức dùng; giữ nguyên logic chấm/SRS.
- Không đổi dữ liệu, không đổi schema, không thêm quyền hay thư viện.

## Kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **93 test, 0 failure** (16 lớp) |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK | `Vocab-1.1.10-debug.apk`, versionCode `120`, versionName `1.1.10`, 16.799.465 byte |
| SHA-256 | `D310A72E6FE3E07CB9D0A19D55E803982EBC6BD97AE16B2BF04D3B4F63032C57` |
| Xác nhận DEX | Có "Chế độ luyện", "Tất cả chế độ luyện tập", "Sắp xếp: ", "Dữ liệu" |
| Quyền | Không có `android.permission.INTERNET` |
| Schema | Không đổi (Room version 3) |

## Giới hạn xác minh

- Chưa push GitHub (không có credential) → chưa có lượt CI mới; APK do máy cục bộ build.
- Chưa cài trên thiết bị thật; bố cục mới cần xem trực tiếp để tinh chỉnh.
