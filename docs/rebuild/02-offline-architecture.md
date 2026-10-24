# Giai đoạn 2 — Kiến trúc Vocab Offline Rebuild

Trạng thái: thiết kế kỹ thuật cơ sở; chưa phải tuyên bố rằng ứng dụng đã được triển khai hoặc build thành APK.

## 1. Các bất biến sản phẩm

1. Miễn phí 100%: không paywall, subscription, in-app purchase hoặc khóa tính năng.
2. Offline 100% trong lúc chạy: không đăng nhập, backend, cloud sync, analytics, quảng cáo, remote config hoặc tải nội dung từ xa.
3. Không cần từ vựng đóng gói sẵn. Cho phép nhập CSV/TSV/TXT/JSON; bộ demo nhỏ chỉ phục vụ kiểm tra và có thể xóa.
4. Dữ liệu học và lịch ôn nằm trên thiết bị. Có xuất/nhập bản sao lưu do người dùng chủ động thực hiện.
5. Không phụ thuộc vào quyền INTERNET. Kiểm tra manifest cuối cùng để bảo đảm quyền này không xuất hiện do dependency kéo theo.
6. Mọi chức năng dựa vào engine Android (TTS/nhận dạng giọng nói) phải kiểm tra khả năng offline và thông báo rõ khi không hỗ trợ.
7. Không dùng tài nguyên độc quyền của APK tham chiếu khi chưa có quyền sử dụng.

## 2. Stack mục tiêu

- Android native: Kotlin.
- UI: Jetpack Compose + Material 3; ưu tiên bố cục rõ ràng, thao tác một tay trên màn hình điện thoại.
- Kiến trúc: feature-first + MVVM, UI state bất biến và luồng dữ liệu một chiều.
- Lưu trữ: Room/SQLite cho từ, danh mục, lịch sử, SRS, phiên học và thống kê; DataStore cho tùy chọn giao diện/nhắc học.
- Tác vụ nền: WorkManager cho việc cần trì hoãn; AlarmManager chỉ khi yêu cầu giờ chính xác thực sự cần thiết và tuân thủ chính sách Android.
- Điều hướng: Navigation Compose.
- Kiểm thử: JUnit cho miền logic/SRS/import; Room tests; Compose UI tests cho luồng chính; kiểm tra APK/manifest ở CI.
- Build: Gradle Kotlin DSL; compile/target SDK hiện hành theo toolchain CI, min SDK Android 7.0/API 24 để phù hợp mức tối thiểu thấy trong APK tham chiếu, trừ khi dependency cần mức cao hơn.

Không đưa Flutter/Firebase/Billing SDK vào kiến trúc mới. Các dependency phải được rà soát để không kéo mạng vào app.

## 3. Các module/lớp

```
app
├── core/model          # Word, Category, ReviewState, StudySession, LearningEvent
├── core/database       # Room entities, DAO, migrations
├── core/data           # Repository, import/export, backup/restore
├── core/learning       # SRS scheduler, session planner, answer grading
├── core/speech         # Local TTS capability checks; optional on-device recognition
├── core/designsystem   # Theme, reusable components, accessibility
├── feature/home        # Today's queue, progress, streak
├── feature/library     # Word CRUD, categories, tags, search, import
├── feature/study       # Session orchestration and exercise router
├── feature/exercises   # Flashcard, MCQ, typing, context gap, matching, listening
├── feature/srs         # Due queue, review result, schedule settings
├── feature/roadmap     # Local goal and study plan
├── feature/statistics  # Accuracy, retention, time, streak and trends
├── feature/widgets     # Home-screen widgets backed by repository
├── feature/reminders   # Local notification settings and schedule
└── feature/settings    # Import/export, appearance, local data controls
```

Đây là ranh giới logic dự kiến, không bắt buộc mỗi dòng phải là Gradle module riêng. Có thể bắt đầu bằng một app module với package boundaries rõ ràng để tránh over-modularization.

## 4. Mô hình dữ liệu cốt lõi

### Word
- `id` (UUID/string ổn định)
- `term`, `normalizedTerm`, `phonetic`, `partOfSpeech`
- `meaning`, `definition`, `example`, `exampleTranslation`, `notes`
- `categoryId`, `tags`, `audioReference` (optional local file/reference)
- `createdAt`, `updatedAt`, `archivedAt` (nullable)

### ReviewState (một bản ghi cho mỗi từ)
- `wordId`, `level` (0–5), `dueAt`, `lastReviewedAt`
- `consecutiveCorrect`, `lapseCount`, `lastRating`
- `updatedAt`, `scheduleVersion`

### LearningEvent / StudySession
- event/session id, word id, exercise type, result, latency, timestamp, session id.
- Không lưu nội dung ghi âm nhạy cảm mặc định; nếu có ghi âm thì phải là thao tác rõ ràng và lưu cục bộ.

### Category / Goal / AppSettings
- Danh mục, mục tiêu học cá nhân, số từ mới/ngày, tùy chọn bài tập, thời gian nhắc và giao diện.
- Không lưu ID tài khoản cloud hoặc token mạng.

## 5. SRS và quy tắc lịch ôn

Thiết kế SRS phải độc lập UI để kiểm thử. Mốc 6 cấp của app tham chiếu là bằng chứng về tên cấp, **không** đủ để suy ra chính xác lịch ngày của họ. Vocab cần có lịch mặc định được tài liệu hóa, cấu hình được và có test; không giả vờ là bản sao thuật toán nội bộ chưa biết.

Đề xuất phiên bản thuật toán ban đầu:
- Cấp 0: từ mới/chưa ổn định.
- Cấp 1–5: tăng dần độ ổn định.
- Mỗi câu trả lời phân loại thành Again / Hard / Good / Easy; có thể ánh xạ từ kết quả dạng bài.
- Sai hoặc “Again”: đưa từ vào hàng thử lại trong phiên sau một khoảng ngắn, không làm mất lịch sử.
- Đúng: cập nhật cấp và dueAt theo bảng khoảng cách được định nghĩa trong cấu hình, lưu scheduleVersion để có thể thay đổi sau này.
- Giữ lịch sử sự kiện để tính độ chính xác và phục hồi khi đổi thuật toán.
- Các mốc ngày cuối cùng sẽ được chốt bằng test và cấu hình rõ trong bước triển khai, không tuyên bố tương đương Luyện Từ khi chưa có bằng chứng.

## 6. Luồng phiên học

1. Người dùng chọn bộ từ/mục tiêu và số lượng.
2. Session planner lấy từ mới và từ đến hạn từ Room.
3. Bộ điều phối chọn dạng bài theo cấu hình và trạng thái từ; flashcard dùng để giới thiệu, không độc quyền cho ôn tập.
4. Grader chuẩn hóa câu trả lời (Unicode, hoa/thường, khoảng trắng; tùy chọn bỏ dấu chỉ khi người dùng bật).
5. Learning engine ghi event, cập nhật ReviewState và tiến độ trong transaction.
6. Tóm tắt cuối phiên hiển thị số câu, độ chính xác, thời gian và từ cần củng cố.

Các dạng bài ưu tiên: Flashcard, Multiple Choice, Typing, Contextual Fill-in-the-Blank, Matching, Listening/Listen-and-Type khi TTS offline khả dụng, Translation/Sentence và Mixed. Game được tách module, có thể tắt và không được can thiệp lịch SRS ngoài các kết quả học đã xác định.

## 7. Quy tắc nhập/xuất

- CSV/TSV/TXT/JSON, UTF-8, xử lý BOM và dòng rỗng.
- Preview trước khi nhập, ánh xạ cột, báo lỗi theo dòng, chọn xử lý trùng (bỏ qua/cập nhật/nhập thành từ mới).
- Import phải chạy trong transaction hoặc rollback an toàn; không xóa dữ liệu hiện có nếu lỗi.
- Export có schemaVersion, xuất cả từ/danh mục/SRS/lịch sử theo tùy chọn.
- Backup/restore kiểm tra tính hợp lệ, cảnh báo dữ liệu sẽ bị thay thế và tạo bản sao lưu trước khi restore.
- Dữ liệu mặc định ở app-private storage; không yêu cầu quyền truy cập toàn bộ bộ nhớ.

## 8. Offline, quyền và bảo mật

- Không khai báo `android.permission.INTERNET`; CI sẽ kiểm tra merged manifest.
- Không tích hợp Firebase, Play Billing, quảng cáo, crash reporting có truyền dữ liệu, đăng nhập Google hay remote APIs.
- Chỉ dùng Android document picker để người dùng chủ động chọn file nhập/xuất.
- TTS: kiểm tra engine và voice đã cài; nêu rõ khi voice cần tải thêm. Không tự tải voice hoặc dùng mạng.
- Nhận dạng giọng nói: chỉ bật nếu hệ thống xác nhận xử lý on-device; nếu không, ẩn/disable dạng bài hoặc yêu cầu chuyển sang dạng gõ.
- Thông báo: dữ liệu và nội dung nhắc được tạo cục bộ; yêu cầu quyền thông báo khi cần; lịch phải được phục hồi sau reboot nếu dùng alarm.
- Không cần tài khoản; dữ liệu có thể bị mất khi gỡ cài đặt nếu chưa export, vì vậy cần nhắc người dùng sao lưu.

## 9. Giao diện/UX

- Bottom navigation dự kiến: Hôm nay, Từ vựng, Học, Thống kê, Cài đặt.
- Trang Hôm nay: tiến độ mục tiêu, từ đến hạn, nút bắt đầu học, streak và lịch sử ngắn.
- Trang Từ vựng: tìm kiếm nổi bật, bộ lọc danh mục/tag/trạng thái, thêm/sửa/xóa, nhập dữ liệu.
- Màn học: một nhiệm vụ chính mỗi màn hình, trạng thái tiến trình rõ, nút lớn, thao tác không gây nhầm lẫn; giải thích đáp án sau phản hồi.
- Không dùng premium badges, khóa tính năng, tiền ảo ép buộc hoặc luồng mua hàng.
- Accessibility: cỡ chữ thích ứng, TalkBack labels, tương phản tốt, không chỉ dùng màu để biểu đạt đúng/sai.

## 10. Chiến lược kiểm thử và Definition of Done

- Unit tests cho SRS ở ranh giới cấp, thời gian, sai/đúng, múi giờ và ngày đổi.
- Tests cho chuẩn hóa từ, chấm câu trả lời, randomizer không lặp bất hợp lý, session planner.
- Import tests: BOM, dấu tiếng Việt, delimiter, cột thiếu, duplicate, tệp rỗng/lớn và rollback.
- Database tests: migration, transaction, cascade behavior và restore.
- UI tests: thêm/sửa từ, import, bắt đầu phiên, trả lời, thoát/tiếp tục, xem lịch ôn, export/restore.
- CI checks: assembleDebug, test, lint, merged manifest không có INTERNET, quét dependency cho billing/ads/Firebase/analytics.
- Kiểm thử thực tế trên Samsung Galaxy S22 Ultra/Android của người dùng vẫn là bước cần thiết; CI không thay thế được kiểm tra thiết bị.

Giai đoạn 2 chỉ được coi là thiết kế hoàn tất khi các bất biến, module boundaries, schema, luồng học, offline policy và kiểm thử đã được thống nhất. Việc triển khai, build APK và kiểm thử thiết bị thuộc các giai đoạn tiếp theo.
