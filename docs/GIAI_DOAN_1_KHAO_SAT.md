# Giai đoạn 1 — Khảo sát mã nguồn LuyenTu 2.0

**Nhánh:** `luyentu2.0`  
**Mốc khảo sát:** 24/10/2026  
**Trạng thái:** Hoàn tất khảo sát tĩnh; chưa xác nhận vận hành trên điện thoại thật.

## 1. Mục tiêu và phạm vi

Giai đoạn này kiểm tra cấu trúc dự án, điểm khởi chạy, lưu trữ dữ liệu, nhập/xuất, lịch ôn tập, quyền Android và quy trình build. Không viết lại ứng dụng, không xóa dữ liệu và không thêm các mini-game Monkey Rescue/Flappy.

Nguồn LuyenTu 1.4.0 được phân tích từ archive/decompile; không có mã nguồn gốc đầy đủ để sao chép nguyên trạng. Vì vậy LuyenTu 2.0 tái tạo tính năng và luồng sử dụng có thể xác định, không phải bản sao nguyên mã nguồn.

## 2. Sơ đồ cấu trúc đã xác nhận

- `app/src/main/java/vn/luyentu/app/MainActivity.kt`: Activity và giao diện Jetpack Compose; điều hướng các mục chính, thao tác thư viện từ, nhập tệp, phiên luyện tập và thống kê.
- `app/src/main/java/vn/luyentu/app/VocabularyDatabase.kt`: SQLite, mô hình từ vựng/bộ từ/thư mục, lịch sử ôn, migration và sao lưu/khôi phục JSON.
- `app/src/main/java/vn/luyentu/app/SrsScheduler.kt`: hàm lập lịch SRS tách riêng, có thể kiểm thử độc lập.
- `CsvTools.kt`: phân tích và xuất CSV/TXT/TSV.
- `LuyenTuWidgetProvider.kt` cùng `res/xml/luyentu_widget_info.xml`: widget màn hình chính.
- `DailyReminderWorker.kt`: nhắc ôn cục bộ.
- `app/src/test/`: unit tests; workflow `.github/workflows/android.yml` chạy `gradle testDebugUnitTest assembleDebug` và tải APK debug lên artifact.

## 3. Tính năng hiện có theo mã nguồn

- Thư viện từ vựng; bộ từ và thư mục; tìm kiếm, thêm/sửa/xóa/chuyển từ.
- Nhập hàng loạt CSV/TXT/TSV vào bộ đang chọn; xuất thư viện/bộ từ; backup và restore JSON.
- Các chế độ luyện tập: thẻ từ, trắc nghiệm, nối từ, gõ từ, nghe viết và chế độ đặc biệt có biến thể điền từ.
- Phiên chọn số lượng 10/20/30/50 từ, lấy từ đến hạn và xáo trộn thứ tự khi bắt đầu.
- SRS với bốn mức Again/Hard/Good/Easy, khoảng ôn, ease factor, số lần quên và bước học lại.
- Thống kê, lịch sử ôn, chuỗi ngày học, mục tiêu hằng ngày, nhắc ôn và widget.
- Cơ sở dữ liệu SQLite có bảng từ vựng, bộ từ, thư mục và lịch sử ôn; phiên bản schema hiện là 4.

Đây là kiểm kê dựa trên mã nguồn, không đồng nghĩa mọi luồng đã được kiểm thử thủ công hoặc đã giống hoàn toàn ứng dụng tham chiếu.

## 4. Quyền và ràng buộc offline

Manifest hiện khai báo `POST_NOTIFICATIONS`, không khai báo `INTERNET`. Dữ liệu học được lưu cục bộ. Tính năng đọc từ dùng Android TextToSpeech; khả năng đọc khi không có mạng phụ thuộc dữ liệu giọng đọc offline đã cài trên thiết bị. Cần kiểm tra manifest hợp nhất của APK cuối cùng, không chỉ manifest nguồn.

## 5. Kiểm tra build hiện tại

Commit khảo sát: `970f52dcdb952f9e508ab446a6567dcf3fb55397`.

GitHub Actions run **40093195201** đã kết thúc với kết quả **success** cho lệnh `testDebugUnitTest assembleDebug`. Điều này xác nhận CI build và unit tests hiện có đã qua ở commit đó; không xác nhận đã cài/chạy trên Samsung Galaxy S22 Ultra.

## 6. Rủi ro và phần chưa được xác minh

1. **Luồng phiên luyện tập:** cần kiểm thử tiến độ, hoàn tất phiên, từ bị loại khỏi danh sách đến hạn sau khi chấm, và trạng thái khi thoát/đổi màn hình giữa phiên.
2. **SRS:** unit tests hiện có kiểm tra một số trường hợp cơ bản. Cần kiểm thử bổ sung các chuỗi Again → học lại → Hard/Good/Easy, dữ liệu cũ sau migration và hành vi khi ngày/giờ hệ thống thay đổi.
3. **Trắc nghiệm:** đã có xử lý khi số nghĩa khác nhau không đủ để tạo câu hỏi; vẫn cần kiểm tra nghĩa trùng khác chữ hoa/thường hoặc khoảng trắng và dữ liệu thư viện nhỏ.
4. **Nhập tệp:** cần kiểm thử CSV có dấu phẩy trong trường nghĩa/ví dụ được đặt trong dấu ngoặc kép, BOM UTF-8, dòng lỗi, tệp rỗng, tiêu đề tiếng Việt/Anh, từ trùng và dữ liệu lớn. Chưa xác nhận hỗ trợ XLSX.
5. **Khôi phục JSON:** cần kiểm thử tệp hỏng/phiên bản không hỗ trợ và xác minh rõ thao tác restore thay thế dữ liệu hiện có.
6. **Offline thực tế:** cần kiểm tra APK cài đặt ở chế độ máy bay, TTS offline, nhắc giờ và widget sau khởi động lại.
7. **Giao diện/thiết bị:** chưa có xác nhận thử nghiệm thủ công trên Galaxy S22 Ultra; chưa thể tuyên bố giao diện/luồng giống 100% LuyenTu.

## 7. Tiêu chí bàn giao Giai đoạn 1

- [x] Xác định điểm khởi chạy và các tệp lõi.
- [x] Lập sơ đồ kiến trúc ở mức tệp/thành phần.
- [x] Kiểm kê các chức năng đã có.
- [x] Ghi rõ ràng buộc offline/quyền Android.
- [x] Ghi lại CI hiện tại và giới hạn xác minh.
- [x] Liệt kê rủi ro cần xử lý ở giai đoạn triển khai tiếp theo.
- [ ] Kiểm thử trực tiếp trên điện thoại — chuyển sang giai đoạn kiểm thử, cần thiết bị.

**Kết luận:** Giai đoạn 1 (khảo sát mã nguồn) hoàn tất ở mức kiểm tra tĩnh và tài liệu. Không được coi đây là xác nhận ứng dụng đã hoàn thiện; các rủi ro ở mục 6 vẫn cần được xử lý và kiểm thử ở các giai đoạn tiếp theo.
