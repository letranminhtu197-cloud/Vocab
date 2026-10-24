# Giai đoạn 1 — Phân tích Luyện Từ 1.4.0

Ngày phân tích: 2026-10-24  
Nguồn: gói `LuyenTu.zip` người dùng cung cấp, chứa bản APK đã giải mã tài nguyên/DEX của `com.luyentu.mobile` (versionName 1.4.0, versionCode 82). Đây **không phải** mã nguồn Flutter/Dart gốc.

## 1. Phạm vi và độ tin cậy

Đã kiểm tra cấu trúc ZIP, manifest Android, tài nguyên strings, tên thành phần Android, asset và các chuỗi định danh còn lại trong bản biên dịch. Tên feature và class dưới đây là dấu vết quan sát được; chúng không chứng minh toàn bộ thuật toán hoặc UI đã được phục hồi. Flutter AOT nằm trong `libapp.so`, vì vậy không được giả định đã có mã Dart gốc để sao chép nguyên trạng.

## 2. Thông tin nền tảng quan sát được

- Application ID: `com.luyentu.mobile`.
- Version: 1.4.0 (code 82).
- Android min SDK 24; target SDK 36.
- Ứng dụng dùng Flutter; có các split APK và native libraries.
- Manifest có quyền `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, quyền billing và các thành phần Firebase Messaging.
- Có 3 widget Android: thẻ từ, chuỗi ngày học và ảnh đại diện.
- Asset gồm dữ liệu từ tiếng Anh, hình ảnh/mascot, animation Lottie, âm thanh, đồ họa chủ đề và tài nguyên trò chơi.

Các quyền mạng, Firebase và billing là dấu hiệu ứng dụng tham chiếu có thành phần trực tuyến/thương mại. Bản Vocab mới **không** được mang theo các phụ thuộc này.

## 3. Nhóm tính năng nhận diện từ tên feature / class / asset

| Nhóm | Bằng chứng quan sát | Hướng tái triển khai offline |
|---|---|---|
| Từ vựng và danh mục | `features/words`, `features/categories`, bộ dữ liệu `english_words.txt` | CRUD từ, danh mục, tag, tìm kiếm/lọc, nhập từ tệp do người dùng chọn; không ép cài sẵn kho từ lớn |
| Phiên học và luyện tập | `features/practice`; tên loại Flashcard, Listening, Matching, Typing, Quiz, Mixed, Sentence, Translation, Monkey Rescue, Flappy | Hệ thống phiên học dùng chung và nhiều dạng bài; game là module tùy chọn, không chặn chức năng cốt lõi |
| Lặp lại ngắt quãng (SRS) | `practice_srs_api.dart`, `PracticeSrsWord`, `PracticeSrsOverview`, `SrsSessionSettings`, `nextReviewAt`, các mức level0–level5 | Bộ lập lịch SRS chạy cục bộ, lưu lịch sử từng từ, từ đến hạn và thống kê; cần định nghĩa thuật toán rõ ràng và kiểm thử riêng |
| Lộ trình học | `features/roadmap`, `RoadmapSrsSettingsStore`, `RoadmapSrsDistributionChart` | Lộ trình theo mục tiêu/ngày, phân bổ từ mới và ôn tập, biểu đồ tiến độ |
| Trang chủ và tiến độ | `features/home`, streak-related resources | Dashboard hôm nay, số từ đã học/đến hạn, chuỗi ngày và lịch hoạt động |
| Widget màn hình chính | `WordWidgetProvider`, `StreakWidgetProvider`, `AvatarWidgetProvider` | Widget Android đọc dữ liệu cục bộ; cập nhật an toàn khi app/DB thay đổi |
| Thông báo | `features/notifications`, receiver lập lịch thông báo và boot | Nhắc học cục bộ, cấu hình được, phục hồi lịch sau khởi động; tuân thủ quyền Android |
| Phát âm / nghe | `features/speech`, intent TTS, âm thanh đúng/sai | Android TTS chỉ khi voice khả dụng ngoại tuyến; ghi âm/nhận dạng giọng nói chỉ dùng engine on-device được xác nhận, không âm thầm gửi dữ liệu ra mạng |
| Hồ sơ / mascot / giao diện | `features/profile`, mascot SVG, theme art | Hồ sơ, avatar/mascot, theme cục bộ; tài nguyên mới phải có quyền sử dụng rõ ràng |
| Tính năng trực tuyến | `features/auth`, `global_chat`, `leaderboard`, `classes`, `shared_streak`, Firebase identifiers | Không triển khai cloud login/chat/leaderboard/lớp học đồng bộ. Có thể thay bằng mục tiêu và thống kê cá nhân trên thiết bị |

## 4. Dạng bài học quan sát được

Các tên sau xuất hiện trong chuỗi đã biên dịch: Flashcard, Listening, Matching, Typing, Quiz, Mixed, Sentence, Translation, Monkey Rescue, Flappy. Đây là danh sách dạng bài có bằng chứng tên gọi, không phải xác nhận rằng mọi dạng bài hoạt động độc lập hoặc có thể chạy offline trong APK tham chiếu.

Bản Vocab cần ưu tiên theo thứ tự:
1. Flashcard để giới thiệu từ mới và xem nghĩa/phiên âm/ví dụ.
2. Trắc nghiệm nhận diện nghĩa.
3. Gõ từ theo gợi ý/nghĩa.
4. Điền từ vào ngữ cảnh.
5. Nghe và gõ — chỉ bật nếu TTS offline dùng được.
6. Ghép cặp, dịch câu, nghe/chọn đáp án và phiên trộn.
7. Game học từ là module phụ; không thay thế việc ôn SRS.

## 5. Widget từ — hành vi có thể xác nhận

`WordWidgetProvider` có action identifiers FLIP, MOVE, TOGGLE_TRANSLATION, SPEAK, ANSWER và RESET. Các khóa SharedPreferences gồm `lt_word_widget_payload`, `lt_word_widget_reserve`, `lt_word_widget_payload_state`, `lt_widget_answer_queue`; queue câu trả lời là JSON có word id/result/path/time và được giới hạn 200 phần tử.

Kết luận thiết kế: widget Vocab nên là client mỏng của kho dữ liệu cục bộ, chỉ hiển thị/ghi sự kiện qua repository an toàn; không coi SharedPreferences payload là nguồn dữ liệu chính. Mọi câu trả lời từ widget phải được xử lý idempotent để tránh cộng tiến độ hai lần.

## 6. Tài nguyên dữ liệu

APK có `assets/data/english_words.txt` (khoảng 38 nghìn dòng theo phép đếm sơ bộ). Không sao chép mặc định toàn bộ danh sách vào Vocab: người dùng muốn tự nhập bộ từ của mình và không cần kho từ dựng sẵn, ngoài một bộ dữ liệu demo nhỏ có thể xóa. Tài nguyên hình ảnh, mascot, âm thanh và game trong APK tham chiếu không được sao chép nguyên xi nếu chưa xác định quyền sử dụng; ưu tiên tự thiết kế hoặc dùng tài nguyên có giấy phép phù hợp.

## 7. Những điều chưa thể kết luận từ gói giải mã

- Không có mã Dart gốc có cấu trúc; không thể tuyên bố đã phục hồi toàn bộ logic.
- Không thể xác nhận chính xác thuật toán SRS, trọng số điểm, mọi màn hình, hay các quy tắc premium chỉ từ tên class/chuỗi.
- Không thể khẳng định các dạng nghe/nhận dạng giọng nói đều ngoại tuyến.
- Không nên phụ thuộc backend, API, Firebase, đăng nhập, thanh toán hay dữ liệu từ xa của app tham chiếu.

## 8. Tiêu chí chấp nhận cho giai đoạn 1

- [x] Ghi rõ nguồn và giới hạn bằng chứng.
- [x] Phân nhóm tính năng có dấu vết trong APK.
- [x] Phân biệt tính năng cốt lõi, widget, hệ thống học và tính năng trực tuyến.
- [x] Xác định các tính năng cần thay thế/loại bỏ để đáp ứng yêu cầu miễn phí và offline.
- [x] Ghi lại rủi ro giấy phép và việc không thể khôi phục mã Dart gốc.
