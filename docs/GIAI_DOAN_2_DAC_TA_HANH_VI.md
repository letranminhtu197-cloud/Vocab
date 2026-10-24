# Giai đoạn 2 — Đặc tả hành vi và ưu tiên hoàn thiện

**Nhánh:** `luyentu2.0`  
**Mục tiêu:** xác định hành vi cần đúng trước khi mở rộng giao diện; ưu tiên tính đúng của phiên ôn, SRS và nhập dữ liệu.  
**Phạm vi:** tái tạo hành vi có thể quan sát từ LuyenTu, không sao chép nguyên mã nguồn đã không có sẵn.

## 1. Các nguyên tắc sản phẩm

1. Ứng dụng cá nhân, miễn phí, không tài khoản, quảng cáo, backend hay đồng bộ đám mây.
2. Dữ liệu từ vựng và lịch sử ôn nằm cục bộ; không yêu cầu bộ từ dựng sẵn.
3. Nhập hàng loạt phải đơn giản, hỗ trợ CSV/TXT/TSV, báo số từ thêm mới và số từ bị bỏ qua.
4. Phiên luyện tập phải có danh sách từ ổn định lúc bắt đầu, phản hồi rõ ràng và không tính lặp một từ đã chấm.
5. Không tuyên bố “giống hệt” khi chưa có đối chiếu đầy đủ trên thiết bị và với bản tham chiếu.

## 2. Hợp đồng hành vi cho phiên luyện tập

- Nguồn: các từ đến hạn trong bộ đang chọn tại thời điểm bắt đầu.
- Số lượng: chọn 10/20/30/50; số từ thực tế bằng `min(số lượng chọn, số từ đến hạn)`.
- Thứ tự: xáo trộn khi bắt đầu để tránh luôn gặp từ theo thứ tự bảng chữ cái.
- Kết quả: Again/Hard/Good/Easy hoặc chấm đúng/sai của dạng bài được chuyển thành lịch SRS.
- Tiến độ: số câu đã xử lý phải tăng đúng một sau mỗi từ được chấm; không tăng khi chỉ lật thẻ hoặc mở đáp án.
- Kết thúc: khi toàn bộ ID trong phiên được xử lý, hiển thị hoàn thành và cho phép thiết lập phiên mới.
- Bộ từ thay đổi trong lúc luyện: phiên hiện tại phải tránh crash hoặc tính nhầm nếu một từ bị xóa/chuyển bộ từ bên ngoài.

## 3. Hợp đồng hành vi cho SRS

- Again: đánh dấu quên, tăng bộ đếm lapse và đưa từ quay lại sớm.
- Hard: lịch gần hơn Good/Easy; không được làm tăng cấp độ như một câu trả lời dễ.
- Good/Easy: tăng số lần nhớ thành công và cập nhật khoảng ôn theo quy tắc đã chọn.
- Mốc thời gian được lưu bằng epoch milliseconds; cần kiểm thử biên khi từ vừa đến hạn, thời gian đổi và app được mở lại.
- Các quy tắc thời gian cuối cùng phải được đối chiếu với LuyenTu tham chiếu trước khi tuyên bố tương đương. Các unit test hiện tại mô tả hành vi của code hiện tại, không tự chứng minh rằng lịch này trùng bản gốc.

## 4. Hợp đồng nhập/xuất dữ liệu

- Từ và nghĩa là bắt buộc; trường IPA, ví dụ, chủ đề là tùy chọn.
- CSV có dấu phẩy/dấu ngoặc kép phải được phân tích đúng; BOM UTF-8 không được làm hỏng tiêu đề.
- Nhập trùng theo term không phân biệt hoa/thường: bỏ qua và báo số lượng, không âm thầm ghi đè từ đã có.
- Tệp rỗng, chỉ có tiêu đề hoặc không có cặp từ-nghĩa hợp lệ phải trả thông báo dễ hiểu.
- Xuất rồi nhập lại không được làm mất các trường.
- Restore JSON thay thế dữ liệu hiện tại, do đó UI phải cảnh báo rõ trước khi thực hiện và lỗi dữ liệu phải không để lại trạng thái nửa khôi phục.

## 5. Giai đoạn 2 đã xử lý trong mã

- Đã tách logic tạo lựa chọn trắc nghiệm thành `MultipleChoiceChoices` để kiểm thử độc lập, vẫn chạy hoàn toàn offline.
- Hàm tạo đáp án trim, chuyển chữ thường và gộp khoảng trắng trước khi loại trùng; đáp án đúng chỉ xuất hiện một lần. Khi không đủ nghĩa khác nhau, hàm trả về ít hơn bốn lựa chọn thay vì bịa đáp án.
- Đã thêm unit tests cho đáp án đúng, nghĩa trùng khác hoa/thường/khoảng trắng, thư viện ít từ và nghĩa đúng rỗng.
- Đã bổ sung kiểm thử chuỗi SRS: Good nhiều lần làm tăng khoảng ôn, sau đó Again đặt lại repetitions/interval và tăng lapse.
- Bộ test CSV hiện có kiểm thử tiêu đề tiếng Việt, trường có dấu phẩy, nhập/xuất vòng lại và bỏ dòng thiếu từ/nghĩa.
- Đặc tả phiên học, SRS, nhập/xuất và điều kiện nghiệm thu đã được ghi rõ ở các mục trên.

## 6. Giới hạn nghiệm thu cần nói rõ

- Luồng phiên học hiện đã chụp danh sách ID lúc bắt đầu và loại từ sau khi chấm; vẫn cần kiểm thử tương tác trên thiết bị khi xóa/chuyển bộ từ hoặc đóng app giữa phiên.
- Chưa có bằng chứng đủ để khẳng định lịch SRS hoặc toàn bộ giao diện/hành vi trùng khớp bản LuyenTu gốc. Unit tests xác nhận quy tắc đang được triển khai, không xác nhận tương đương bản tham chiếu.
- Kiểm thử UI tự động trên thiết bị thật và chế độ máy bay chưa được thực hiện trong môi trường này.

## 7. Kết quả kiểm thử

- Các bài test unit được bổ sung trong `MultipleChoiceChoicesTest.kt` và `SrsSchedulerTest.kt`.
- CI Android chạy `testDebugUnitTest assembleDebug`; cần xem trạng thái run mới nhất và artifact trên GitHub Actions trước khi phát hành APK.

**Trạng thái:** phần sửa lỗi và unit tests thuộc phạm vi Giai đoạn 2 đã được commit lên nhánh `luyentu2.0`. Việc xác nhận cuối cùng phụ thuộc CI của commit mới nhất; kiểm thử thiết bị và đối chiếu trực tiếp với bản gốc được ghi nhận là bước nghiệm thu tiếp theo, không tuyên bố đã làm.
