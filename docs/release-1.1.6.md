# Ghi chú phát hành Vocab 1.1.6

Ngày 2026-10-23. Môi trường: JDK 17, Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35.

## Bốn tính năng mới (tái tạo offline từ MochiVocab)

| Tính năng | Tương ứng bên MochiVocab | Tệp |
|---|---|---|
| **Xáo chữ** — sắp xếp lại chữ cái (giữ ranh giới từ trong cụm, gợi ý chữ đầu) | `flow_game/write_game_*` | `ScrambleLogic.kt`, `QuizScreen.kt` |
| **Nghe chọn nghĩa** — nghe TTS offline rồi chọn nghĩa đúng, không lộ từ | nhóm quiz/audio | `QuizScreen.kt` |
| **Giới hạn số từ ôn mỗi phiên** — 10 / 20 / 50 / Tất cả, áp cho flashcard đến hạn và chế độ Đến hạn | `review_word_limit` | `VocabDashboard.kt`, `MainActivity.kt` |
| **Thử thách từ vựng hằng ngày** — 5 từ chọn từ sổ từ, cố định trong ngày, đổi mỗi ngày, đánh dấu hoàn thành | `daily_vocab_challenge` | `DailyChallengeLogic.kt`, `MainActivity.kt`, `LessonScreen.kt` |

Bảng đối chiếu đầy đủ (gồm cả phần không thể/không nên sao chép): `docs/mochivocab-feature-parity.md`.

## Kết quả kiểm chứng

| Hạng mục | Kết quả |
|---|---|
| `gradle test` | **81 test, 0 failure** (14 lớp) — tăng từ 69 lên 81 |
| Test mới | `ScrambleLogicTest` (6 ca), `DailyChallengeLogicTest` (6 ca) |
| `gradle assembleDebug` | **BUILD SUCCESSFUL** |
| APK | `Vocab-1.1.6-debug.apk`, versionCode `116`, versionName `1.1.6`, 16.750.309 byte |
| SHA-256 | `7AE6F7BA39FB2E9E3F0386C0890FBD07083DBB9FB70554CB38A48012A9D8C264` |
| DEX chứa chuỗi mới | "Xáo chữ", "Nghe chọn nghĩa", "Thử thách từ vựng hôm nay", "Giới hạn số từ ôn mỗi phiên" |
| Quyền | Không có `android.permission.INTERNET` (giữ nguyên offline) |
| Schema | Không đổi (vẫn Room version 3) — không cần migration |

## Không làm và lý do

- Không sao chép mã/asset/nội dung MochiVocab: logic nằm trong Dart AOT, không có source; asset và
  `learn_vocab_data.json` thuộc bản quyền.
- Không thêm đăng nhập, thanh toán, bảng xếp hạng, bạn bè, sự kiện, hội thoại, vườn cây: cần server,
  trái yêu cầu 100% offline.
- Từ điển tra cứu ngoài sổ từ: cần dữ liệu từ điển lớn, trái yêu cầu "không nạp sẵn từ vựng".

## Giới hạn xác minh

- Chưa push GitHub (không có credential trên máy) → chưa có lượt CI mới; APK do máy cục bộ build.
- Chưa cài trên thiết bị thật; mới kiểm chứng bằng unit test, biên dịch và đọc metadata/DEX của APK.
