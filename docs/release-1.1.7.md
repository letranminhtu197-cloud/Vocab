# Ghi chú phát hành Vocab 1.1.7

Ngày 2026-10-23. Môi trường: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35.

## Hai thay đổi chính

### 1. Nhập hàng loạt nhiều định dạng (một file → nhiều từ)

`ImportCodec.kt` (mới) tự nhận dạng nội dung file:

| Định dạng | Ví dụ | Cách xử lý |
|---|---|---|
| CSV có tiêu đề | `term,meaning,...` | Dùng lại `CsvCodec.parse` với đầy đủ cột phụ và metadata SRS |
| TSV (dán từ Excel/Sheets) | `term<TAB>meaning<TAB>topic` | `CsvCodec.parse(text, '\t')` |
| CSV kiểu châu Âu | `term;meaning` | tự chọn dấu `;` |
| TXT mỗi dòng một từ | `allocate - phân bổ` | `ImportCodec.parsePlain` |
| TXT có ví dụ | `allocate - phân bổ - We allocate resources.` | phần thứ ba thành `example` |
| Dấu phân cách khác | `allocate: phân bổ`, `allocate | phân bổ` | hỗ trợ `:`, `|`, `=`, `,`, `;`, tab |
| Ghi chú | dòng bắt đầu `#` hoặc `//` | bỏ qua |
| CSV không tiêu đề | `allocate,phân bổ` | nhận dạng không có header → đọc theo cặp |

Vẫn giữ: giới hạn 20 MB, kiểm tra UTF-8 nghiêm ngặt, màn hình xem trước số từ mới/trùng/dòng lỗi.

### 2. Khóa khởi đầu IELTS 60 từ

`StarterCourse.kt` chứa 60 từ tiếng Anh học thuật thông dụng, 6 chủ đề (Environment, Education,
Technology, Health, Work & Economy, Society), mỗi từ có từ loại, nghĩa tiếng Việt và câu ví dụ **do dự án
tự soạn**. Thêm bằng nút "Thêm khóa khởi đầu 60 từ" ở trang chủ (khi sổ từ trống) hoặc trong Sổ từ.
Không cần tài khoản, không cần mạng; không sao chép dữ liệu từ MochiVocab hay ứng dụng nào khác.

## Kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **93 test, 0 failure** (16 lớp) — tăng từ 81 |
| Test mới | `ImportCodecTest` (8 ca), `StarterCourseTest` (4 ca) |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK | `Vocab-1.1.7-debug.apk`, versionCode `117`, versionName `1.1.7`, 16.766.693 byte |
| SHA-256 | `0FCCF1437751E94FBF1C3DFC17D1F2AFD187DB554CDD5E4216EDAA3DD06D93CD` |
| Quyền | Không có `android.permission.INTERNET` |
| Schema | Không đổi (Room version 3) |
| Chuỗi trong DEX | "Nhập CSV/TXT", "Thêm khóa khởi đầu 60 từ" |

## Không làm

- Không sửa/bẻ khóa MochiVocab, không sao chép giao diện, âm thanh, hình ảnh, nội dung khóa học của họ.
- Không thêm đăng nhập, thanh toán, xếp hạng, hub cộng đồng.

## Giới hạn xác minh

- Chưa push GitHub (không có credential) → chưa có lượt CI mới; APK do máy cục bộ build.
- Chưa cài trên thiết bị thật; mới kiểm chứng bằng unit test, biên dịch và đọc metadata/DEX của APK.
