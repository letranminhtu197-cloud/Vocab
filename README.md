# LuyenTu 2.0

Ứng dụng Android cá nhân học từ vựng tiếng Anh/IELTS, ưu tiên Samsung Galaxy S22 Ultra.

## Nguyên tắc dự án
- Một người dùng, không đăng nhập, không tài khoản.
- Không quảng cáo, analytics, backend hoặc quyền Internet.
- Dữ liệu học tập được lưu cục bộ trên thiết bị.
- Từ vựng do người dùng tự nhập; không ép dùng bộ từ có sẵn.
- Hỗ trợ import/export dữ liệu bằng file do người dùng chọn.
- Thiết kế giao diện rõ ràng, nhất quán, ưu tiên học chủ động và ôn lặp lại ngắt quãng (SRS).

## Trạng thái
Đang khởi tạo nền tảng LuyenTu 2.0 độc lập trên nhánh `luyentu2.0`. Bản đầu tiên là khung Android Compose; các module cơ sở dữ liệu, nhập từ, bài tập, SRS và kiểm thử sẽ được triển khai theo từng commit. Không coi bản khung này là bản phát hành hoàn chỉnh.

## Build
Yêu cầu JDK 17, Android SDK Platform 35 và Gradle 8.9+.

```bash
gradle assembleDebug
```

APK debug dự kiến tại `app/build/outputs/apk/debug/app-debug.apk`.

## Quyền riêng tư
Ứng dụng không khai báo quyền `INTERNET`. Tính năng đọc/nghe phát âm sẽ chỉ sử dụng khả năng offline trên thiết bị; nếu Android không có gói giọng/ngôn ngữ offline phù hợp, ứng dụng phải thông báo rõ thay vì tự chuyển sang dịch vụ mạng.
