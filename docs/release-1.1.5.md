# Ghi chú phát hành Vocab 1.1.5

Ngày build: 2026-10-22. Môi trường: Windows, JDK 17 (Microsoft OpenJDK 17.0.20.1), Gradle 8.9,
Android Gradle Plugin 8.7.3, Kotlin 2.0.21, compileSdk 35, build-tools 34.0.0 + 36.0.0.

## Kết quả đã xác minh

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **69 test, 0 failure, 0 skip** (12 lớp test) |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK đầu ra | `app/build/outputs/apk/debug/app-debug.apk` → sao chép thành `D:\Dự án\Vocab-1.1.5-debug.apk` |
| Kích thước | 16.733.925 byte (~16,0 MB) |
| SHA-256 | `B053CA6EFD7642B74C28779F3B6762BE6CE174E16A131CD5401A76BBE1D94FC7` |
| `aapt2 dump badging` | `com.vocab.ielts.offline`, `versionCode='115'`, `versionName='1.1.5'`, minSdk 26, targetSdk 35 |
| `aapt2 dump permissions` | `RECORD_AUDIO`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`; **không có `android.permission.INTERNET`** |
| Khớp mã nguồn | `app/build.gradle.kts` khai báo `versionCode = 115`, `versionName = "1.1.5"` |
| Tái lập | Build lại từ cây git đã commit (HEAD nhánh `main`) cho ra APK **trùng SHA-256** và `69 test / 0 failure` |

## Thay đổi mã nguồn trong bản này

Đối chiếu chi tiết ở `docs/mochivocab-comparison-1.1.5.md`. Tóm tắt:

1. `ReviewStore.kt` (mới) — ghi kết quả ôn trong transaction, đọc bản ghi mới nhất trước khi áp SRS;
   thay ba chỗ ghi đè bằng bản sao trong `MainActivity`.
2. `WordStatus.kt` (mới) — một định nghĩa duy nhất cho "đến hạn"/"từ yếu"/"từ mới"; dùng ở
   `MainActivity`, `QuizScreen`, `LearningPathLogic`.
3. `QuizLogic.kt` — chấp nhận biến thể dấu phân cách khi so đáp án gõ tay (`compactAnswer`).
4. `WordDetailSheet.kt` (mới) + `MainActivity` — chi tiết từ dạng bottom sheet, có "Luyện từ này".
5. `QuizScreen.kt` — chế độ luyện riêng một từ (`focusWordId`), ẩn bộ chọn chế độ khi đang luyện riêng.
6. `LibrarySorting.kt` (mới) — sắp xếp A→Z / Mới thêm / Đến hạn trước trong tab Sổ từ.
7. `StudyQueueLogic.priorityReason` + `LessonScreen` — hiển thị lý do ưu tiên trên thẻ bài học.
8. Test mới: `WordStatusTest` (6), `LibrarySortingTest` (3), 2 ca biến thể đáp án trong `QuizLogicTest`,
   1 ca lý do ưu tiên trong `StudyQueueLogicTest`; cập nhật `LearningPathLogicTest` theo định nghĩa "từ yếu" mới.

Không có thay đổi schema Room (vẫn version 3), không cần migration, không thêm quyền, không thêm thư viện.

## Lệnh tái lập

```bash
# yêu cầu JDK 17, SDK platform 35, build-tools 34.0.0
gradle test
gradle assembleDebug
aapt2 dump badging  app/build/outputs/apk/debug/app-debug.apk
aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk
```

## Giới hạn của lần xác minh này

- Không chạy lại được GitHub Actions từ máy này: không có `git`/`gh` cài sẵn, không có thông tin đăng nhập
  GitHub (kiểm tra `~/.git-credentials`, `~/.ssh`, `cmdkey /list` đều trống). Vì vậy **chưa push** và
  **chưa có lượt CI mới**; APK ở trên do máy cục bộ build, không phải artifact CI.
- Chưa cài APK lên thiết bị thật (không có thiết bị kết nối; emulator trên máy là x86_64 nên không chạy
  được APK arm64 của MochiVocab, còn APK Vocab thì chưa được kiểm thử thao tác tay).
- Các sai lệch do môi trường build cục bộ (không nằm trong repo): Gradle/JDK được tải bổ sung; SDK thiếu
  platform 35 và build-tools 34 nên được cài thủ công; Java trên máy bị hạn chế ghi ngoài workspace nên
  build dùng `-Djava.io.tmpdir` và `-Duser.home` trỏ vào thư mục làm việc.
