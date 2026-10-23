# Phân tích nguồn MochiVocab và kế hoạch nâng cấp Vocab (1.1.11)

Ngày 2026-10-23. Mục tiêu: đọc các nguồn người dùng gửi, rút ra tính năng thật của MochiVocab, rồi chuyển
thành nâng cấp cụ thể cho Vocab.

## 1. Nguồn đã đọc được gì

| Nguồn | Kết quả |
|---|---|
| 3 video YouTube (`LBQri2ga4TE`, `JkwuEzNPHO0`, `NxkHk8BlEas`) | Chỉ lấy được **tiêu đề** qua YouTube oEmbed: "MochiVocab hoạt động như thế nào? – Main Features", "Cùng Mochi TẠO THÓI QUEN học tiếng Anh…", "Giới thiệu App học từ vựng MochiMochi". **Không có phụ đề/khung hình** nên không phân tích nội dung video. |
| TikTok `@vgghaa/video/7595435017334033671` | oEmbed trả về đúng video nhưng **không có tiêu đề/mô tả**; nội dung video không truy cập được. |
| Blog `mochidemy.com/blog/spaced-repetition/` | Đọc đầy đủ: giải thích spaced repetition, Leitner 3–5 hộp, mô tả MochiVocab có "thời điểm vàng", chia 5 cấp độ, flashcard có âm thanh/hình ảnh/ví dụ. |
| Blog `mochidemy.com/blog/mochimochi-tinh-nang/` | Đọc đầy đủ: danh sách 13 tính năng (thời điểm vàng, 5 cấp, flashcard, sổ tay bỏ tick, streak, Mochi Rank, Mochi Garden, IELTS Zone, MochiHub, cộng đồng, thành tích, tra 70.000 từ, cài đặt/tài khoản). |
| Blog `mochidemy.com/blog/mochimochi-tu-vung-tieng-anh/` | Đọc đầy đủ: nhấn mạnh spaced repetition, 5 cấp độ, kho 8.000 từ với nhiều khóa học, flashcard có ảnh + audio + ví dụ. |

Kết luận: phần **học** của MochiVocab được mô tả rõ trong blog; phần video không phân tích được. Vì vậy
kế hoạch dưới đây dựa trên blog + những gì đã kiểm chứng trong APK/`libapp.so` ở các bước trước.

## 2. Tính năng MochiVocab (theo blog) → trạng thái trong Vocab

| # | Tính năng (nguồn blog) | Vocab hiện tại | Việc cần làm |
|---|---|---|---|
| 1 | **Thời điểm vàng**: tính lúc sắp quên và nhắc | Có nhắc 19:00 cố định + nhãn "thời điểm vàng" cho từng từ | Nâng cấp: nhắc theo từ đến hạn gần nhất, không chỉ 19:00 |
| 2 | **5 cấp độ ghi nhớ** (Leitner) | SRS 5 cấp 4h/24h/72h/168h/720h | **1.1.11:** thêm biểu đồ phân bố 5 cấp |
| 3 | Flashcard: nghĩa, IPA, **phát âm thường + phát âm chậm**, ví dụ, **dịch câu ví dụ**, ảnh | Có nghĩa, IPA, TTS thường, ví dụ; chưa có dịch câu ví dụ, chưa có ảnh | **1.1.11:** thêm nút **Nghe chậm** (TTS 0.6×). Dịch câu ví dụ: cần cột DB mới (xem §3) |
| 4 | **Sổ tay bỏ tick**: tạm dừng ôn một từ | Chưa có | Cần cột `paused` + migration (xem §3) |
| 5 | **Streak** | Có (đếm ngày hoạt động) | Đủ dùng |
| 6 | **Mochi Rank** (bảng xếp hạng) | Không có | **Không làm** — cần server/tài khoản |
| 7 | **Mochi Garden** (trồng cây theo mục tiêu) | Không có | **Không làm** — gắn server/phần thưởng; có thể thay bằng biểu đồ tiến độ thuần nội bộ sau |
| 8 | **IELTS Zone** | Quiz/lộ trình theo chủ đề IELTS | Đủ ý tưởng, có thể thêm bộ câu hỏi đọc hiểu sau |
| 9 | **MochiHub** (tin tức/ưu đãi) | Không có | **Không làm** — nội dung từ server |
| 10 | **Cộng đồng** | Không có | **Không làm** — cần mạng |
| 11 | **Thành tích** | 6 huy hiệu cục bộ | Đủ dùng |
| 12 | **Tra 70.000 từ** | Chỉ tìm trong sổ từ của bạn | **Không làm** — cần dữ liệu từ điển lớn, trái yêu cầu "không nạp sẵn" |
| 13 | **Khóa học 8.000 từ** | Khóa khởi đầu 60 từ + import hàng loạt | Thêm gói khóa học JSON tự tạo (xem §3) |
| 14 | Cài đặt âm thanh/ngôn ngữ | Có dark mode, giọng TTS | Đủ dùng |

## 3. Đã làm trong 1.1.11

1. **Phát âm chậm 0.6×** (`OfflineSpeakButton` thêm `allowSlow`): dùng trong thẻ bài học và chi tiết từ —
   tương ứng cặp "phát âm thường / phát âm chậm" của flashcard MochiVocab.
2. **Biểu đồ phân bố 5 cấp ghi nhớ** (`LevelDistribution` + `LevelDistributionChart`): hiển thị ở tab
   Thống kê; mục tiêu là thấy tỷ lệ từ đã lên cấp 4–5, đúng cách blog mô tả "biểu đồ 5 cấp lý tưởng".
   Có 6 unit test cho phần đếm/tỷ lệ.

## 4. Nâng cấp tiếp theo (cần migration hoặc nhiều thời gian hơn)

| Ưu tiên | Việc | Thay đổi dữ liệu |
|---|---|---|
| 1 | **Dịch câu ví dụ** (`exampleMeaning`): hiển thị dưới câu ví dụ, thêm vào dialog sửa từ và CSV | Migration Room 3 → 4, thêm cột TEXT |
| 2 | **Tạm dừng ôn một từ** (`paused`): bỏ khỏi hàng đợi đến hạn/thử thách nhưng giữ dữ liệu | Migration 3 → 5 (chung đợt với #1) |
| 3 | **Nhắc "thời điểm vàng" theo từ**: tính `nextReviewAtMillis` sớm nhất rồi đặt alarm đúng giờ đó | Không đổi DB; cần quyền alarm chính xác (đã có `USE_EXACT_ALARM`) |
| 4 | **Gói khóa học JSON**: file gồm nhiều bài, mỗi bài nhiều từ, import offline | Không đổi DB; chỉ thêm parser + màn hình |
| 5 | **Bài tập khó dần theo cấp độ** (cấp thấp: chọn nghĩa; cấp cao: gõ từ/viết câu) | Không đổi DB; dùng `srsLevel` có sẵn |

## 5. Không làm trong Vocab

- Không sao chép giao diện, mascot, hình ảnh, âm thanh, font, câu chữ hay nội dung khóa học của MochiVocab.
- Không bẻ khóa, không bỏ giới hạn trả phí, không giả lập tài khoản.
- Không thêm bảng xếp hạng, cộng đồng, hub, thanh toán, đồng bộ đám mây: cần mạng và tài khoản.

## 6. Điều chỉnh so với bản 1.1.10

- Không thay đổi schema, không thêm quyền; giữ 100% offline.
- Thêm 6 unit test (`LevelDistributionTest`) và hai thành phần giao diện mới.
