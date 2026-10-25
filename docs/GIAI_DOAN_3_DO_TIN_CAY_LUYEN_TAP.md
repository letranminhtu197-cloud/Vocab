# Giai đoạn 3 — Độ tin cậy phiên luyện tập và chất lượng bài tập

**Nhánh:** `luyentu2.0`  
**Mục tiêu:** làm cho việc chấm đáp án nhất quán giữa các dạng bài, tránh sai do định dạng văn bản và giữ tiến độ phiên học dễ hiểu.

## Phạm vi

1. Chuẩn hóa câu trả lời ở một nơi dùng chung cho các dạng gõ từ, nghe viết, điền từ và trắc nghiệm.
2. So sánh đáp án không phân biệt hoa/thường, bỏ khoảng trắng dư và bỏ dấu câu cuối câu thông dụng.
3. Kiểm tra trạng thái trả lời sau khi chọn đáp án, tránh tô nhầm lựa chọn đúng.
4. Làm rõ thông tin hoàn thành phiên học.
5. Bổ sung unit tests cho các trường hợp chuẩn hóa và so sánh đáp án.
6. Chạy CI `testDebugUnitTest assembleDebug` và kiểm tra artifact APK.

## Các thay đổi đã đưa vào mã

- Thêm `AnswerNormalizer.kt` với hai hàm thuần `normalize` và `matches`.
- Thêm `AnswerNormalizerTest.kt`: kiểm thử chữ hoa/thường, khoảng trắng lặp, dấu câu cuối câu và từ khác nhau.
- Trắc nghiệm sử dụng so sánh đã chuẩn hóa khi chấm và khi tô màu đáp án đúng. Điều này tránh trường hợp đáp án có khoảng trắng thừa bị chấm sai.
- Màn hình hoàn thành hiển thị số từ đã chấm so với tổng số từ đã chọn, thay vì diễn đạt dễ gây hiểu nhầm rằng mọi câu đều được trả lời đúng.

## Tiêu chí nghiệm thu

- [ ] CI của commit mới nhất thành công và có artifact APK.
- [ ] Các unit tests mới chạy thành công cùng bộ test hiện có.
- [ ] Kiểm thử thực tế các dạng Trắc nghiệm, Gõ từ vựng, Nghe viết, Điền từ và Thẻ từ.
- [ ] Kiểm tra trường hợp phiên ít từ hơn số lượng được chọn, không còn từ đến hạn, và kết thúc phiên thủ công.
- [ ] Kiểm tra trên thiết bị Android mục tiêu; xác nhận thao tác nhập/xuất và học không cần mạng.

**Giới hạn:** unit tests không thay thế kiểm thử giao diện trên thiết bị. Chưa tuyên bố giao diện hoặc lịch SRS tương đương hoàn toàn với bản LuyenTu gốc nếu chưa đối chiếu trực tiếp.
