# Vocab 1.0.2 — IELTS Vocabulary Offline

**Phiên bản ứng dụng:** `1.0.2` (`versionCode 102`). Đây là bản phát hành cá nhân ưu tiên ngoại tuyến; APK CI hiện là **debug-signed** để cài thử trực tiếp, chưa phải APK phát hành thương mại ký bằng khóa riêng.

Ứng dụng Android cá nhân học từ vựng IELTS Academic, giao diện tiếng Việt, thiết kế cho dùng ngoại tuyến trên điện thoại Android. Ứng dụng không tự cài sẵn danh sách từ vựng; người dùng tự nhập từ, nhập CSV, hoặc chủ động thêm ba từ mẫu chỉ để thử tính năng.

## Đã triển khai
- Kotlin, Jetpack Compose, Material 3.
- Room database cục bộ: từ vựng và tiến độ vẫn còn sau khi đóng/mở ứng dụng.
- Thêm, sửa, xóa, tìm kiếm từ; flashcard; thống kê cơ bản. Mỗi từ hỗ trợ IPA, từ loại, nghĩa, ví dụ, chủ đề IELTS, collocations, word family, ghi chú cá nhân và tags. Khi thêm/sửa sẽ chặn từ trùng không phân biệt chữ hoa/thường; thao tác xóa cần xác nhận và tìm kiếm có trạng thái rỗng rõ ràng.
- Ôn SRS 5 cấp, đánh giá Lại/Khó/Tốt/Dễ; lưu cấp độ, lịch ôn, số lượt ôn và số lần quên. Khi trả lời sai, từ quay về giai đoạn học lại và được hẹn ôn sau 10 phút; lần trả lời đúng tiếp theo bắt đầu lại lịch 1 ngày, sau đó tiến dần theo mốc 3/7/14/30 ngày.
- Kết quả của các dạng quiz cũng cập nhật SRS cho đúng từ đang luyện, không chỉ tăng thống kê tổng.
- Hiển thị trạng thái SRS: từ mới, sắp đến hạn, thời điểm vàng hoặc quá hạn.
- Lưu cục bộ tổng số câu quiz, tỷ lệ đúng và điểm/số câu của phiên quiz hiện tại qua các lần mở ứng dụng; có thể đặt lại bộ đếm phiên mà không xóa thống kê tổng.
- Thống kê hoạt động học theo từng ngày trong 7 ngày gần nhất và chuỗi ngày học liên tiếp; ghi nhận lượt trả lời quiz và lượt ôn SRS, hoàn toàn cục bộ.
- Các dạng luyện: chọn nghĩa, chọn từ theo nghĩa, tự gõ từ tiếng Anh từ nghĩa tiếng Việt, điền từ còn thiếu trong câu ví dụ, viết câu với từ mục tiêu và ôn từ yếu.
- Luyện tập có thể giới hạn theo chủ đề IELTS tự nhập; không tự tạo hoặc tải bộ từ vựng.
- Chấm đáp án tức thì với các dạng có đáp án xác định; dạng viết câu yêu cầu người học tự đánh giá và lưu kết quả tự đánh giá vào SRS. Đây không phải hệ thống chấm ngữ pháp tự động.
- Chế độ ôn riêng các từ mới/đến hạn, tách khỏi chế độ xem toàn bộ sổ từ; tab Ôn tập mở thẳng hàng đợi từ đến hạn, ưu tiên từ mới và từ quá hạn trước.
- Thống kê có tiến độ theo chủ đề và danh sách tối đa 5 từ cần ưu tiên dựa trên số lần quên.
- Lộ trình học theo chủ đề do người dùng nhập: tổng quan tiến độ, phần trăm từ đã ôn, số từ đến hạn và từ yếu theo từng chủ đề; mỗi chặng mở bài học giới hạn đúng chủ đề đó. Từ không có chủ đề được gom riêng.
- Bài học ngắn tối đa 5 từ, ưu tiên từ chưa ôn; người học tự đánh giá nhớ/chưa nhớ để cập nhật tiến độ SRS.
- Sao lưu/khôi phục ZIP cục bộ có phiên bản định dạng, bảo toàn nội dung và tiến độ SRS; khi khôi phục, chỉ thêm từ chưa có và bỏ qua từ trùng. File khôi phục bị từ chối nếu thiếu mục bắt buộc, lặp mục dữ liệu, sai phiên bản hoặc chứa UTF-8 lỗi; giới hạn kích thước nén và giải nén đều được áp dụng.
- CSV và ZIP giữ thời điểm tạo từ, lần ôn gần nhất, metadata SRS, collocations, word family, ghi chú và tags. Cơ sở dữ liệu được nâng từ schema 1 lên schema 2 mà không xóa từ đã lưu.
- Nhắc ôn hằng ngày khoảng 19:00 bằng thông báo cục bộ; có công tắc bật/tắt, xin quyền thông báo theo Android và tự đăng ký lại sau khi khởi động máy. Android có thể dời thời điểm một chút để tiết kiệm pin.
- Phát âm từ qua Text-to-Speech chỉ khi thiết bị có giọng tiếng Anh được đánh dấu là không cần mạng; nếu không có, ứng dụng hướng dẫn cài giọng offline và không gọi giọng mạng.
- Nhập/xuất CSV qua Android file picker (Storage Access Framework); không cần quyền truy cập toàn bộ bộ nhớ.
- Nhập CSV yêu cầu cột `term` (hoặc `word`) và `meaning` (hoặc `nghĩa`). Các cột `ipa`, `partOfSpeech`, `example`, `topic`, `collocations`, `wordFamily`, `notes`, `tags` tùy chọn. Có thể thêm các cột SRS trong mẫu để giữ tiến độ khi xuất/nhập lại.
- Từ trùng được bỏ qua; dòng thiếu từ hoặc nghĩa được báo số lượng. CSV hỗ trợ trường đặt trong dấu ngoặc kép, dấu phẩy và xuống dòng bên trong trường; các dấu ngoặc kép đặt sai vị trí hoặc ký tự thừa sau dấu ngoặc kép đóng sẽ bị từ chối với thông báo lỗi. Metadata SRS có giá trị số không hợp lệ, cấp SRS ngoài 0–5 hoặc thời điểm âm sẽ khiến toàn bộ lượt nhập bị từ chối thay vì âm thầm đặt tiến độ về 0.
- Không khai báo quyền `INTERNET`; không gọi dịch vụ mạng trong ứng dụng. Quyền thông báo và nhận sự kiện khởi động chỉ phục vụ nhắc ôn cục bộ.

## File CSV mẫu
Tải mẫu rỗng tại [examples/vocab-template.csv](examples/vocab-template.csv). Ví dụ tối thiểu:

```csv
term,meaning
allocate,phân bổ
sustainable,bền vững
```

Có thể chỉ dùng hai cột `term,meaning`; các trường còn lại để trống hoặc bỏ qua. Nên lưu file dưới dạng CSV UTF-8. File có byte UTF-8 lỗi sẽ bị từ chối thay vì âm thầm thay ký tự.

## Build và kiểm tra
Yêu cầu JDK 17, Android SDK Platform 35 và Gradle 8.9:

```bash
gradle test
gradle assembleDebug
```

GitHub Actions chạy unit tests, build debug APK, kiểm tra APK không có quyền Internet và tạo artifact theo phiên bản khai báo trong `app/build.gradle.kts`. Ví dụ với `versionName = "1.0.2"`, artifact là `vocab-1.0.2-debug-apk` và file bên trong là `Vocab-1.0.2-debug.apk`; khi nâng phiên bản, tên tự đổi tương ứng. Mở [Actions](https://github.com/letranminhtu197-cloud/Vocab/actions), chọn lượt build thành công gần nhất và tải artifact ở cuối trang. Artifact hết hạn theo chính sách lưu trữ GitHub Actions.

## Quyền riêng tư
Dữ liệu từ vựng lưu trong cơ sở dữ liệu cục bộ của ứng dụng. Android Auto Backup bị tắt; nên dùng Sao lưu ZIP hoặc Xuất CSV định kỳ nếu muốn giữ bản sao. Không cấp quyền mạng vì ứng dụng không cần Internet để học.

## Phạm vi và lưu ý của bản 1.0.2
- Không tự nạp bộ từ vựng; bạn tự nhập từ hoặc dùng file CSV của mình.
- Phát âm dùng giọng Text-to-Speech offline có sẵn trên thiết bị; ứng dụng không đóng gói bộ âm thanh riêng. Nếu máy không có giọng tiếng Anh offline, chức năng phát âm có thể không hoạt động cho đến khi cài giọng ngoại tuyến trong cài đặt Android.
- Nhắc hằng ngày dùng cơ chế lịch của Android; thời điểm thực tế có thể trễ do chế độ tiết kiệm pin hoặc giới hạn nền của hệ điều hành.
- CI kiểm tra unit tests, tạo APK debug và xác nhận APK không yêu cầu quyền Internet. Chưa thể khẳng định mọi luồng đã được kiểm thử thủ công trên Samsung Galaxy S22 Ultra thật; trước khi coi là hoàn tất tuyệt đối, cần cài APK trên máy và kiểm tra nhập/xuất CSV, sao lưu/khôi phục ZIP, thông báo, TTS offline và toàn bộ màn hình.
