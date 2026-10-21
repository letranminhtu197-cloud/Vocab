# Vocab — IELTS Vocabulary Offline

Ứng dụng Android cá nhân học từ vựng IELTS Academic, giao diện tiếng Việt, thiết kế cho dùng ngoại tuyến trên điện thoại Android. Ứng dụng không tự cài sẵn danh sách từ vựng; người dùng tự nhập từ, nhập CSV, hoặc chủ động thêm ba từ mẫu chỉ để thử tính năng.

## Đã triển khai
- Kotlin, Jetpack Compose, Material 3.
- Room database cục bộ: từ vựng và tiến độ vẫn còn sau khi đóng/mở ứng dụng.
- Thêm, sửa, xóa, tìm kiếm từ; flashcard; thống kê cơ bản.
- Ôn SRS 5 cấp, đánh giá Lại/Khó/Tốt/Dễ; lưu cấp độ, lịch ôn, số lượt ôn và số lần quên.
- Quiz trắc nghiệm nghĩa tiếng Việt, chấm đáp án tức thì, giải thích kết quả và ví dụ nếu có.
- Chế độ ôn riêng các từ mới/đến hạn, tách khỏi chế độ xem toàn bộ sổ từ.
- Sao lưu/khôi phục ZIP cục bộ có phiên bản định dạng, bảo toàn nội dung và tiến độ SRS; khi khôi phục, chỉ thêm từ chưa có và bỏ qua từ trùng.
- CSV và ZIP giữ thời điểm tạo từ, lần ôn gần nhất cùng metadata SRS.
- Nhắc ôn hằng ngày khoảng 19:00 bằng thông báo cục bộ; có công tắc bật/tắt, xin quyền thông báo theo Android và tự đăng ký lại sau khi khởi động máy. Android có thể dời thời điểm một chút để tiết kiệm pin.
- Nhập/xuất CSV qua Android file picker (Storage Access Framework); không cần quyền truy cập toàn bộ bộ nhớ.
- Nhập CSV yêu cầu cột `term` (hoặc `word`) và `meaning` (hoặc `nghĩa`). Các cột `ipa`, `partOfSpeech`, `example`, `topic` tùy chọn. Có thể thêm các cột SRS trong mẫu để giữ tiến độ khi xuất/nhập lại.
- Từ trùng được bỏ qua; dòng thiếu từ hoặc nghĩa được báo số lượng. CSV có hỗ trợ trường đặt trong dấu ngoặc kép, dấu phẩy và xuống dòng bên trong trường.
- Không khai báo quyền `INTERNET`; không gọi dịch vụ mạng trong ứng dụng. Quyền thông báo và nhận sự kiện khởi động chỉ phục vụ nhắc ôn cục bộ.

## File CSV mẫu
Tải mẫu rỗng tại [examples/vocab-template.csv](examples/vocab-template.csv). Ví dụ tối thiểu:

```csv
term,meaning
allocate,phân bổ
sustainable,bền vững
```

Có thể chỉ dùng hai cột `term,meaning`; các trường còn lại để trống hoặc bỏ qua. Nên lưu file dưới dạng CSV UTF-8.

## Build và kiểm tra
Yêu cầu JDK 17, Android SDK Platform 35 và Gradle 8.9:

```bash
gradle test
gradle assembleDebug
```

GitHub Actions chạy unit tests, build debug APK, kiểm tra APK không có quyền Internet và tải artifact `vocab-debug-apk`. Xem tab [Actions](https://github.com/letranminhtu197-cloud/Vocab/actions) để xác nhận trạng thái build thực tế.

## Quyền riêng tư
Dữ liệu từ vựng lưu trong cơ sở dữ liệu cục bộ của ứng dụng. Android Auto Backup bị tắt; nên dùng Sao lưu ZIP hoặc Xuất CSV định kỳ nếu muốn giữ bản sao. Không cấp quyền mạng vì ứng dụng không cần Internet để học.

## Những phần vẫn cần hoàn thiện so với master prompt
Đây vẫn là bản phát triển, chưa nên coi là hoàn tất toàn bộ yêu cầu ban đầu. Các hạng mục còn lại gồm nâng cấp dạng bài luyện (điền từ theo ngữ cảnh, ôn từ sai/yếu), thống kê theo thời gian, phát âm/audio offline bằng tệp âm thanh thật do người dùng cung cấp hoặc được đóng gói hợp pháp, hoàn thiện bài học ngắn theo phiên và kiểm thử giao diện trên emulator/thiết bị Samsung thật. Không có bộ 250–300 từ được tự nạp; nội dung học là do người dùng quản lý.
