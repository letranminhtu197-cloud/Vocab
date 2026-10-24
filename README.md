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
- Thư viện bộ từ và thư mục riêng trong SQLite: tạo, đổi tên, xóa, chuyển bộ vào thư mục, chọn bộ để luyện; liên kết từ với bộ và nâng cấp cơ sở dữ liệu cũ lên schema 4.
- Sổ từ theo bộ: thêm, sửa, xóa, tìm kiếm, lọc theo chủ đề, chuyển từ giữa các bộ; trường từ, nghĩa, IPA, ví dụ và chủ đề.
- Nhập CSV/TXT/TSV theo lô trong một giao dịch SQLite; nhận diện tiêu đề Anh/Việt, bỏ qua từ trùng mà không ghi đè. Xuất CSV/TXT/TSV từ bộ đang mở hoặc toàn bộ thư viện.
- Các dạng luyện tập: thẻ từ, trắc nghiệm, nghe và gõ, điền từ theo câu/nghĩa, ghép nhiều cặp từ-nghĩa, dịch nghĩa sang tiếng Anh và ôn hỗn hợp.
- Phiên ôn có lựa chọn 10/20/30/50 từ và danh sách cố định khi bắt đầu; tiến độ cập nhật sau từng kết quả.
- SRS được tách thành lịch lập thuần có thể kiểm thử: Again/Hard/Good/Easy, khoảng ôn, hệ số dễ, số lần quên, bước học lại; sửa tính chuỗi học để tính cả ngày hôm nay khi đã ôn.
- Thống kê tổng từ, lượt ôn, từ đến hạn, từ đã vững, cấp độ Lv0–Lv5, biểu đồ hoạt động 7 ngày, bộ lọc từ và lịch sử ôn.
- Chuỗi ngày học, lộ trình cột mốc offline và mục tiêu ôn tập hằng ngày có thể chọn.
- Nhắc ôn cục bộ hằng ngày lúc 20:00 (tuỳ chọn; cần cấp quyền thông báo).
- Widget màn hình chính hiển thị từ vựng, nghĩa, chuỗi học và số từ đến hạn; nhấn widget để mở ứng dụng.
- Sao lưu/khôi phục JSON đầy đủ từ vựng, trạng thái SRS, lịch sử ôn và các cài đặt mục tiêu/nhắc nhở. Khôi phục thay thế dữ liệu hiện có.
- GitHub Actions chạy kiểm thử import/export và build APK debug.

## Còn thiếu so với mục tiêu tái tạo đầy đủ

- Hai mini-game Monkey Rescue và Flappy.
- Tùy chỉnh avatar/widget và lựa chọn từ hiển thị; widget hiện dùng biểu tượng sách và từ đầu tiên theo thứ tự bảng chữ cái.
- Hiệu chỉnh thuật toán SRS sau khi đối chiếu hành vi thực tế; lịch gốc chưa thể xác minh chính xác từ APK đã biên dịch.
- Kiểm thử UI, hiệu năng, thông báo, khả năng nhập hàng chục nghìn từ và offline trên Galaxy S22 Ultra thật.
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
