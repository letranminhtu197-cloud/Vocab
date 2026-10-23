# Vocab 1.2.0 — IELTS Vocabulary Offline

**Phiên bản ứng dụng:** `1.2.0` (`versionCode 125`). Đây là bản phát hành cá nhân ưu tiên ngoại tuyến; APK CI hiện là **debug-signed** để cài thử trực tiếp, chưa phải APK phát hành thương mại ký bằng khóa riêng.

Ứng dụng Android cá nhân học từ vựng IELTS Academic, giao diện tiếng Việt, thiết kế cho dùng ngoại tuyến trên điện thoại Android. Ứng dụng không tự cài sẵn danh sách từ vựng; người dùng tự nhập từ, nhập CSV, hoặc chủ động thêm ba từ mẫu chỉ để thử tính năng.

## Đã triển khai
- Kotlin, Jetpack Compose, Material 3.
- Room database cục bộ: từ vựng và tiến độ vẫn còn sau khi đóng/mở ứng dụng.
- Thêm, sửa, xóa, tìm kiếm từ; flashcard; thống kê cơ bản. Mỗi từ hỗ trợ IPA, từ loại, nghĩa, ví dụ, chủ đề IELTS, collocations, từ đồng nghĩa, word family, ghi chú cá nhân và tags. Khi thêm/sửa sẽ chặn từ trùng không phân biệt chữ hoa/thường; thao tác xóa cần xác nhận và tìm kiếm có trạng thái rỗng rõ ràng.
- Ôn SRS 5 cấp, đánh giá Lại/Khó/Tốt/Dễ; lịch cơ sở lần lượt 1, 3, 7, 14 và 30 ngày. “Khó” lên lịch bằng một nửa khoảng của cấp mục tiêu (tối thiểu 1 giờ); “Lại” đặt ôn lại sau 10 phút và ghi nhận lần quên. Tiến độ/lịch đã lưu được giữ nguyên; lịch mới được áp dụng khi đánh giá từ tiếp theo.
- Kết quả của các dạng quiz cũng cập nhật SRS cho đúng từ đang luyện, không chỉ tăng thống kê tổng.
- Hiển thị trạng thái SRS: từ mới, sắp đến hạn, thời điểm vàng hoặc quá hạn.
- Lưu cục bộ tổng số câu quiz, tỷ lệ đúng và điểm/số câu của phiên quiz hiện tại qua các lần mở ứng dụng; có thể đặt lại bộ đếm phiên mà không xóa thống kê tổng.
- Thống kê hoạt động học theo từng ngày trong 7 ngày gần nhất và chuỗi ngày học liên tiếp; ghi nhận lượt trả lời quiz và lượt ôn SRS, hoàn toàn cục bộ.
- Thành tựu cục bộ được tính từ sổ từ, lượt ôn, chuỗi ngày và mục tiêu ngày; có tiến độ/mốc mở khóa mà không cần tài khoản, mạng hoặc bảng dữ liệu thành tựu riêng.
- Trang chủ tập trung vào việc cần làm tiếp theo, số từ đến hạn và mục tiêu ngày; flashcard, lộ trình, quiz và ghép cặp nằm cùng khu Học. Thanh điều hướng rút còn bốn khu: Hôm nay, Học, Sổ từ, Tiến độ.
- Sổ từ ưu tiên xem nhanh từ, nghĩa và trạng thái ôn; phần IPA, ví dụ, collocations, từ đồng nghĩa, họ từ, ghi chú và tags mở trong chi tiết từ.
- Tiến độ nhóm các chỉ số chính, biểu đồ 5 cấp nhớ, hoạt động 7 ngày, tiến độ chủ đề, từ cần ưu tiên và thành tựu.
- Bài ghép từ với nghĩa theo nhóm 4 cặp dùng dữ liệu của người học, chấm một lượt cho mỗi từ và cập nhật SRS; sai một cặp sẽ được thử lại trước khi ghi nhận kết quả.
- Các dạng luyện: chọn nghĩa, chọn từ theo nghĩa, tự gõ từ tiếng Anh từ nghĩa tiếng Việt, **nghe phát âm offline rồi gõ từ**, **nói từ bằng nhận diện giọng nói ngoại tuyến trên thiết bị**, điền từ còn thiếu trong câu ví dụ, viết câu với từ mục tiêu, nhận diện nhóm collocations/từ đồng nghĩa/họ từ đã nhập và ôn từ yếu.
- Luyện tập có thể giới hạn theo chủ đề IELTS tự nhập; không tự tạo hoặc tải bộ từ vựng.
- Chấm đáp án tức thì với các dạng có đáp án xác định; dạng viết câu yêu cầu người học tự đánh giá và lưu kết quả tự đánh giá vào SRS. Đây không phải hệ thống chấm ngữ pháp tự động.
- Quiz trắc nghiệm loại các lựa chọn có nội dung hiển thị trùng nhau, bỏ lựa chọn nhiễu rỗng và luôn giữ đáp án mục tiêu; nếu dữ liệu người dùng chưa đủ hai lựa chọn khác nhau, ứng dụng giải thích cách bổ sung dữ liệu thay vì tạo câu hỏi vô nghĩa.
- Các chế độ trắc nghiệm hiển thị vị trí câu hiện tại và thanh tiến độ trong phạm vi từ đang luyện.
- Có chế độ liên kết từ với nhóm collocations và word family đã lưu trong từng mục. Đây là bài nhận diện/ghi nhớ dữ liệu do người dùng nhập, không tự suy diễn hoặc tạo họ từ nếu chưa có dữ liệu.
- Chế độ ôn riêng các từ mới/đến hạn, tách khỏi chế độ xem toàn bộ sổ từ; khu Học có flashcard, lộ trình, quiz và ghép cặp.
- Thống kê có tiến độ theo chủ đề và danh sách tối đa 5 từ cần ưu tiên dựa trên số lần quên.
- Khi sổ từ còn trống, màn hình Sổ từ hướng dẫn thêm từ hoặc nhập file; có thể dùng khóa khởi đầu tự soạn hoặc thêm từ mẫu tùy chọn.
- Lộ trình học theo chủ đề do người dùng nhập: tổng quan tiến độ, phần trăm từ đã ôn, số từ đến hạn và từ yếu theo từng chủ đề; mỗi chặng mở bài học giới hạn đúng chủ đề đó. Từ không có chủ đề được gom riêng.
- Tùy chọn mục tiêu 5/10/15/20 từ mỗi phiên học; lựa chọn được lưu cục bộ và áp dụng cho phiên học kế tiếp. Nếu chủ đề có ít từ hơn mục tiêu, phiên chỉ dùng số từ hiện có.
- Phiên bài học có thể đặt mục tiêu 5/10/15/20 từ, ưu tiên từ chưa ôn; người học tự đánh giá nhớ/chưa nhớ để cập nhật tiến độ SRS.
- Trong phiên bài học, từ được đánh giá “Lại” sẽ được đưa về cuối phiên để thử nhớ lại thêm một lượt; mỗi từ chỉ được tự động đưa vào lượt củng cố một lần trong phiên, tránh lặp vô hạn.
- Sao lưu/khôi phục ZIP cục bộ có phiên bản định dạng, bảo toàn nội dung và tiến độ SRS; khi khôi phục, có màn hình xem trước và chỉ thêm từ chưa có, không ghi đè từ đang có. File khôi phục bị từ chối nếu thiếu mục bắt buộc, lặp mục dữ liệu, sai phiên bản hoặc chứa UTF-8 lỗi; giới hạn kích thước nén và giải nén đều được áp dụng.
- CSV và ZIP giữ thời điểm tạo từ, lần ôn gần nhất, metadata SRS, câu dịch, trạng thái tạm dừng, collocations, từ đồng nghĩa, word family, ghi chú và tags. Cơ sở dữ liệu có migration tuần tự đến schema 4, giữ nguyên dữ liệu từ đã lưu.
- Nhắc ôn dùng mốc đến hạn gần nhất thay cho giờ cố định; có công tắc bật/tắt, xin quyền thông báo theo Android và tự đăng ký lại sau khi khởi động máy. Android có thể dời thời điểm một chút để tiết kiệm pin.
- Phát âm từ qua Text-to-Speech chỉ khi thiết bị có giọng tiếng Anh được đánh dấu là không cần mạng; nếu không có, ứng dụng hướng dẫn cài giọng offline và không gọi giọng mạng.
- Chế độ Nói từ khai báo khả năng nhìn thấy Android `RecognitionService` trên Android 11+ và chỉ dùng `SpeechRecognizer.createOnDeviceSpeechRecognizer` khi Android báo có hỗ trợ. Quyền microphone chỉ được hỏi khi người dùng chủ động bật chế độ này; nếu thiết bị thiếu bộ nhận diện/ gói ngôn ngữ offline, app báo rõ và không chuyển sang nhận diện mạng. Kết quả nhận diện không phải điểm chấm phát âm hay trọng âm.
- Nhập/xuất CSV qua Android file picker (Storage Access Framework); không cần quyền truy cập toàn bộ bộ nhớ. CSV giới hạn 20 MB, kiểm tra UTF-8 nghiêm ngặt và có màn hình xem trước số từ mới/trùng/dòng lỗi trước khi xác nhận nhập.
- Nhập CSV yêu cầu cột `term` (hoặc `word`) và `meaning` (hoặc `nghĩa`). Các cột `ipa`/`phonetic`, `partOfSpeech`/`pos`, `example`, `topic`, `collocations`/`collocation`, `synonyms`/`synonym`, `wordFamily`, `notes`, `tags` tùy chọn. Có thể thêm các cột SRS trong mẫu để giữ tiến độ khi xuất/nhập lại.
- Từ trùng được bỏ qua; dòng thiếu từ hoặc nghĩa được báo số lượng. CSV hỗ trợ trường đặt trong dấu ngoặc kép, dấu phẩy và xuống dòng bên trong trường; các dấu ngoặc kép đặt sai vị trí hoặc ký tự thừa sau dấu ngoặc kép đóng sẽ bị từ chối với thông báo lỗi. Metadata SRS có giá trị số không hợp lệ, cấp SRS ngoài 0–5 hoặc thời điểm âm sẽ khiến toàn bộ lượt nhập bị từ chối thay vì âm thầm đặt tiến độ về 0.
- Không khai báo quyền `INTERNET`; không gọi dịch vụ mạng trong ứng dụng. Quyền thông báo, nhận sự kiện khởi động và microphone (chỉ xin khi chủ động dùng chế độ Nói từ) phục vụ các chức năng cục bộ.
- Bấm vào một từ trong sổ từ mở **chi tiết từ**: đầy đủ IPA, từ loại, nghĩa, ví dụ, chủ đề, collocations, từ đồng nghĩa, họ từ, thẻ, ghi chú, cấp SRS, số lượt ôn/quên và lịch ôn kế tiếp; từ đó có thể **Luyện từ này** ngay ở chế độ gõ từ, hoặc sửa/xóa.
- Sổ từ có sắp xếp A → Z, Mới thêm hoặc Đến hạn trước, kết hợp với bộ lọc và tìm kiếm hiện có.
- Trạng thái “đến hạn” và “từ yếu” được định nghĩa một lần trong `WordStatus` và dùng chung cho Tổng quan, Sổ từ, Luyện tập, Lộ trình, Thống kê — số liệu giữa các màn hình khớp nhau.
- Ghi kết quả ôn trong một transaction đọc lại bản ghi mới nhất (`ReviewStore`), nên các lượt trả lời sát nhau không ghi đè lẫn nhau hay làm mất lượt ôn.
- Chấp nhận biến thể dấu phân cách khi gõ đáp án: “well-being”, “well being” và “wellbeing” đều đúng; “don't” và “dont” đều đúng, nhưng vẫn không chấp nhận từ khác nghĩa.
- Dạng luyện **Xáo chữ**: đảo thứ tự chữ cái của từ (giữ ranh giới từ trong cụm) và cho gợi ý chữ cái đầu; dùng chung quy tắc chấm với dạng gõ từ.
- Dạng luyện **Nghe chọn nghĩa**: nghe phát âm ngoại tuyến rồi chọn nghĩa đúng trong 4 lựa chọn, không hiện từ tiếng Anh trước khi trả lời.
- **Giới hạn số từ ôn mỗi phiên** (10 / 20 / 50 / Tất cả) áp cho flashcard “Chỉ từ đến hạn” và chế độ quiz “Đến hạn”, tránh phiên ôn quá dài.
- **Thử thách từ vựng hằng ngày**: 5 từ được chọn từ chính sổ từ, giữ nguyên trong ngày và đổi mỗi ngày; hoàn thành được đánh dấu theo ngày và hiển thị lại sau khi mở ứng dụng.
- **Nhập hàng loạt nhiều định dạng**: CSV có tiêu đề (term/meaning), TSV dán từ Excel/Google Sheets, CSV kiểu châu Âu (`;`), hoặc TXT mỗi dòng `từ - nghĩa` (có thể thêm `- câu ví dụ`), `từ: nghĩa`, `từ | nghĩa`. Nhập 1 file là thêm được nhiều từ cùng lúc; có xem trước số từ mới/trùng/dòng lỗi trước khi xác nhận.
- **Khóa khởi đầu IELTS 60 từ** (6 chủ đề, do Vocab tự soạn): thêm bằng một nút bấm, không cần tài khoản, không cần mạng; sau đó có thể sửa/xóa từng từ. Đây không phải dữ liệu trích từ ứng dụng khác.

## File CSV mẫu
Tải mẫu rỗng tại [examples/vocab-template.csv](examples/vocab-template.csv). Ví dụ tối thiểu:

```csv
term,meaning
allocate,phân bổ
sustainable,bền vững
```

Có thể chỉ dùng hai cột `term,meaning`; các trường còn lại để trống hoặc bỏ qua. Nên lưu file dưới dạng CSV UTF-8. File có byte UTF-8 lỗi sẽ bị từ chối thay vì âm thầm thay ký tự.

## Cải tiến học tập và trải nghiệm ở bản 1.2.0
- Tổ chức lại điều hướng thành bốn khu, gom flashcard, lộ trình, quiz và ghép cặp vào khu Học.
- Trang Hôm nay chỉ ưu tiên phiên tiếp theo, tiến độ mục tiêu và thử thách trong ngày; chọn mục tiêu và giới hạn phiên được thu gọn khi không cần chỉnh.
- Thẻ trong Sổ từ rút gọn còn từ, nghĩa, chủ đề và trạng thái ôn; mở chi tiết để xem toàn bộ ví dụ và ghi chú.
- Trang Tiến độ nhóm các số liệu vào một bảng tổng quan; phần nhắc học chuyển về khu này.
- Thêm trò chơi ghép từ–nghĩa ngoại tuyến theo chủ đề; kết quả mỗi từ vẫn được ghi vào SRS.

## Cải tiến học tập và trải nghiệm ở bản 1.1.14
- **Nhập gói khóa học JSON**: một tệp nhiều bài, mỗi bài nhiều từ, kèm ví dụ/dịch ví dụ/collocations; hộp xác nhận hiển thị tên khóa, số bài, số từ mới/trùng/lỗi.
- Bộ đọc JSON tối giản tự viết (`SimpleJson`), không thêm thư viện; nhận cả tên trường tiếng Anh và tiếng Việt, chấp nhận danh sách phẳng hoặc array gốc.
- Tệp mẫu: `docs/course-pack-example.json`.

## Cải tiến học tập và trải nghiệm ở bản 1.1.13
- **Tạm dừng ôn một từ** (như bỏ tick sổ tay Mochi): từ vẫn giữ trong sổ và giữ cấp SRS nhưng không vào hàng đợi ôn, quiz, thử thách ngày hay nhắc thời điểm vàng. Bật/tắt trong sửa từ hoặc chi tiết từ.
- **Dịch nghĩa câu ví dụ**: thêm trường trong sửa từ, hiển thị trong thẻ học và chi tiết từ; CSV/backup giữ trường mới.
- **Migration Room 3 → 4** thêm hai cột `exampleMeaning`, `paused`; dữ liệu cũ giữ nguyên. Nên sao lưu ZIP trước khi cập nhật.

## Cải tiến học tập và trải nghiệm ở bản 1.1.12
- **Thời điểm vàng theo lịch ôn thật**: nhắc ở mốc gần nhất sắp tới thay vì cố định 19:00.
- Thông báo hiển thị số từ đang đến hạn thực tế; mở app xong alarm được tính lại cho mốc kế tiếp.
- Trang chủ có thẻ "⏰ Thời điểm vàng" (từ nên ôn + thời gian) kèm nút Ôn ngay.
- Sau khi khởi động máy, lịch nhắc được dựng lại từ sổ từ.
- Không đổi schema/quyền; alarm dùng `setAndAllowWhileIdle` nên có thể trễ vài phút khi tiết kiệm pin.

## Cải tiến học tập và trải nghiệm ở bản 1.1.11
- Thêm **Nghe chậm** (TTS 0.6×) cạnh nút nghe thường trong thẻ bài học và chi tiết từ — tương ứng cặp phát âm thường/chậm của flashcard MochiVocab.
- Thêm **biểu đồ phân bố 5 cấp ghi nhớ** ở Thống kê, kèm tỷ lệ từ "đã nhớ tốt" (cấp 4–5).
- Phân tích nguồn tham khảo (blog mochidemy) và kế hoạch nâng cấp nằm ở `docs/mochivocab-upgrade-1.1.11.md`.

## Cải tiến học tập và trải nghiệm ở bản 1.1.10
- Trang chủ mở bằng hành động học; nhắc học chuyển xuống cuối để không chiếm chỗ.
- Sổ từ gọn hơn: số từ + menu **Sắp xếp** + nút **Dữ liệu** (nhập/xuất/sao lưu mở khi cần).
- Luyện tập chỉ hiện 4 chế độ dùng nhiều nhất; mở "Tất cả chế độ luyện tập" khi cần 10 chế độ còn lại.
- Không đổi dữ liệu, schema hay quyền.

## Cải tiến học tập và trải nghiệm ở bản 1.1.9
- Bộ thiết kế mới theo phong cách app học ngoại ngữ: nút dày có cạnh dưới, thanh tiến độ chia đoạn, bảng màu tươi (xanh lá/vàng/xanh dương).
- Nút CTA trang chủ, nút thử thách, nút lật thẻ, 4 nút đánh giá và nút chuyển câu đều dùng `VocabButton` (lún khi nhấn).
- Phiên học và trắc nghiệm dùng `SegmentProgressBar` để thấy tiến độ theo từng chặng.
- Toàn bộ hình khối vẽ bằng Compose; không thêm ảnh, font hay thư viện.

## Cải tiến học tập và trải nghiệm ở bản 1.1.8
- Giao diện "gamified" thiết kế riêng: bảng màu mint/vàng/hồng, thẻ bo tròn lớn, hỗ trợ dark mode (`AppTheme.kt`).
- Trang chủ có vòng tiến độ tròn cho mục tiêu ngày và nhãn chuỗi ngày học.
- Phiên bài học có dãy chấm tiến độ từng từ, nút đánh giá Lại/Khó/Tốt/Dễ phân màu, giữ nguyên khoảng thời gian ôn.
- Hoàn thành phiên có hiệu ứng giấy màu vẽ bằng Canvas; trắc nghiệm có nhãn A/B/C/D.
- Không thêm ảnh, font, âm thanh hay thư viện mới — toàn bộ hình vẽ bằng Compose.

## Cải tiến học tập và trải nghiệm ở bản 1.1.7
- Nhập hàng loạt từ nhiều định dạng văn bản (CSV/TSV/TXT) — dán từ Excel, Google Sheets hoặc file ghi chú là có ngay nhiều từ.
- Bộ đọc file tự nhận dạng dấu phân cách (`,` `;` tab) và tự nhận biết có tiêu đề hay không; dòng bắt đầu bằng `#` hoặc `//` được bỏ qua như ghi chú.
- Khóa khởi đầu IELTS 60 từ / 6 chủ đề soạn riêng cho Vocab, thêm bằng một nút bấm; học được ngay mà không cần đăng nhập, không cần mạng.
- Thêm `ImportCodecTest` (8 ca) và `StarterCourseTest` (4 ca); tổng 93 unit test.

## Cải tiến học tập và trải nghiệm ở bản 1.1.6
- Bốn dạng/tiện ích mới tái tạo offline từ nhóm tính năng MochiVocab: Xáo chữ, Nghe chọn nghĩa, giới hạn từ ôn mỗi phiên và thử thách từ vựng hằng ngày.
- Thử thách ngày dùng bộ từ cố định theo ngày (cùng một ngày luôn ra cùng bộ) và ưu tiên từ đến hạn/từ yếu.
- Bộ chọn giới hạn ôn được lưu cục bộ, áp ngay cho phiên kế tiếp.
- Bổ sung `ScrambleLogicTest` (6 ca) và `DailyChallengeLogicTest` (6 ca); tổng 81 unit test.

## Cải tiến học tập và trải nghiệm ở bản 1.1.5
- Thẻ trong phiên bài học hiển thị **lý do ưu tiên** (Từ mới, Cần củng cố, Từ yếu, Đến hạn ôn, Ôn duy trì) để người học hiểu vì sao từ xuất hiện.
- Chi tiết từ cho thấy toàn bộ dữ liệu tự nhập cùng trạng thái ghi nhớ, thay vì phải đọc trong thẻ danh sách dài.
- Định nghĩa trạng thái từ được hợp nhất; sửa lệch số liệu giữa các màn hình.
- Ghi SRS theo transaction, tránh mất lượt ôn khi trả lời nhanh liên tiếp.
- So khớp đáp án gõ tay linh hoạt hơn với gạch nối, khoảng trắng và dấu nháy.
- Bổ sung unit tests cho `WordStatus`, `LibrarySorting`, lý do ưu tiên và biến thể đáp án.

## Cải tiến học tập và trải nghiệm ở bản 1.1.4
- Bài học dùng hàng đợi hỗn hợp có ưu tiên: từ mới và đến hạn được đưa lên trước; các từ có lịch sử quên lặp lại được tăng ưu tiên, thay vì chỉ chọn từ mới rồi xáo trộn ngẫu nhiên.
- Mỗi thẻ trong bài học hiển thị lý do ưu tiên (từ mới, cần củng cố, đến hạn ôn hoặc ôn duy trì), giúp người học hiểu vì sao từ xuất hiện.
- Tất cả dạng quiz đều có chỉ báo tiến độ câu hỏi, kể cả dạng gõ từ, điền từ và viết câu.
- Màu đáp án đúng/sai dùng các màu theo bảng màu Material 3 của giao diện sáng/tối để cải thiện độ tương phản.
- Bổ sung unit tests cho thứ tự ưu tiên của hàng đợi học tập, trường hợp từ bị quên nhiều lần và thứ tự phân xử ổn định.

## Build và kiểm tra
Yêu cầu JDK 17, Android SDK Platform 35 và Gradle 8.9:

```bash
gradle test
gradle assembleDebug
```

GitHub Actions chạy unit tests, build debug APK, kiểm tra APK không có quyền Internet và tạo artifact theo phiên bản khai báo trong `app/build.gradle.kts`. Với `versionName = "1.2.0"`, artifact là `vocab-1.2.0-debug-apk` và file bên trong là `Vocab-1.2.0-debug.apk`. Mở [Actions](https://github.com/letranminhtu197-cloud/Vocab/actions), chọn lượt build thành công gần nhất và tải artifact ở cuối trang. Artifact hết hạn theo chính sách lưu trữ GitHub Actions.

## Quyền riêng tư
Dữ liệu từ vựng lưu trong cơ sở dữ liệu cục bộ của ứng dụng. Android Auto Backup bị tắt; nên dùng Sao lưu ZIP hoặc Xuất CSV định kỳ nếu muốn giữ bản sao. Không cấp quyền mạng vì ứng dụng không cần Internet để học.

## Phạm vi và lưu ý của bản 1.2.0
- Không tự nạp bộ từ vựng; bạn tự nhập từ hoặc dùng file CSV của mình.
- Phát âm dùng giọng Text-to-Speech offline có sẵn trên thiết bị; ứng dụng không đóng gói bộ âm thanh riêng. Nếu máy không có giọng tiếng Anh offline, chức năng phát âm có thể không hoạt động cho đến khi cài giọng ngoại tuyến trong cài đặt Android.
- Nhắc hằng ngày dùng cơ chế lịch của Android; thời điểm thực tế có thể trễ do chế độ tiết kiệm pin hoặc giới hạn nền của hệ điều hành.
- CI kiểm tra unit tests, tạo APK debug và xác nhận APK không yêu cầu quyền Internet. Chưa thể khẳng định mọi luồng đã được kiểm thử thủ công trên Samsung Galaxy S22 Ultra thật; trước khi coi là hoàn tất tuyệt đối, cần cài APK trên máy và kiểm tra nhập/xuất CSV, sao lưu/khôi phục ZIP, thông báo, TTS offline và toàn bộ màn hình.
