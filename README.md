# LuyenTu 2.0

Ứng dụng Android cá nhân học từ vựng tiếng Anh/IELTS, xây dựng lại theo các tính năng có thể xác định từ LuyenTu 1.4.0. Ưu tiên Samsung Galaxy S22 Ultra.

## Nguyên tắc bắt buộc

- Miễn phí, không quảng cáo, không tài khoản, không gói premium.
- Không backend, analytics, cloud sync hay API từ xa.
- Ứng dụng không khai báo quyền `INTERNET`; dữ liệu học lưu trên thiết bị.
- Người dùng tự nhập bộ từ; không bắt buộc tải bộ từ có sẵn.
- Các tính năng phụ thuộc vào dịch vụ Android (ví dụ TTS) chỉ hoạt động offline khi thiết bị đã cài sẵn dữ liệu giọng đọc offline.
- Mã nguồn gốc Flutter của LuyenTu không có trong archive đã cung cấp; dự án này tái tạo hành vi/tính năng tương đương, không phải bản sao nguyên mã nguồn.

## Đã triển khai trên nhánh `luyentu2.0`

- Kotlin + Jetpack Compose; SQLite cục bộ.
- Sổ từ: thêm, sửa, xoá, tìm kiếm, lọc theo chủ đề; trường từ, nghĩa, IPA, ví dụ, chủ đề.
- Nhập CSV và TXT/TSV; xuất CSV và TXT/TSV; nhận diện nhiều tên cột; bỏ qua từ trùng.
- Các dạng luyện tập: thẻ từ, trắc nghiệm, nghe/phát âm và gõ từ, điền từ vào câu, ghép từ-nghĩa, dịch nghĩa sang tiếng Anh, ôn hỗn hợp.
- Lịch ôn cách quãng nâng cấp: Again/Hard/Good/Easy, khoảng ôn thay đổi theo kết quả, hệ số dễ, số lần quên, bước học lại và ưu tiên từ quá hạn.
- Thống kê tổng từ, lượt ôn, từ đến hạn, từ đã vững; biểu đồ hoạt động 7 ngày và lịch sử ôn gần đây.
- Chuỗi ngày học, lộ trình cột mốc offline và mục tiêu ôn tập hằng ngày có thể chọn.
- Nhắc ôn cục bộ hằng ngày lúc 20:00 (tuỳ chọn; cần cấp quyền thông báo).
- Widget màn hình chính hiển thị từ vựng, nghĩa, chuỗi học và số từ đến hạn; nhấn widget để mở ứng dụng.
- Sao lưu/khôi phục JSON đầy đủ từ vựng, trạng thái SRS, lịch sử ôn và các cài đặt mục tiêu/nhắc nhở. Khôi phục thay thế dữ liệu hiện có.
- GitHub Actions chạy kiểm thử import/export và build APK debug.

## Còn thiếu so với mục tiêu tái tạo đầy đủ

- Hai mini-game Monkey Rescue và Flappy.
- Tùy chỉnh avatar/widget và lựa chọn từ hiển thị; widget hiện dùng biểu tượng sách và từ đầu tiên theo thứ tự bảng chữ cái.
- Hiệu chỉnh thuật toán SRS sau khi đối chiếu hành vi thực tế; lịch gốc chưa thể xác minh chính xác từ APK đã biên dịch.
- Kiểm thử UI, hiệu năng, thông báo và offline trên Galaxy S22 Ultra thật.
- Xác nhận CI thành công cho phiên bản mới nhất và kiểm tra APK đầu ra.

Các tính năng online trong ứng dụng gốc như tài khoản, chat, leaderboard dùng máy chủ, mua hàng/subscription và đồng bộ đám mây được loại bỏ theo yêu cầu miễn phí/offline một người dùng; không thay bằng dịch vụ từ xa.

## Build

Yêu cầu JDK 17, Android SDK Platform 35 và Gradle 8.9+.

```bash
gradle testDebugUnitTest assembleDebug
```

APK debug dự kiến tại `app/build/outputs/apk/debug/app-debug.apk`. Chỉ coi APK sẵn sàng khi GitHub Actions hoàn tất thành công.

## Quyền riêng tư

Ứng dụng không khai báo quyền `INTERNET`. Dữ liệu từ vựng, tiến độ và lịch sử ôn được lưu cục bộ. Khi tự chọn tệp từ một nhà cung cấp lưu trữ đám mây trong bộ chọn Android, chính nhà cung cấp đó có thể cần mạng; đây không phải yêu cầu của ứng dụng học.
