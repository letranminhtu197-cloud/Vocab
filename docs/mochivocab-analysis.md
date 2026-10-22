# Phân tích gói MochiVocab và hướng nâng cấp Vocab

## Phạm vi và giới hạn kiểm chứng

Đã kiểm tra cấu trúc cả ba ZIP được cung cấp và đọc các tên tệp/tài nguyên có thể truy cập. Đây là APK Android đã build và mã Java được dịch ngược bằng JADX, không phải bộ mã nguồn Flutter/Dart gốc hoàn chỉnh. Không nên coi tên asset là bằng chứng tuyệt đối cho mọi luồng nghiệp vụ.

- `sources.zip`: 7.513 tệp `.java`, chủ yếu là mã Android/dependency đã dịch ngược; nhiều tên package/class bị rút gọn hoặc làm rối.
- `com.mochimochi.android.en.apk.zip`: 1.220 tệp bên trong APK, gồm Android resources, manifest, tài nguyên Flutter và các plugin.
- `resources.zip`: 98 tệp, gồm `libapp.so`, `libflutter.so`, cấu hình ngôn ngữ và tài nguyên Android. Logic Dart AOT nằm trong thư viện đã biên dịch; không thể đọc như các file Dart thuần.

## Bằng chứng tài nguyên nhìn thấy được

APK chứa khoảng 413 mục trong `assets/flutter_assets`, trong đó có:
- Tài nguyên âm thanh cho lật flashcard, âm thanh ứng dụng, phản hồi đúng/sai, mở đầu và kết thúc phiên.
- Font Quicksand và Baloo 2, nhiều icon cho từ điển, học, ôn tập, IELTS, mục tiêu ngày, streak, âm thanh, vườn/tiến độ và nhiệm vụ.
- Các animation Lottie cho onboarding, kết quả phiên, hiệu ứng và trạng thái tiến độ.
- Tệp dữ liệu `assets/json/learn_vocab_data.json` và một số tệp sự kiện. Những dữ liệu này thuộc ứng dụng gốc và không được nhập vào Vocab, vì Vocab phải bắt đầu với sổ từ do người dùng tự tạo/nhập.
- Tên tài nguyên cho các phần dictionary, learn, review, IELTS, daily task, garden, streak và audio gợi ý các khu vực sản phẩm tương ứng. Chúng là tín hiệu về thiết kế/tổ chức tính năng, không phải bằng chứng đủ để xác nhận từng thuật toán nội bộ.

## Đối chiếu với Vocab hiện tại

Vocab hiện đã có nền tảng chức năng quan trọng: Room offline, nhập/xuất CSV, sao lưu/khôi phục ZIP, flashcard, SRS bốn mức đánh giá, quiz nhiều dạng, luyện từ yếu/đến hạn, lộ trình theo chủ đề, TTS offline, thống kê và nhắc học cục bộ. Không cần viết lại toàn bộ ứng dụng hoặc chuyển framework chỉ để bắt chước một ứng dụng Flutter.

Khoảng trống ưu tiên là khả năng định hướng người học ngay từ trang chủ: tiến độ hôm nay, mục tiêu cá nhân, số từ đến hạn và lối tắt rõ ràng đến các khu vực chính. Bản nâng cấp dashboard thêm các phần này mà không cần mạng, không tự thêm từ, không sao chép mã nguồn/tài nguyên thương hiệu của MochiVocab.

## Nguyên tắc triển khai

1. Giữ Kotlin/Jetpack Compose và Room; dữ liệu tiếp tục lưu cục bộ.
2. Chỉ dùng các pattern UX phổ quát như phân cấp thị giác, tiến độ, mục tiêu và lối tắt; không sao chép asset, hình minh họa, font đóng gói hoặc nội dung độc quyền.
3. Không đưa dữ liệu mẫu MochiVocab vào ứng dụng.
4. Mỗi thay đổi cần qua unit tests và GitHub Actions trước khi gộp.
5. Những tính năng cần mạng, tài khoản hoặc backend của ứng dụng tham khảo không phù hợp với yêu cầu offline 100% và không được thêm vào Vocab.
