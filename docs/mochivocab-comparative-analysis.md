# Phân tích đối chiếu MochiVocab và kế hoạch nâng cấp Vocab

Ngày kiểm tra: 2026-10-22

## 1. Phạm vi đã kiểm tra

Ba ZIP được kiểm tra bằng `unzip -t` và giải nén không báo lỗi:

| Gói | Kết quả kiểm tra | Nội dung thực tế |
|---|---|---|
| `sources.zip` | 7.513 file | Mã Java Android đã dịch ngược, tên lớp/phương thức bị rút gọn ở nhiều nơi |
| `com.mochimochi.android.en.apk.zip` | 1.220 file | Nội dung APK, manifest, Flutter assets, hình ảnh, âm thanh, font và JSON |
| `resources.zip` | 98 file | Các APK split và thư viện native, bao gồm `libapp.so` đã biên dịch |

Đây là bản APK Flutter đã dịch ngược/tách gói, **không phải dự án Flutter gốc hoàn chỉnh**. `libapp.so` chứa mã Dart đã biên dịch; chuỗi ký tự và tên đường dẫn còn sót lại giúp nhận diện một phần kiến trúc nhưng không đủ để tái dựng chính xác toàn bộ luồng nghiệp vụ. Vì vậy, báo cáo phân biệt dấu vết quan sát được với suy luận, không tuyên bố đã đọc được mã Dart gốc.

## 2. Các nhóm tính năng có bằng chứng

Tên package/đường dẫn và chuỗi còn trong thư viện native cho thấy MochiVocab có các nhóm sau:

- **Học và ôn từ:** flow học/ôn, fast review, flashcard, câu hỏi, màn hình tổng kết, giới hạn số từ ôn, sắp lịch ôn.
- **Nhiều hình thức luyện tập:** các lớp/đường dẫn liên quan đến game viết từ, nghe, nói, luyện kỹ năng và bài tập theo bài học.
- **Tiến độ và thói quen:** streak, mục tiêu streak, nhiệm vụ hằng ngày, thanh tiến độ, tổng kết ôn tập, thông báo nhắc học, huy hiệu và bảo vệ streak.
- **Sổ từ và tra cứu:** notebook/sổ từ, tìm kiếm từ điển, thêm từ, sửa ghi chú, lọc từ và dữ liệu từ.
- **Nội dung học có cấu trúc:** bài học, câu hỏi có đáp án nhiễu và giải thích. JSON `learn_vocab_data.json` chứa các bài học theo chủ đề với đoạn văn, câu hỏi, đáp án đúng/sai và giải thích. Đây là nội dung đóng gói sẵn của ứng dụng tham khảo; Vocab sẽ không sao chép hoặc nạp nội dung này.
- **Các hệ thống ngoài phạm vi offline:** dấu vết của tài khoản/đăng nhập, API, cộng đồng, bảng xếp hạng, sự kiện, hỗ trợ, gói Premium/thanh toán và dữ liệu từ xa. Các tính năng phụ thuộc máy chủ này không phù hợp với yêu cầu Vocab không mạng.

Tài nguyên Flutter có nhóm ảnh/icon, Lottie, SVG, font và hiệu ứng âm thanh. Điều này cho thấy giao diện tham khảo chú trọng phản hồi trực quan và trạng thái học, nhưng không phải lý do để sao chép asset, âm thanh, văn bản hoặc thiết kế độc quyền.

## 3. Đối chiếu với Vocab hiện tại

### Đã có trong Vocab

- Cơ sở dữ liệu Room cục bộ, thêm/sửa/xóa/tìm kiếm và lọc sổ từ.
- Nhập CSV có xem trước; xuất CSV; sao lưu/khôi phục ZIP có kiểm tra dữ liệu.
- Flashcard, SRS 4 mức đánh giá, lịch ôn, từ mới/đến hạn/quá hạn/từ yếu.
- Trắc nghiệm chọn nghĩa, tìm từ theo nghĩa, gõ từ, điền từ, tự viết câu, collocations, word family và synonyms.
- Tiến độ phiên, độ chính xác, từ sai, thống kê theo ngày, streak, mục tiêu hằng ngày và lộ trình theo chủ đề.
- Phát âm Text-to-Speech chỉ khi thiết bị có giọng tiếng Anh offline; thông báo nhắc học cục bộ.
- Giao diện Material 3 sáng/tối; không yêu cầu quyền Internet; không tự nạp sổ từ mẫu.

### Khoảng trống có giá trị cao

1. **Nghe rồi viết chính tả:** Vocab đã có TTS offline và bài gõ từ, nhưng trước thay đổi này chưa có chế độ tách biệt để nghe phát âm trước khi nhập từ. Chế độ mới dùng TTS offline sẵn có và vẫn chấm bằng từ người dùng đã nhập.
2. **Phản hồi học tập rõ ràng hơn:** cần giữ tiến độ câu, trạng thái đúng/sai, đáp án và nút tiếp tục nhất quán giữa các chế độ.
3. **Khả năng dùng với dữ liệu ít:** phải nêu rõ khi chưa có giọng offline, chưa đủ từ hoặc thiếu trường dữ liệu; không tạo lựa chọn giả hoặc tự sinh từ vựng.
4. **Giao diện và khả năng tiếp cận:** cần tiếp tục kiểm tra kích thước vùng chạm, nội dung dài, màn hình nhỏ, chế độ tối và tương phản.
5. **Tính toàn vẹn dữ liệu:** tiếp tục kiểm thử nhập CSV, trùng từ, backup/restore, cập nhật SRS và nâng cấp database.
6. **Xác minh trên thiết bị:** CI không thay thế kiểm thử thực tế trên Samsung Galaxy S22 Ultra. TTS phụ thuộc giọng offline cài trên máy; lịch thông báo có thể bị Android trì hoãn.

## 4. Nguyên tắc thiết kế khi nâng cấp

- Giữ ứng dụng 100% offline: không thêm quyền Internet, API, tài khoản, quảng cáo, analytics hoặc đồng bộ đám mây.
- Không cài sẵn danh sách từ; dữ liệu bài học phải xuất phát từ từ người dùng tự nhập.
- Không sao chép mã nguồn, hình ảnh, âm thanh, nội dung bài học hoặc asset độc quyền của MochiVocab/Duolingo. Chỉ áp dụng các mô hình UX phổ quát bằng triển khai và tài nguyên riêng.
- Các câu hỏi trắc nghiệm phải có đáp án xác định; bài tự viết câu chỉ dùng tự đánh giá nếu chưa có bộ chấm ngôn ngữ offline đáng tin cậy.
- Mỗi thay đổi có phạm vi rõ, kiểm thử phù hợp và CI xanh trước khi nhập vào `main`.

## 5. Thay đổi trong nhánh này

- Bổ sung chế độ **Nghe & gõ** trong luyện tập: phát từ mục tiêu bằng nút TTS offline, không hiện từ trước khi trả lời, nhận đáp án bằng cùng bộ chuẩn hóa chính tả hiện có, ghi nhận kết quả vào thống kê/SRS qua callback hiện hữu và cho xem đáp án sau khi trả lời.
- Không thêm thư viện hoặc quyền mạng; nếu thiết bị không có giọng tiếng Anh offline, nút phát âm giải thích trạng thái thay vì chuyển sang dịch vụ mạng.
- Bản phân tích này là tài liệu kỹ thuật nội bộ cho dự án Vocab; không chứa mã nguồn hoặc asset trích xuất từ ứng dụng tham khảo.

## 6. Những gì cần làm tiếp trước khi gọi là hoàn thiện

- Rà soát từng luồng UI trên thiết bị thật: thêm/sửa/xóa, nhập/xuất CSV, backup/restore, flashcard/SRS, mọi chế độ quiz, thông báo, TTS và chủ đề.
- Bổ sung kiểm thử UI/instrumentation cho luồng nhập đáp án, điều hướng, xoay màn hình và trạng thái trống nếu môi trường build cho phép.
- Đánh giá thiết kế Material 3 trên màn hình nhỏ và font lớn; cải thiện phân cấp thông tin, khoảng cách, trạng thái rỗng, xác nhận thao tác phá hủy và phản hồi lỗi.
- Chỉ phát hành APK ký bằng khóa cá nhân sau khi kiểm thử thiết bị hoàn tất; APK debug từ CI chỉ phù hợp để cài thử.
