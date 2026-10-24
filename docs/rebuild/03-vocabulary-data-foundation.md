# Giai đoạn 3 — Nền tảng thư viện từ vựng

## Đã đưa lên nhánh phát triển

- `CategoryEntity` và `CategoryDao`: danh mục lưu cục bộ, tên chuẩn hóa có unique index, theo dõi thời gian tạo/cập nhật.
- Mở rộng `WordDao`: snapshot nhất quán cho tác vụ nghiệp vụ, tra cứu từ theo ID, đếm từ, quan sát toàn bộ và từ đến hạn.
- `WordLibraryRepository`: ranh giới nghiệp vụ local-only cho CRUD từ và danh mục; kiểm tra từ/nghĩa rỗng, cấp SRS ngoài phạm vi, số lần ôn âm và trùng từ sau chuẩn hóa Unicode/whitespace.
- Nhập hàng loạt qua Room transaction: từ sai hoặc trùng bên trong tệp bị từ chối trước khi ghi; từ đã có trên máy được bỏ qua; cả batch được ghi trong một transaction.
- Nâng Room schema từ version 4 lên 5 với migration tạo bảng danh mục và unique index; các migration 1→2→3→4 vẫn được giữ.
- Unit test cho chuẩn hóa từ, khoảng trắng và Unicode.

## Phạm vi chưa hoàn tất

Đây là nền tảng dữ liệu, chưa phải toàn bộ giai đoạn 3. Các bước còn lại:
1. Nối giao diện thư viện hiện tại vào repository và thêm màn hình quản lý danh mục.
2. Xây luồng nhập file hoàn chỉnh có preview, ánh xạ cột, báo lỗi theo số dòng và lựa chọn xử lý trùng.
3. Bổ sung kiểm thử Room migration/transaction trên Android.
4. Chạy CI xác nhận unit tests, assembleDebug và kiểm tra merged APK không có quyền INTERNET.

## Lưu ý kỹ thuật

- Không thêm quyền INTERNET hay dịch vụ mạng.
- Room migration được thiết kế giữ dữ liệu cũ, không dùng destructive migration.
- `WordEntity.topic` hiện có trong schema cũ nên vẫn tương thích dữ liệu chủ đề cũ; bảng `categories` là danh mục độc lập trong bước nền tảng này. Quan hệ chính thức giữa từ và danh mục sẽ được nối trong bước tiếp theo để tránh tự động diễn giải lại dữ liệu cũ.
- Chưa tuyên bố build thành công cho tới khi GitHub Actions hoàn tất.
