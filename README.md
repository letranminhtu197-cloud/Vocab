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
- Sáu chế độ người dùng: Flashcard, Trắc nghiệm, Nối từ, Gõ từ vựng, Nghe viết và Đặc biệt; chế độ Đặc biệt chọn ngẫu nhiên biến thể bài tập, gồm cả điền từ.
- Phiên ôn có lựa chọn 10/20/30/50 từ và danh sách cố định khi bắt đầu; tiến độ cập nhật sau từng kết quả.
- SRS được tách thành lịch lập thuần có thể kiểm thử: Again/Hard/Good/Easy, khoảng ôn, hệ số dễ, số lần quên, bước học lại; sửa tính chuỗi học để tính cả ngày hôm nay khi đã ôn.
- Thống kê tổng từ, lượt ôn, từ đến hạn, từ đã vững, cấp độ Lv0–Lv5, biểu đồ hoạt động 7 ngày, bộ lọc từ và lịch sử ôn.
- Chuỗi ngày học, lộ trình cột mốc offline và mục tiêu ôn tập hằng ngày có thể chọn.
- Nhắc ôn cục bộ hằng ngày lúc 20:00 (tuỳ chọn; cần cấp quyền thông báo).
- Widget màn hình chính hiển thị từ vựng, nghĩa, chuỗi học và số từ đến hạn; nhấn widget để mở ứng dụng.
- Sao lưu/khôi phục JSON đầy đủ từ vựng, trạng thái SRS, lịch sử ôn và các cài đặt mục tiêu/nhắc nhở. Khôi phục thay thế dữ liệu hiện có.
- GitHub Actions chạy `testDebugUnitTest` và `assembleDebug`; lần chạy cho commit `1f7ab81994c12f3faf9b689e55942400d23c4950` đã hoàn tất job `build` với kết quả `success` và tạo artifact `luyentu-2.0-debug-apk`.

## Còn thiếu so với mục tiêu tái tạo đầy đủ

- Hai mini-game Monkey Rescue và Flappy.
- Tùy chỉnh avatar/widget và lựa chọn từ hiển thị; widget hiện dùng biểu tượng sách và từ đầu tiên theo thứ tự bảng chữ cái.
- Hiệu chỉnh thuật toán SRS sau khi đối chiếu hành vi thực tế; lịch gốc chưa thể xác minh chính xác từ APK đã biên dịch.
- Chưa có kiểm thử UI tự động/instrumentation và chưa chạy kiểm thử thủ công trên Galaxy S22 Ultra thật; cần xác nhận thao tác nhập/xuất, khôi phục backup, thông báo, widget, TTS offline và chế độ máy bay.
- Chưa đo hiệu năng với hàng chục nghìn từ; không tuyên bố đã xác nhận quy mô này.
- Xác nhận CI thành công cho phiên bản mới nhất và kiểm tra APK đầu ra.

Các tính năng online trong ứng dụng gốc như tài khoản, chat, leaderboard dùng máy chủ, mua hàng/subscription và đồng bộ đám mây được loại bỏ theo yêu cầu miễn phí/offline một người dùng; không thay bằng dịch vụ từ xa.

## Build

Yêu cầu JDK 17, Android SDK Platform 35 và Gradle 8.9+.

```bash
gradle testDebugUnitTest assembleDebug
```

APK debug được tạo bởi GitHub Actions. Với commit `1f7ab81994c12f3faf9b689e55942400d23c4950`, job `build` đã thành công và artifact `luyentu-2.0-debug-apk` có sẵn tại [trang chạy workflow #479](https://github.com/letranminhtu197-cloud/Vocab/actions/runs/40115422618). Mở trang này, kéo xuống mục **Artifacts** và tải ZIP; giải nén để lấy `app-debug.apk`. Đây là APK debug để thử nghiệm, chưa phải bản phát hành đã kiểm thử trên thiết bị thật.

## Quyền riêng tư

Ứng dụng không khai báo quyền `INTERNET`. Dữ liệu từ vựng, tiến độ và lịch sử ôn được lưu cục bộ. Khi tự chọn tệp từ một nhà cung cấp lưu trữ đám mây trong bộ chọn Android, chính nhà cung cấp đó có thể cần mạng; đây không phải yêu cầu của ứng dụng học.


## Checklist kiểm thử trước khi sử dụng hằng ngày

- [x] CI: unit tests và build APK debug hoàn tất thành công ở workflow #479.
- [x] Manifest nguồn không khai báo quyền `INTERNET`.
- [ ] Cài APK trên thiết bị thật và xác nhận mở ứng dụng/điều hướng không crash.
- [ ] Nhập thử CSV, TSV và TXT; xác nhận từ trùng không ghi đè ngoài ý muốn; xuất rồi nhập lại.
- [ ] Kiểm tra đủ các chế độ luyện, lưu kết quả, đóng/mở lại app và kiểm tra tiến độ SRS.
- [ ] Tạo rồi khôi phục JSON backup; xác nhận dữ liệu và cài đặt.
- [ ] Bật/tắt nhắc nhở, kiểm tra quyền thông báo và widget.
- [ ] Bật chế độ máy bay, kiểm tra các luồng chính; kiểm tra TTS với dữ liệu giọng đọc offline đã cài.

Các mục chưa đánh dấu cần được kiểm tra thực tế trước khi coi bản này là hoàn thiện cuối cùng.
