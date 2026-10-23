# Ghi chú phát hành Vocab 1.1.14

Ngày 2026-10-23. Môi trường: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35.

## Bước 4: nhập "gói khóa học" JSON (không cần migration)

Một tệp JSON có thể chứa **nhiều bài, mỗi bài nhiều từ**, kèm ví dụ/dịch ví dụ/collocations…

```json
{
  "course": "IELTS khởi đầu (ví dụ)",
  "lessons": [
    { "title": "Environment",
      "words": [
        { "term": "sustainable", "meaning": "bền vững",
          "example": "We need sustainable solutions.",
          "exampleMeaning": "Chúng ta cần các giải pháp bền vững." }
      ]
    }
  ]
}
```

Hỗ trợ thêm: `{"words": [...]}` (danh sách phẳng) và array gốc. Tên trường nhận cả tiếng Anh và
tiếng Việt (`term`/`word`/`từ`, `meaning`/`nghĩa`, `exampleMeaning`/`nghĩa câu ví dụ`, `paused`/`tạm dừng`).
Từ thiếu `term` hoặc `meaning` được đếm là dòng lỗi, không làm hỏng cả tệp.

Giao diện:
- Nút **Dữ liệu → Nhập tệp** nhận thêm `application/json`; nếu nội dung bắt đầu bằng `{` hoặc `[` thì
  dùng bộ đọc JSON, ngược lại dùng CSV/TSV/TXT như cũ.
- Hộp xác nhận hiển thị **tên khóa + số bài + số từ mới/trùng/lỗi** trước khi ghi.
- Tệp mẫu: `docs/course-pack-example.json`.

Kỹ thuật:
- `SimpleJson.kt` (mới): bộ đọc JSON tối giản (object/array/string escape/number/bool/null), không thêm
  thư viện, chạy được trên JVM để unit test.
- `CoursePackCodec.kt` (mới): chuyển JSON thành danh sách `WordEntity`, giữ `exampleMeaning`, `paused`,
  `collocations`… và gán chủ đề theo tiêu đề bài nếu từ không có `topic`.
- `MainActivity`: nhánh nhập JSON + hộp xác nhận khóa học.
- Không đổi schema, không thêm quyền, vẫn offline 100%.

## Kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **120 test, 0 failure** (21 lớp) — tăng từ 111 |
| Test mới | `SimpleJsonTest` (3 ca), `CoursePackCodecTest` (6 ca) |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK | `Vocab-1.1.14-debug.apk`, versionCode `124`, versionName `1.1.14`, 16.832.233 byte |
| SHA-256 | `279AE86476F800E2BCFC5A6325C8E67D312A9EDEE8B20F77C9ABFD6B8FB76FF9` |
| Xác nhận DEX | Có "Xác nhận nhập khóa học", "gói khóa học", "Khóa học nhập từ JSON" |
| Quyền | Không có `android.permission.INTERNET` |
| Schema | Không đổi (Room version 4) |

## Giới hạn xác minh

- Chưa push GitHub (không có credential) → chưa có lượt CI mới.
- Chưa cài trên thiết bị thật; luồng chọn file JSON mới kiểm chứng qua unit test + đọc DEX.
- JSON chỉ hỗ trợ đúng cấu trúc mô tả ở trên (không phải JSON Schema tổng quát).
