# Vocab 1.2.0

## Cải tiến

- Thu gọn thanh điều hướng thành Hôm nay, Học, Sổ từ và Tiến độ.
- Chuyển flashcard, lộ trình theo chủ đề và bài tập vào cùng khu Học; thêm trò ghép từ với nghĩa theo chủ đề.
- Trang Hôm nay gom lời nhắc đến hạn, tiến độ mục tiêu và thử thách ngày vào một luồng. Tùy chọn mục tiêu và giới hạn phiên được ẩn đến khi cần.
- Sổ từ rút gọn thẻ danh sách để quét nhanh; chi tiết từ tiếp tục chứa ví dụ, IPA, cụm từ, ghi chú và trạng thái SRS.
- Trang Tiến độ gom các số liệu cơ bản và chứa cài đặt nhắc học.
- Giữ thang SRS đã được sửa gần nhất trên `main`: 1, 3, 7, 14 và 30 ngày. Lịch đã lưu không bị đặt lại; các đánh giá mới dùng thang này.
- Cập nhật [bảng đối chiếu tính năng MochiVocab](mochivocab-feature-parity.md). Vocab triển khai lại các luồng học phù hợp với ứng dụng offline, không lấy nội dung hay tài sản của ứng dụng tham khảo.

## Tương thích dữ liệu

- Nâng versionCode từ 124 lên 125, versionName từ 1.1.14 lên 1.2.0.
- Không đổi schema Room (version 4); dữ liệu từ, lịch SRS đã lưu và thiết lập cục bộ được giữ nguyên.
- Không thêm quyền hoặc phụ thuộc thư viện; ứng dụng vẫn không khai báo quyền Internet.

## Kiểm chứng

- Chưa chạy unit tests theo yêu cầu công việc.
- Chưa biên dịch APK cục bộ vì môi trường hiện tại không có lệnh Gradle. Pull request sẽ chạy workflow Android hiện có để kiểm tra test, build APK và quyền offline.
- Chưa cài thử trên điện thoại thật.

## Giới hạn tính năng

Ảnh minh họa gắn vào thẻ từ, từ điển và kho khóa học trực tuyến, tài khoản, đồng bộ, bảng xếp hạng,
bạn bè, cộng đồng, sự kiện và thanh toán chưa có trong ứng dụng offline. Chi tiết nằm trong bảng đối chiếu.
