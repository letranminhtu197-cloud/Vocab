# LuyenTu 2.0

Ứng dụng Android cá nhân học từ vựng tiếng Anh/IELTS, ưu tiên Samsung Galaxy S22 Ultra.

## Nguyên tắc
- Không tài khoản, quảng cáo, analytics hay backend.
- Ứng dụng không khai báo quyền `INTERNET`; học và quản lý bộ từ không cần mạng.
- Từ vựng, lịch ôn và lịch sử ôn lưu trong SQLite cục bộ.
- Người dùng tự nhập bộ từ; không bắt buộc tải bộ từ có sẵn.
- Nhập/xuất CSV thông qua bộ chọn tệp của Android.
- Hướng phát triển: nhiều dạng bài tập chủ động, SRS, thống kê và sao lưu.

## Đã triển khai trên nhánh `luyentu2.0`
- Giao diện Android bằng Kotlin + Jetpack Compose.
- Sổ từ: thêm, tìm kiếm và xóa từ; trường từ tiếng Anh, nghĩa, IPA, chủ đề và ví dụ.
- Lịch ôn cách quãng cơ bản với lựa chọn Quên/Khó/Nhớ/Dễ.
- Thống kê số từ, lượt ôn và số từ đến hạn.
- Nhập/xuất CSV; bỏ qua từ trùng thay vì ghi đè dữ liệu.
- GitHub Actions build APK debug tự động.

## Trạng thái phát triển
Đây vẫn là bản alpha, chưa phải bản phát hành hoàn chỉnh và chưa xác nhận hoạt động trên điện thoại thật. Các dạng bài tập bổ sung, kiểm thử SRS/CSV, kiểm thử offline trên thiết bị và xác nhận APK build thành công vẫn cần hoàn thành.

## Build
Yêu cầu JDK 17, Android SDK Platform 35 và Gradle 8.9+.

```bash
gradle assembleDebug
```

APK debug dự kiến tại `app/build/outputs/apk/debug/app-debug.apk`. Chỉ coi APK sẵn sàng khi workflow build hoàn tất thành công.

## Quyền riêng tư
Ứng dụng không khai báo quyền `INTERNET`. Việc chọn tệp trong Android dùng Storage Access Framework; nếu người dùng tự chọn nhà cung cấp đám mây, việc chuyển tệp qua nhà cung cấp đó có thể cần mạng. Việc học trong ứng dụng vẫn được thiết kế để chạy cục bộ.
