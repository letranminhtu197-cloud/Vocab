# Giai đoạn 3 — Nền tảng thư viện từ vựng

## Đã triển khai trên nhánh phát triển

- `CategoryEntity` và `CategoryDao`: danh mục lưu cục bộ, tên chuẩn hóa có unique index, theo dõi thời gian tạo/cập nhật.
- `WordDao`: quan sát từ và từ đến hạn, lấy snapshot, tra cứu theo ID, đếm, thêm/sửa/xóa.
- `WordLibraryRepository`: ranh giới nghiệp vụ local-only cho CRUD từ và danh mục; kiểm tra từ/nghĩa rỗng, cấp SRS 0–5, số lần ôn âm và trùng từ sau chuẩn hóa Unicode/whitespace.
- Giao diện sổ từ được nối với repository để thêm/sửa/xóa từ; lỗi xác thực hiển thị cho người dùng thay vì làm ứng dụng dừng.
- Nhập CSV có bước xem trước, tự ánh xạ tên cột qua các bí danh phổ biến (ví dụ `word`/`term`, `meaning`/`translation`), đếm từ trùng và dòng không hợp lệ; số dòng vật lý cần kiểm tra được hiển thị trước khi xác nhận nhập.
- Nhập hàng loạt qua Room transaction: từ trùng bên trong file bị loại ở bước preview hoặc bị từ chối tại lớp nghiệp vụ; từ đã có trên máy được bỏ qua; batch được ghi trong một transaction.
- Giao diện quản lý danh mục: tạo, xem, xóa; danh mục có thể được chọn làm chủ đề trong bộ soạn từ.
- Room schema nâng từ version 4 lên 5 bằng migration tạo bảng danh mục và unique index; giữ nguyên chuỗi migration 1→2→3→4→5.
- Unit tests cho chuẩn hóa từ (whitespace/Unicode), dòng CSV không hợp lệ, nhãn khoảng ôn SRS và câu lệnh migration tạo bảng/index danh mục.

## Giới hạn cần nêu rõ

- `WordEntity.topic` vẫn là chuỗi để bảo toàn dữ liệu cũ; bảng `categories` chưa được nối bằng khóa ngoại `categoryId`. Giao diện dùng tên danh mục làm giá trị chủ đề tương thích ngược.
- CSV tự ánh xạ các tên cột đã biết. File có tiêu đề hoàn toàn không nhận diện được vẫn cần đổi tên cột trước khi nhập; chưa có hộp thoại ánh xạ thủ công từng cột.
- Migration 4→5 hiện có unit test xác nhận câu lệnh tạo bảng/index và phiên bản migration; vẫn cần kiểm thử Room instrumentation trên SQLite thật để xác nhận schema thực tế và tính nguyên tử của transaction. CI unit tests/build là bước xác minh đang chạy sau các thay đổi mới nhất.

## Ràng buộc offline

- Không thêm quyền INTERNET, Firebase, billing, quảng cáo, analytics hoặc dịch vụ mạng.
- Không dùng destructive migration; dữ liệu từ vựng hiện có phải được giữ lại.
