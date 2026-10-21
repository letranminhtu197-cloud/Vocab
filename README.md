# Vocab — IELTS Vocabulary Offline

Ứng dụng Android cá nhân học từ vựng IELTS Academic, giao diện tiếng Việt, thiết kế cho dùng ngoại tuyến trên điện thoại Android. Ứng dụng không tự cài sẵn danh sách từ vựng; người dùng tự nhập từ, nhập CSV, hoặc chủ động thêm ba từ mẫu chỉ để thử tính năng.

## Đã triển khai
- Kotlin, Jetpack Compose, Material 3.
- Room database cục bộ: từ vựng và tiến độ vẫn còn sau khi đóng/mở ứng dụng.
- Thêm, sửa, xóa, tìm kiếm từ; flashcard; thống kê cơ bản. Khi thêm/sửa sẽ chặn từ trùng không phân biệt chữ hoa/thường; thao tác xóa cần xác nhận và tìm kiếm có trạng thái rỗng rõ ràng.
- Ôn SRS 5 cấp, đánh giá Lại/Khó/Tốt/Dễ; lưu cấp độ, lịch ôn, số lượt ôn và số lần quên.
- Hiển thị trạng thái SRS: từ mới, sắp đến hạn, thời điểm vàng hoặc quá hạn.
- Lưu cục bộ tổng số câu quiz và tỷ lệ đúng qua các lần mở ứng dụng.
- Thống kê hoạt động học theo từng ngày trong 7 ngày gần nhất và chuỗi ngày học liên tiếp; ghi nhận lượt trả lời quiz và lượt ôn SRS, hoàn toàn cục bộ.
- Quiz trắc nghiệm nghĩa tiếng Việt, chấm đáp án tức thì, giải thích kết quả và ví dụ nếu có.
- Chế độ ôn riêng các từ mới/đến hạn, tách khỏi chế độ xem toàn bộ sổ từ.
- Bài học ngắn tối đa 5 từ, ưu tiên từ chưa ôn; người học tự đánh giá nhớ/chưa nhớ để cập nhật tiến độ SRS.
- Sao lưu/khôi phục ZIP cục bộ có phiên bản định dạng, bảo toàn nội dung và tiến độ SRS; khi khôi phục, chỉ thêm từ chưa có và bỏ qua từ trùng. File khôi phục bị từ chối nếu thiếu mục bắt buộc, lặp mục dữ liệu, sai phiên bản hoặc chứa UTF-8 lỗi; giới hạn kích thước nén và giải nén đều được áp dụng.
- CSV và ZIP giữ thời điểm tạo từ, lần ôn gần nhất cùng metadata SRS.
- Nhắc ôn hằng ngày khoảng 19:00 bằng thông báo cục bộ; có công tắc bật/tắt, xin quyền thông báo theo Android và tự đăng ký lại sau khi khởi động máy. Android có thể dời thời điểm một chút để tiết kiệm pin.
- Phát âm từ qua Text-to-Speech chỉ khi thiết bị có giọng tiếng Anh được đánh dấu là không cần mạng; nếu không có, ứng dụng hướng dẫn cài giọng offline và không gọi giọng mạng.
- Nhập/xuất CSV qua Android file picker (Storage Access Framework); không cần quyền truy cập toàn bộ bộ nhớ.
- Nhập CSV yêu cầu cột `term` (hoặc `word`) và `meaning` (hoặc `nghĩa`). Các cột `ipa`, `partOfSpeech`, `example`, `topic` tùy chọn. Có thể thêm các cột SRS trong mẫu để giữ tiến độ khi xuất/nhập lại.
- Từ trùng được bỏ qua; dòng thiếu từ hoặc nghĩa được báo số lượng. CSV hỗ trợ trường đặt trong dấu ngoặc kép, dấu phẩy và xuống dòng bên trong trường; các dấu ngoặc kép đặt sai vị trí hoặc ký tự thừa sau dấu ngoặc kép đóng sẽ bị từ chối với thông báo lỗi. Metadata SRS có giá trị số không hợp lệ, cấp SRS ngoài 0–5 hoặc thời điểm âm sẽ khiến toàn bộ lượt nhập bị từ chối thay vì âm thầm đặt tiến độ về 0.
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
Đây vẫn là bản phát triển, chưa nên coi là hoàn tất toàn bộ yêu cầu ban đầu. Thống kê theo ngày/tuần và chuỗi ngày học đã được gộp sau khi CI thành công. Các hạng mục còn lại gồm tiếp tục cải tiến giao diện, kiểm thử các trường hợp biên và kiểm thử toàn bộ luồng trên emulator/thiết bị Samsung Galaxy S22 Ultra thật. Phát âm hiện dựa trên giọng Text-to-Speech offline đã cài trên Android; chưa đóng gói bộ tệp âm thanh riêng. Không có bộ 250–300 từ được tự nạp; nội dung học là do người dùng quản lý.
