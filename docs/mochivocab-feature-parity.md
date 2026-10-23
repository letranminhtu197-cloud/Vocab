# Đối chiếu tính năng MochiVocab — Vocab 1.2.0

Cập nhật: 2026-10-23. Bảng này đối chiếu hành vi học, không sao chép giao diện, nội dung, mã nguồn,
âm thanh hay hình ảnh của MochiVocab. Bản tham khảo cục bộ là MochiVocab 5.16.2; phần logic Flutter
được biên dịch AOT nên không thể xác minh thuật toán hoặc từng luồng nội bộ. Mô tả tính năng dựa trên
[trang tính năng chính thức của MochiVocab](https://mochidemy.com/blog/mochimochi-tinh-nang/).

## Tính năng học

| Nhóm | Vocab hiện tại | Mức tương đương |
|---|---|---|
| Ôn đúng lúc / “Thời điểm vàng” | Lịch SRS cục bộ, số từ đến hạn, nhắc theo mốc gần nhất | Tương đương về mục đích; công thức nội bộ khác |
| Năm cấp độ ghi nhớ | Năm cấp SRS, biểu đồ phân bố và bốn mức tự đánh giá | Tương đương về biểu diễn; lịch khoảng cách do Vocab tự định nghĩa |
| Flashcard | Nghĩa, IPA, từ loại, TTS thường/chậm, ví dụ và dịch ví dụ | Tương đương phần học; chưa hỗ trợ ảnh minh họa |
| Sổ tay từ | Tìm kiếm, lọc, sắp xếp, xem chi tiết, tạm dừng từng từ | Tương đương |
| Học và ôn | Flashcard, lộ trình theo chủ đề, bài tập nhiều dạng, ôn từ yếu/đến hạn | Tương đương về nhóm luồng |
| Trò chơi | Ghép từ–nghĩa ngoại tuyến theo nhóm nhỏ, ngoài các dạng gõ, nghe và xáo chữ | Thêm cách luyện riêng |
| Khóa học | Nhập CSV/TSV/TXT hoặc gói JSON nhiều bài; chủ đề do người học quản lý | Một phần; không đồng bộ kho khóa học của Mochi |
| Mục tiêu ngày, streak, thử thách | Mục tiêu, chuỗi ngày, thử thách từ trong sổ và thành tựu lưu cục bộ | Tương đương cho việc tạo thói quen |
| Vườn tiến độ | Mục tiêu, biểu đồ hoạt động, phân bố SRS và thành tựu cá nhân | Thay bằng tiến độ thuần nội bộ; không có cây/vật phẩm |
| Phát âm | Giọng tiếng Anh offline của thiết bị; có nhận diện giọng nói offline nếu máy hỗ trợ | Tương đương mục đích; phụ thuộc gói ngôn ngữ trên máy |

## Những tính năng cần dịch vụ của Mochi

Tài khoản, đồng bộ máy chủ, bảng xếp hạng, bạn bè, cộng đồng, tin tức/sự kiện, mua hàng và thư viện
từ điển/khóa học trực tuyến không nằm trong Vocab offline. Thêm chúng cần backend, quyền mạng, chính
sách dữ liệu và nội dung được cấp phép riêng. Không nhập bộ từ, âm thanh, hình minh họa, animation,
font hoặc dữ liệu đóng gói từ MochiVocab.

## Tổ chức giao diện 1.2.0

- **Hôm nay**: một việc tiếp theo, tiến độ mục tiêu và thử thách ngày.
- **Học**: flashcard, lộ trình, bài tập và ghép cặp ở cùng một khu.
- **Sổ từ**: thẻ danh sách rút gọn; thông tin đầy đủ vẫn ở màn hình chi tiết.
- **Tiến độ**: số liệu chính, hoạt động 7 ngày, chủ đề, từ cần ưu tiên, năm cấp nhớ và thành tựu.

Đây là phạm vi tương đương khả thi với kiến trúc offline hiện tại, không phải bản sao 1:1 của một
sản phẩm trực tuyến. Ảnh gắn vào thẻ từ và các dịch vụ xã hội vẫn là khoảng trống nếu muốn tiếp tục
mở rộng.
