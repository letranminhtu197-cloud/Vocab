# Đối chiếu MochiVocab ↔ Vocab và phạm vi thay đổi 1.1.5

Ngày kiểm tra: 2026-10-22. Tài liệu này chỉ ghi lại những gì đã kiểm tra trực tiếp trên tệp có trong máy,
không suy diễn mã nguồn Dart gốc.

## 1. Vật liệu đã kiểm tra và giới hạn

| Vật liệu | Đường dẫn | Đã kiểm tra |
|---|---|---|
| XAPK gốc | `D:\Dự án\MochiVocab+-+Learn+English_5.16.2_APKPure.xapk` | ZIP 5 split + `manifest.json`: base `com.mochimochi.android.en.apk`, `config.arm64_v8a`, `config.xxhdpi`, `config.en`, `config.ja`; không có OBB |
| APK gộp (tự tạo) | `D:\Dự án\MochiVocab-5.16.2-universal-arm64.apk` | `aapt2 dump badging`: `com.mochimochi.android.en`, versionCode `3805`, versionName `5.16.2`, minSdk 24, targetSdk 37; `aapt2 dump permissions`: 42 quyền, có `INTERNET`, `ACCESS_NETWORK_STATE`, `BILLING`, `AD_ID`, `RECORD_AUDIO` |
| Java dịch ngược | `C:\Users\minht\Desktop\MochiVocab\sources` | JADX dump 7.513 tệp; phần `com/mochimochi` chỉ có 5 lớp phía Android (`MainActivity`, `MochiWidgetProvider`, `MochiLiveActivityService`, `MochiDateChangeReceiver`, `b`, `c`, `R`). Logic nằm trong Flutter AOT, không phải Java |
| Tài nguyên giải mã | `C:\Users\minht\Desktop\MochiVocab\resources` | `res/values*/strings.xml`, `assets/flutter_assets` (413 tệp: 19 mp3, 36 Lottie JSON, 317 ảnh/icon, 16 font), `assets/json/learn_vocab_data.json` |
| Chuỗi trong `libapp.so` | `resources\config.arm64_v8a.apk\lib\arm64-v8a\libapp.so` (15,6 MB) | Quét `package:mochi_vocab_flutter/[A-Za-z0-9_/\-]+\.dart` thu được **937 đường dẫn Dart duy nhất**, 149 đoạn thư mục; danh sách lưu tại `work/mochi-dart-paths.txt` |

Giới hạn: `libapp.so` là mã Dart AOT. Các đường dẫn tệp và chuỗi còn lại cho biết **kiến trúc tính năng**,
không cho biết hằng số thời gian SRS, công thức tính điểm hay nội dung chấm câu. Vì vậy mọi kết luận về
thuật toán của MochiVocab dưới đây đều ghi rõ là suy luận từ tên/đường dẫn, không phải đọc mã.

## 2. Nhóm chức năng của MochiVocab (bằng chứng: đường dẫn Dart + asset)

| Nhóm | Bằng chứng tiêu biểu | Diễn giải |
|---|---|---|
| Luồng học | `features/flow_learn/...`, `features/flow_review/...`, `features/flow_game/presentation/game/write_game_*.dart` | Có phiên học, phiên ôn và trò chơi viết từ tách khỏi phiên học |
| Ôn nhanh / giới hạn ôn | `fast_review`, `review_word_limit` | Có chế độ ôn nhanh và khái niệm "giới hạn số từ ôn" |
| Sổ từ / tra cứu | `notebook_v2`, `add_word`, `dictionary_v2`, `dictionary_search`, `detail_note_level` | Sổ từ nhiều tầng, thêm từ, tra cứu, chi tiết từ theo "level" |
| Lộ trình / nhiệm vụ | `daily_task`, `daily_vocab_challenge`, `streak`, `achievements`, `mochi_garden`, `current_plant`, `get_item` | Chuỗi ngày, nhiệm vụ ngày, thành tựu, vườn cây và vật phẩm |
| Nội dung đóng gói | `assets/json/learn_vocab_data.json` (6.715 byte, 4 bài) | Mỗi câu gồm `paragraph`, `question`, `trueAnswer`, `falseAnswers[]`, `explain`; đây là nội dung sẵn của hãng |
| Âm thanh | 19 tệp `assets/audio/*.mp3` (`audio_flip_flashcard`, `ending_game_show_result`, `star_notice_streak_day`, …) | Phản hồi âm thanh theo sự kiện học, không phải TTS |
| Hình ảnh/animation | 317 ảnh, 36 Lottie | Phản hồi trực quan dày, cần asset riêng |
| Cần mạng / tài khoản | `auth`, `payment`, `leaderboard`, `community`, `event`, `support_ticket`, `firebase`, `connectivity`, `force_update` | Các phần không thể dùng offline |
| Khác | `conversation`, `ielts_zone`, `friends`, `create_room` | Hội thoại, khu IELTS, bạn bè, phòng học nhóm |

Kết luận: MochiVocab là sản phẩm **online-first** (42 quyền, có billing, tài khoản, bảng xếp hạng). Những gì
đáng học là *mô hình học* (phiên học/ôn tách biệt, ôn nhanh, chi tiết từ, chuỗi ngày, nhiệm vụ), không phải
phần hạ tầng mạng hay nội dung đóng gói.

## 3. Vocab 1.1.4 — hiện trạng đã xác minh

- APK người dùng cung cấp: `D:\Dự án\Vocab-1.1.4-debug.apk` → `aapt2 dump badging`:
  `com.vocab.ielts.offline`, versionCode `114`, versionName `1.1.4`, minSdk 26, targetSdk 35.
- Mã nguồn: `github.com/letranminhtu197-cloud/Vocab` nhánh `main`, commit mới nhất `ee0846e3`
  ("Document end-of-session retry for forgotten lesson cards"), `app/build.gradle.kts` khai báo
  `versionCode = 114`, `versionName = "1.1.4"` → **APK khớp mã nguồn mới nhất**.
- GitHub Actions: 5 lần chạy gần nhất của workflow "Android build" đều `success`, lần cuối tại `ee0846e3`.
- Quyền trong APK: `RECORD_AUDIO`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`; **không có `INTERNET`**.

## 4. Bảng đối chiếu

Phân loại: 1 = đã có và kiểm chứng; 2 = có nhưng thiếu/lỗi; 3 = có giao diện nhưng chưa hoạt động;
4 = không có; 5 = không phù hợp với yêu cầu offline.

| Tính năng MochiVocab | Bằng chứng phía MochiVocab | Vocab | Trạng thái | Ghi chú |
|---|---|---|---|---|
| Phiên học có thứ tự ưu tiên | `flow_learn` | `LessonScreen.kt`, `StudyQueueLogic.kt` | 1 | Ưu tiên đến hạn/mới/yếu, có retry cuối phiên |
| Ôn nhanh từ đến hạn | `fast_review` | `QuizScreen` chế độ "Đến hạn" | 1 | |
| Giới hạn số từ mỗi phiên | `review_word_limit` | `lesson_target` (5/10/15/20) | 1 | |
| Lịch ôn ngắt quãng | không đọc được hằng số trong AOT | `SrsScheduler.kt` (4h/24h/72h/168h/720h, 4 mức đánh giá) | 1 | Có unit test |
| Flashcard lật thẻ | `audio_flip_flashcard` | tab Trang chủ | 1 | |
| Trắc nghiệm chọn nghĩa | `flow_game` | `QuizScreen` "Chọn nghĩa" | 1 | |
| Gõ từ / viết từ | `write_game_3.dart` | "Gõ từ", "Nghe & gõ" | 1 | |
| Điền từ vào câu | không có bằng chứng trực tiếp | "Điền từ" (`clozeSentence`) | 1 | |
| Nói từ | `conversation` (suy luận) | "Nói từ" qua nhận diện offline | 2 | Phụ thuộc gói nhận diện tiếng Anh trên máy |
| Tự viết câu | không có bằng chứng | "Viết câu" | 2 | Chỉ tự đánh giá, không chấm ngữ pháp |
| Xử lý câu sai | `flow_game` (suy luận) | tổng kết phiên + luyện lại từ sai | 1 | |
| Từ yếu / hay quên | `detail_note_level`, `review_word_limit` (suy luận) | "Từ yếu" | 2 | Định nghĩa lệch giữa các màn hình → đã sửa ở 1.1.5 |
| Chi tiết từ | `detail_note_level`, `dictionary_v2` | trước đây chỉ có thẻ trong sổ từ | 4 → 1 | Thêm `WordDetailSheet` ở 1.1.5 |
| Tra cứu từ điển | `dictionary_search` | — | 5 | Cần dữ liệu từ điển lớn; trái yêu cầu "không nạp sẵn" |
| Sổ từ + lọc + tìm kiếm | `notebook_v2` | tab Sổ từ | 1 | 1.1.5 thêm sắp xếp |
| Lộ trình theo chủ đề | `daily_task` (một phần) | tab Lộ trình | 1 | |
| Chuỗi ngày / mục tiêu ngày | `streak`, `daily_task` | `StudyActivityTracker`, `StudyGoalLogic` | 1 | |
| Thành tựu | `achievements` | `AchievementLogic` (6 huy hiệu) | 1 | |
| Vườn cây / vật phẩm | `mochi_garden`, `current_plant`, `get_item` | — | 5 | Gắn với server, thanh toán |
| Bảng xếp hạng / cộng đồng | `leaderboard`, `community`, `friends` | — | 5 | Cần mạng |
| Đăng nhập / thanh toán | `auth`, `payment` | — | 5 | Cần mạng |
| Nhắc học | thông báo đẩy (FCM) | `StudyReminder` (AlarmManager cục bộ) | 1 | Không cần mạng |
| Âm thanh phản hồi | 19 mp3 | TTS offline | 1 | Khác cách làm, cùng mục đích |
| Nội dung bài học đóng gói | `learn_vocab_data.json` | — | 5 | Vocab chỉ dùng dữ liệu người dùng tự nhập |
| Nhập / xuất dữ liệu | `add_word` (suy luận) | CSV + ZIP backup | 1 | |

## 5. Khiếm khuyết đã sửa trong 1.1.5

### 5.1. Ghi đè SRS bằng bản sao cũ (mất lượt ôn)

- Hiện tượng: cả ba nơi ghi SRS (`MainActivity` flashcard, `LessonScreen.onReview`, `QuizScreen.onAnswerRecorded`)
  đều tính từ `WordEntity` đang hiển thị rồi `dao.update(word.copy(...))` cả dòng. Hai lượt trả lời sát nhau
  (chuyển câu nhanh, đổi chế độ, bấm đúp) có thể ghi đè bằng `reviewCount` cũ → mất một lượt ôn và lệch lịch.
- Sửa: `ReviewStore.rate(wordId, rating, now)` đọc lại bản ghi trong `withTransaction` rồi mới áp `SrsScheduler`.
- Tệp: `ReviewStore.kt` (mới), `MainActivity.kt`, `WordDao.kt` (không đổi), `QuizScreen.kt` (không đổi chữ ký).
- Migration: không cần (cùng schema v3).
- Kiểm thử: `SrsSchedulerTest` (có sẵn) + kiểm thử thủ công hai lượt liên tiếp.
- Ảnh hưởng offline: không.

### 5.2. Ba định nghĩa "đến hạn"/"từ yếu" khác nhau

- Hiện tượng: sổ từ coi "yếu" là `lapseCount > 0 || (reviewCount > 0 && srsLevel <= 1)`, quiz chỉ coi
  `lapseCount > 0`, lộ trình lại chỉ đếm `lapseCount > 0`; "đến hạn" được viết lại ở 6 chỗ.
  Hệ quả: số liệu ở Tổng quan, Sổ từ, Luyện tập và Thống kê không khớp.
- Sửa: `WordStatus.kt` là nguồn duy nhất; thay toàn bộ chỗ kiểm tra trong `MainActivity`, `QuizScreen`,
  `LearningPathLogic`.
- Migration: không.
- Kiểm thử: `WordStatusTest` (6 ca).
- Offline: không ảnh hưởng.

### 5.3. Chấp nhận đáp án gõ tay quá khắt khe

- Hiện tượng: `normalizeAnswer` biến mọi dấu phân cách thành khoảng trắng nên "well-being" ≠ "wellbeing" ≠
  "well being", "don't" ≠ "dont" — người học gõ đúng từ vẫn bị chấm sai.
- Sửa: `QuizLogic.compactAnswer` bỏ khoảng trắng khi so sánh; `isCorrectAnswer` chấp nhận cả hai dạng.
- Migration: không.
- Kiểm thử: 2 ca mới trong `QuizLogicTest` (chấp nhận biến thể, không chấp nhận từ khác nghĩa).
- Offline: không.

### 5.4. Sổ từ thiếu màn hình chi tiết

- Hiện tượng: toàn bộ trường (IPA, collocations, synonyms, word family, tags, notes, lịch ôn) chỉ hiện
  trong thẻ danh sách; không xem được trạng thái ghi nhớ đầy đủ, không luyện riêng một từ.
- Sửa: `WordDetailSheet.kt` (bottom sheet) hiển thị đủ trường + cấp SRS + lịch ôn kế tiếp + hành động
  "Luyện từ này" / "Sửa" / "Xóa"; `QuizScreen` nhận `focusWordId` để luyện riêng một từ ở chế độ gõ.
- Migration: không.
- Kiểm thử: kiểm thử biên dịch + thao tác thủ công; logic thuần không đổi.
- Offline: không.

### 5.5. Sổ từ không sắp xếp được

- Sửa: `LibrarySorting.kt` + `LibrarySort` (A→Z, Mới thêm, Đến hạn trước) và hàng chip trong tab Sổ từ.
- Migration: không.
- Kiểm thử: `LibrarySortingTest` (3 ca).

## 6. Không đưa vào Vocab (và lý do)

| Tính năng | Lý do |
|---|---|
| Từ điển trực tuyến, tra cứu `dictionary_v2` | Cần mạng và dữ liệu từ điển lớn |
| Bảng xếp hạng, bạn bè, cộng đồng, sự kiện | Cần backend, tài khoản; trái yêu cầu offline |
| Thanh toán, gói Premium | Không có mục đích trong app cá nhân |
| Vườn cây, vật phẩm, nhiệm vụ theo mùa | Gắn với hệ thống server/phần thưởng |
| Nạp sẵn `learn_vocab_data.json` hoặc từ điển mẫu | Yêu cầu: người dùng tự nhập/import |
| Âm thanh, Lottie, font của MochiVocab | Tài sản có bản quyền; Vocab dùng TTS và Material 3 |

## 7. Cách kiểm chứng lại

```powershell
# 1) Metadata APK
aapt2 dump badging  D:\Dự án\Vocab-1.1.5-debug.apk
aapt2 dump permissions D:\Dự án\Vocab-1.1.5-debug.apk   # phải KHÔNG có android.permission.INTERNET

# 2) Đơn vị
gradle test

# 3) Build
gradle assembleDebug
```

Kết quả thực tế của lần chạy được ghi trong `docs/release-1.1.5.md`.
