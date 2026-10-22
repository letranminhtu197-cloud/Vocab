# Vocab 1.1.3 — IELTS Vocabulary Offline

**Phiên bản ứng dụng:** `1.1.3` (`versionCode 113`). Đây là bản phát hành cá nhân ưu tiên ngoại tuyến; APK CI hiện là **debug-signed** để cài thử trực tiếp, chưa phải APK phát hành thương mại ký bằng khóa riêng.

Ứng dụng Android cá nhân học từ vựng IELTS Academic, giao diện tiếng Việt, thiết kế cho dùng ngoại tuyến trên điện thoại Android. Ứng dụng không tự cài sẵn danh sách từ vựng; người dùng tự nhập từ, nhập CSV, hoặc chủ động thêm ba từ mẫu chỉ để thử tính năng.

## Đã triển khai
- Kotlin, Jetpack Compose, Material 3.
- Room database cục bộ: từ vựng và tiến độ vẫn còn sau khi đóng/mở ứng dụng.
- Thêm, sửa, xóa, tìm kiếm từ; flashcard; thống kê cơ bản. Mỗi từ hỗ trợ IPA, từ loại, nghĩa, ví dụ, chủ đề IELTS, collocations, từ đồng nghĩa, word family, ghi chú cá nhân và tags. Khi thêm/sửa sẽ chặn từ trùng không phân biệt chữ hoa/thường; thao tác xóa cần xác nhận và tìm kiếm có trạng thái rỗng rõ ràng.
- Ôn SRS 5 cấp, đánh giá Lại/Khó/Tốt/Dễ; lịch cơ sở lần lượt 4 giờ, 24 giờ, 72 giờ, 168 giờ và 720 giờ. “Khó” lên lịch bằng một nửa khoảng của cấp mục tiêu (tối thiểu 1 giờ); “Lại” đặt ôn lại sau 10 phút và ghi nhận lần quên. Tiến độ/lịch đã lưu được giữ nguyên; lịch mới được áp dụng khi đánh giá từ tiếp theo.
- Kết quả của các dạng quiz cũng cập nhật SRS cho đúng từ đang luyện, không chỉ tăng thống kê tổng.
- Hiển thị trạng thái SRS: từ mới, sắp đến hạn, thời điểm vàng hoặc quá hạn.
- Lưu cục bộ tổng số câu quiz, tỷ lệ đúng và điểm/số câu của phiên quiz hiện tại qua các lần mở ứng dụng; có thể đặt lại bộ đếm phiên mà không xóa thống kê tổng.
- Thống kê hoạt động học theo từng ngày trong 7 ngày gần nhất và chuỗi ngày học liên tiếp; ghi nhận lượt trả lời quiz và lượt ôn SRS, hoàn toàn cục bộ.
- Thành tựu cục bộ được tính từ sổ từ, lượt ôn, chuỗi ngày và mục tiêu ngày; có tiến độ/mốc mở khóa mà không cần tài khoản, mạng hoặc bảng dữ liệu thành tựu riêng.
- Trang chủ có tổng quan số từ, từ đến hạn, số từ đã ôn và chuỗi ngày học; mục tiêu luyện tập hằng ngày tùy chỉnh 5/10/15/20/25/30 lượt được lưu cục bộ, có thanh tiến độ và lối tắt đến Sổ từ, Lộ trình, Thống kê.
- Các dạng luyện: chọn nghĩa, chọn từ theo nghĩa, tự gõ từ tiếng Anh từ nghĩa tiếng Việt, **nghe phát âm offline rồi gõ từ**, **nói từ bằng nhận diện giọng nói ngoại tuyến trên thiết bị**, điền từ còn thiếu trong câu ví dụ, viết câu với từ mục tiêu, nhận diện nhóm collocations/từ đồng nghĩa/họ từ đã nhập và ôn từ yếu.
- Luyện tập có thể giới hạn theo chủ đề IELTS tự nhập; không tự tạo hoặc tải bộ từ vựng.
- Chấm đáp án tức thì với các dạng có đáp án xác định; dạng viết câu yêu cầu người học tự đánh giá và lưu kết quả tự đánh giá vào SRS. Đây không phải hệ thống chấm ngữ pháp tự động.
- Quiz trắc nghiệm loại các lựa chọn có nội dung hiển thị trùng nhau, bỏ lựa chọn nhiễu rỗng và luôn giữ đáp án mục tiêu; nếu dữ liệu người dùng chưa đủ hai lựa chọn khác nhau, ứng dụng giải thích cách bổ sung dữ liệu thay vì tạo câu hỏi vô nghĩa.
- Các chế độ trắc nghiệm hiển thị vị trí câu hiện tại và thanh tiến độ trong phạm vi từ đang luyện.
- Có chế độ liên kết từ với nhóm collocations và word family đã lưu trong từng mục. Đây là bài nhận diện/ghi nhớ dữ liệu do người dùng nhập, không tự suy diễn hoặc tạo họ từ nếu chưa có dữ liệu.
- Chế độ ôn riêng các từ mới/đến hạn, tách khỏi chế độ xem toàn bộ sổ từ; tab Ôn tập mở thẳng hàng đợi từ đến hạn, ưu tiên từ mới và từ quá hạn trước.
- Thống kê có tiến độ theo chủ đề và danh sách tối đa 5 từ cần ưu tiên dựa trên số lần quên.
- Khi sổ từ còn trống, màn hình flashcard cung cấp lối tắt nhập CSV trực tiếp, bên cạnh mở sổ từ và thêm từ mẫu tùy chọn.
- Lộ trình học theo chủ đề do người dùng nhập: tổng quan tiến độ, phần trăm từ đã ôn, số từ đến hạn và từ yếu theo từng chủ đề; mỗi chặng mở bài học giới hạn đúng chủ đề đó. Từ không có chủ đề được gom riêng.
- Tùy chọn mục tiêu 5/10/15/20 từ mỗi phiên học; lựa chọn được lưu cục bộ và áp dụng cho phiên học kế tiếp. Nếu chủ đề có ít từ hơn mục tiêu, phiên chỉ dùng số từ hiện có.
- Phiên bài học có thể đặt mục tiêu 5/10/15/20 từ, ưu tiên từ chưa ôn; người học tự đánh giá nhớ/chưa nhớ để cập nhật tiến độ SRS.
- Sao lưu/khôi phục ZIP cục bộ có phiên bản định dạng, bảo toàn nội dung và tiến độ SRS; khi khôi phục, có màn hình xem trước và chỉ thêm từ chưa có, không ghi đè từ đang có. File khôi phục bị từ chối nếu thiếu mục bắt buộc, lặp mục dữ liệu, sai phiên bản hoặc chứa UTF-8 lỗi; giới hạn kích thước nén và giải nén đều được áp dụng.
- CSV và ZIP giữ thời điểm tạo từ, lần ôn gần nhất, metadata SRS, collocations, từ đồng nghĩa, word family, ghi chú và tags. Cơ sở dữ liệu có migration tuần tự từ schema 1 qua schema 2 lên schema 3, giữ nguyên dữ liệu từ đã lưu.
- Nhắc ôn hằng ngày khoảng 19:00 bằng thông báo cục bộ; có công tắc bật/tắt, xin quyền thông báo theo Android và tự đăng ký lại sau khi khởi động máy. Android có thể dời thời điểm một chút để tiết kiệm pin.
- Phát âm từ qua Text-to-Speech chỉ khi thiết bị có giọng tiếng Anh được đánh dấu là không cần mạng; nếu không có, ứng dụng hướng dẫn cài giọng offline và không gọi giọng mạng.
- Chế độ Nói từ khai báo khả năng nhìn thấy Android `RecognitionService` trên Android 11+ và chỉ dùng `SpeechRecognizer.createOnDeviceSpeechRecognizer` khi Android báo có hỗ trợ. Quyền microphone chỉ được hỏi khi người dùng chủ động bật chế độ này; nếu thiết bị thiếu bộ nhận diện/ gói ngôn ngữ offline, app báo rõ và không chuyển sang nhận diện mạng. Kết quả nhận diện không phải điểm chấm phát âm hay trọng âm.
- Nhập/xuất CSV qua Android file picker (Storage Access Framework); không cần quyền truy cập toàn bộ bộ nhớ. CSV giới hạn 20 MB, kiểm tra UTF-8 nghiêm ngặt và có màn hình xem trước số từ mới/trùng/dòng lỗi trước khi xác nhận nhập.
- Nhập CSV yêu cầu cột `term` (hoặc `word`) và `meaning` (hoặc `nghĩa`). Các cột `ipa`/`phonetic`, `partOfSpeech`/`pos`, `example`, `topic`, `collocations`/`collocation`, `synonyms`/`synonym`, `wordFamily`, `notes`, `tags` tùy chọn. Có thể thêm các cột SRS trong mẫu để giữ tiến độ khi xuất/nhập lại.
- Từ trùng được bỏ qua; dòng thiếu từ hoặc nghĩa được báo số lượng. CSV hỗ trợ trường đặt trong dấu ngoặc kép, dấu phẩy và xuống dòng bên trong trường; các dấu ngoặc kép đặt sai vị trí hoặc ký tự thừa sau dấu ngoặc kép đóng sẽ bị từ chối với thông báo lỗi. Metadata SRS có giá trị số không hợp lệ, cấp SRS ngoài 0–5 hoặc thời điểm âm sẽ khiến toàn bộ lượt nhập bị từ chối thay vì âm thầm đặt tiến độ về 0.
- Không khai báo quyền `INTERNET`; không gọi dịch vụ mạng trong ứng dụng. Quyền thông báo, nhận sự kiện khởi động và microphone (chỉ xin khi chủ động dùng chế độ Nói từ) phục vụ các chức năng cục bộ.

## File CSV mẫu
Tải mẫu rỗng tại [examples/vocab-template.csv](examples/vocab-template.csv). Ví dụ tối thiểu:

```csv
term,meaning
allocate,phân bổ
sustainable,bền vững
```

Có thể chỉ dùng hai cột `term,meaning`; các trường còn lại để trống hoặc bỏ qua. Nên lưu file dưới dạng CSV UTF-8. File có byte UTF-8 lỗi sẽ bị từ chối thay vì âm thầm thay ký tự.

## Cải tiến học tập và trải nghiệm ở bản 1.1.3
- Bài học dùng hàng đợi hỗn hợp có ưu tiên: từ mới và đến hạn được đưa lên trước; các từ có lịch sử quên lặp lại được tăng ưu tiên, thay vì chỉ chọn từ mới rồi xáo trộn ngẫu nhiên.
- Mỗi thẻ trong bài học hiển thị lý do ưu tiên (từ mới, cần củng cố, đến hạn ôn hoặc ôn duy trì), giúp người học hiểu vì sao từ xuất hiện.
- Tất cả dạng quiz đều có chỉ báo tiến độ câu hỏi, kể cả dạng gõ từ, điền từ và viết câu.
- Màu đáp án đúng/sai dùng các màu theo bảng màu Material 3 của giao diện sáng/tối để cải thiện độ tương phản.
- Bổ sung unit tests cho thứ tự ưu tiên của hàng đợi học tập, trường hợp từ bị quên nhiều lần và thứ tự phân xử ổn định.

## Build và kiểm tra
Yêu cầu JDK 17, Android SDK Platform 35 và Gradle 8.9:

```bash
gradle test
gradle assembleDebug
```

GitHub Actions chạy unit tests, build debug APK, kiểm tra APK không có quyền Internet và tạo artifact theo phiên bản khai báo trong `app/build.gradle.kts`. Ví dụ với `versionName = "1.1.3"`, artifact là `vocab-1.1.3-debug-apk` và file bên trong là `Vocab-1.1.3-debug.apk`; khi nâng phiên bản, tên tự đổi tương ứng. Mở [Actions](https://github.com/letranminhtu197-cloud/Vocab/actions), chọn lượt build thành công gần nhất và tải artifact ở cuối trang. Artifact hết hạn theo chính sách lưu trữ GitHub Actions.

## Quyền riêng tư
Dữ liệu từ vựng lưu trong cơ sở dữ liệu cục bộ của ứng dụng. Android Auto Backup bị tắt; nên dùng Sao lưu ZIP hoặc Xuất CSV định kỳ nếu muốn giữ bản sao. Không cấp quyền mạng vì ứng dụng không cần Internet để học.

## Phạm vi và lưu ý của bản 1.1.3
- Không tự nạp bộ từ vựng; bạn tự nhập từ hoặc dùng file CSV của mình.
- Phát âm dùng giọng Text-to-Speech offline có sẵn trên thiết bị; ứng dụng không đóng gói bộ âm thanh riêng. Nếu máy không có giọng tiếng Anh offline, chức năng phát âm có thể không hoạt động cho đến khi cài giọng ngoại tuyến trong cài đặt Android.
- Nhắc hằng ngày dùng cơ chế lịch của Android; thời điểm thực tế có thể trễ do chế độ tiết kiệm pin hoặc giới hạn nền của hệ điều hành.
- CI kiểm tra unit tests, tạo APK debug và xác nhận APK không yêu cầu quyền Internet. Chưa thể khẳng định mọi luồng đã được kiểm thử thủ công trên Samsung Galaxy S22 Ultra thật; trước khi coi là hoàn tất tuyệt đối, cần cài APK trên máy và kiểm tra nhập/xuất CSV, sao lưu/khôi phục ZIP, thông báo, TTS offline và toàn bộ màn hình.
