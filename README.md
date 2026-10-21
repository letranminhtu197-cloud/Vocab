# Vocab — IELTS Vocabulary Offline

Vocab là ứng dụng Android cá nhân học từ vựng IELTS Academic, giao diện tiếng Việt. Bản nền sử dụng Kotlin, Jetpack Compose và Material 3; manifest không khai báo quyền Internet.

## Trạng thái hiện tại
Bản nền có Trang chủ, Lộ trình, flashcard, Sổ tay tìm kiếm, Thống kê, dữ liệu từ mẫu và module SRS độc lập kèm unit tests. Đây **chưa phải bản phát hành đầy đủ** theo master prompt: Room chưa được nối vào UI; tiến độ hiện tại chưa được lưu bền vững; CSV, sao lưu ZIP, audio offline, notifications và bộ 250–300 mục chưa hoàn thành. Chỉ xác nhận APK sau khi GitHub Actions build thành công.

## Build cục bộ
Yêu cầu JDK 17, Android SDK Platform 35 và Gradle 8.9:
```bash
gradle test
gradle assembleDebug
```
APK dự kiến: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions
Workflow `.github/workflows/android.yml` chạy tests, build debug APK, kiểm tra file và tính SHA-256, sau đó tải APK lên artifact `vocab-debug-apk`. Xem tab Actions để biết trạng thái thực tế.

## Cài đặt
Sau khi workflow thành công, tải artifact, giải nén APK và mở trên Samsung Galaxy S22 Ultra. Chỉ cho phép cài ứng dụng từ nguồn đó nếu Android yêu cầu.

## Quyền riêng tư và offline
Ứng dụng không khai báo `android.permission.INTERNET`. Android Auto Backup bị tắt để tránh sao lưu dữ liệu học lên cloud tự động. Build CI cần Internet để tải dependency, nhưng đó không phải quyền mạng của ứng dụng cài trên điện thoại.

## Hạn chế cần hoàn thiện
- Nối Room database với UI để từ vựng và lịch sử được lưu lâu dài.
- Hoàn thiện SRS năm cấp độ và tích hợp vào luồng flashcard/quiz.
- Triển khai Thời điểm vàng và thông báo cục bộ có xử lý quyền Android.
- Import/export CSV chuẩn và backup/restore ZIP.
- Bổ sung dữ liệu khởi tạo đã kiểm duyệt tới 250–300 mục.
- Audio chỉ hiển thị khi có tệp cục bộ thực sự.
- Kiểm tra giao diện và cài đặt trên emulator/thiết bị thật.

CI trigger: workflow uses the runner-provided Android SDK and accepts SDK licenses non-interactively.
