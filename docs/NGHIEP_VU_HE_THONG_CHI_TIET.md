# Tài Liệu Nghiệp Vụ Hệ Thống Military Manager (Bản Chi Tiết)

## 1. Tổng quan
- Hệ thống quản lý nhân sự quân đội, tổ chức quân khu/đơn vị, luồng trình và nghiệp vụ nghỉ phép - ra/vào cổng bằng QR.
- Nền tảng kỹ thuật chính: Spring Boot (JWT stateless), DynamoDB, S3, triển khai trên AWS Lambda + API Gateway qua SAM.
- Toàn bộ API dùng base path `/api/...`, phản hồi theo chuẩn `BaseResponse`.

## 2. Đối tượng và vai trò
### 2.1 Vai trò tài khoản
- `ROLE_SYSTEM_ADMIN`: toàn quyền hệ thống.
- `ROLE_ADMIN_REGION`: quản lý theo phạm vi quân khu.
- `ROLE_ADMIN_UNIT`: quản lý theo phạm vi đơn vị.
- `ROLE_USER`: người dùng thường, phạm vi cá nhân.
- `ROLE_MODERATOR`: đã khai báo vai trò, hiện chưa có nghiệp vụ nổi bật riêng.

### 2.2 Đối tượng dữ liệu chính
- User
- Quân nhân (`MilitaryPersonnel`)
- Quân khu (`MilitaryRegion`)
- Đơn vị (`MilitaryUnit`)
- Nhóm trình (`SubmissionGroup`)
- Luồng trình (`SubmissionFlow`)
- Cấu hình phê duyệt nghỉ phép (`LeaveApprovalConfig`)
- Đơn nghỉ phép (`LeaveRequest`)
- Lịch sử đơn nghỉ phép theo round (`LeaveRequestHistory`)
- Log quét QR ra/vào (`QrScanLog`)

## 3. Xác thực và phân quyền
### 3.1 Đăng nhập
- API công khai: `POST /api/auth/signin`.
- Các API còn lại yêu cầu JWT Bearer token.

### 3.2 Đăng ký tài khoản (signup)
- API: `POST /api/auth/signup` (phải đăng nhập trước).
- Quy tắc theo vai trò:
- `ROLE_USER`: không có quyền tạo user.
- `ROLE_SYSTEM_ADMIN`: tạo được user ở mọi quân khu, mọi đơn vị.
- `ROLE_ADMIN_REGION`: chỉ tạo user trong quân khu của mình và đơn vị thuộc quân khu đó.
- `ROLE_ADMIN_UNIT`: chỉ tạo user trong đơn vị của mình.
- Khi signup sẽ tạo đồng thời:
- 1 bản ghi quân nhân mới.
- 1 user gắn 1-1 với quân nhân.

## 4. Nghiệp vụ quản lý tổ chức
### 4.1 Quân khu
- CRUD quân khu.
- Mã quân khu không được trùng.
- Hỗ trợ logo quân khu lưu trên S3.

### 4.2 Đơn vị
- CRUD đơn vị.
- Mã đơn vị không được trùng.
- Hỗ trợ logo đơn vị lưu trên S3.

### 4.3 Quân nhân
- CRUD quân nhân, có QR hệ thống tự sinh.
- `code` quân nhân sinh tự động theo công thức:
- `regionCode|unitCode|rankCode|positionCode|00001`.
- Số đuôi tăng dần theo cùng tổ hợp tiền tố.
- Mặc định API thêm/sửa quân nhân không nhận `code` và `qrCode` từ FE.
- Vì vậy toàn bộ QR của quân nhân là QR do hệ thống tự sinh.

### 4.4 Phân quyền truy cập quân nhân
- Quyền xem danh sách/chi tiết:
- `ROLE_SYSTEM_ADMIN`: xem toàn bộ.
- `ROLE_ADMIN_REGION`: xem toàn bộ quân nhân trong quân khu của mình.
- `ROLE_ADMIN_UNIT`: xem toàn bộ quân nhân trong đơn vị của mình.
- `ROLE_USER`: xem toàn bộ quân nhân trong đơn vị của mình (theo `unitCode` của quân nhân gắn với user đăng nhập).
- Quyền tạo/xóa/sửa:
- `ROLE_USER`: không có quyền tạo, xóa; chỉ được sửa `imagePath` của chính mình.
- `ROLE_ADMIN_UNIT`: full quyền trong đơn vị.
- `ROLE_ADMIN_REGION`: full quyền trong quân khu.
- `ROLE_SYSTEM_ADMIN`: full quyền toàn hệ thống.

## 5. Nghiệp vụ nhóm trình và luồng trình
### 5.1 SubmissionGroup
- Quản lý nhóm trình và danh sách user trong nhóm.
- Nhóm đang được dùng trong flow thì không được xóa.

### 5.2 SubmissionFlow
- Quản lý luồng trình theo danh sách bước `orderNo`.
- Ràng buộc:
- `orderNo` bắt đầu từ 1 và liên tục.
- `groupId` trong flow phải tồn tại.
- Không trùng group trong cùng một flow.
- Có thêm trường `code` để phân biệt nghiệp vụ luồng trình.
- `code` không được trùng (so sánh không phân biệt hoa/thường).

### 5.3 Luồng nghỉ phép mặc định
- Khi xử lý nghiệp vụ nghỉ phép, hệ thống tìm flow mặc định theo:
- `SubmissionFlow.code = LEAVE` hoặc `SubmissionFlow.code = NGHI_PHEP`.
- Không còn lấy theo `name`.

## 6. Cấu hình phê duyệt nghỉ phép
### 6.1 Mục tiêu
- Cấu hình theo chức vụ có quyền duyệt:
- `militaryPosition` (`EMilitaryPosition`)
- `maxApprovalDays` (số ngày được phép duyệt)
- `effectiveFrom`, `effectiveTo` (khoảng hiệu lực)
- `active` (cờ áp dụng)

### 6.2 Quy tắc dữ liệu
- `effectiveFrom <= effectiveTo`.
- Bộ `militaryPosition + effectiveFrom + effectiveTo` là unique.
- Với cùng `militaryPosition`, không cho phép khoảng hiệu lực chồng lấn:
- Không cho phép `effectiveFrom` mới nằm trong khoảng đã có.
- Không cho phép `effectiveTo` mới nằm trong khoảng đã có.
- Không cho phép khoảng mới bao trùm khoảng đã có.
- Nếu cấu hình còn trong thời gian hiệu lực nhưng tạm không dùng, chuyển `active` từ `true` sang `false`.

### 6.3 API chính
- CRUD: `/api/leave-approval-configs`.
- Bật/tắt áp dụng: `PATCH /api/leave-approval-configs/{id}/active`.
- Kiểm tra cấu hình áp dụng: `GET /api/leave-approval-configs/applicable`.

## 7. Nghiệp vụ yêu cầu nghỉ phép
### 7.1 Thông tin đơn nghỉ
- Trường cốt lõi:
- `militaryPersonnelId`, `userId` người tạo.
- `createdAt`.
- `leaveFrom`, `leaveTo`.
- `status` xử lý.
- `flowId`, `currentOrderNo`, `currentRound`, `currentAssignee`.
- `allowedOutCount`: số lần được phép ra (nhập khi tạo đơn).
- `usedOutCount`: số lần đã ra (khởi tạo trống/null).
- `militaryPersonnel`: DTO thông tin quân nhân gắn với đơn (họ tên, cấp bậc, đơn vị, chức vụ, `imageUrl`...), tra cứu theo `militaryPersonnelId` tại thời điểm trả response, `null` nếu quân nhân không còn tồn tại. Áp dụng cho mọi API trả `LeaveRequestResponse` (`my`, `pending`, chi tiết, các hành động duyệt).

### 7.2 Khởi tạo lịch sử round
- Khi tạo đơn:
- Sinh `round 1.0001`: giống thông tin đơn, thêm:
- `assignee` = username người tạo yêu cầu.
- `flowId` = flow nghỉ phép mặc định.
- `order` = vị trí người tạo trong flow.
- `status` = `TRINH`.
- `reason` = lý do người tạo nhập.
- Sinh luôn `round 1.0002`: bước tiếp theo trong flow, với:
- `assignee` = username của cấp duyệt kế tiếp.
- `order` = order kế tiếp.
- `status` = `CHUA_XU_LY`.
- `reason` để trống.

### 7.3 Duyệt đơn theo luồng
- Người ở round hiện tại vào danh sách duyệt và thao tác:
- Tiếp nhận (`accept`).
- Duyệt (`approve`) kèm lý do (nếu có).
- Trả về (`return`) bắt buộc lý do.
- Trình tiếp (`submit-next`) lên cấp cao hơn khi cần.
- Trường hợp cấp hiện tại không đủ quyền duyệt theo cấu hình nghỉ phép thì dùng `submit-next`.

### 7.4 Xử lý khi trả về hoặc cần bổ sung
- Nếu đơn bị trả về:
- Người tạo sửa và trình lại (`resubmit`), đi lại luồng.
- Nếu đơn đã duyệt nhưng cần bổ sung:
- Người tạo bấm sửa đổi bổ sung (`supplement`).
- Tăng major round, ví dụ từ `1.xxxx` sang `2.0001`.
- Clone dữ liệu từ bước gần nhất, gán `assignee` về người yêu cầu, trạng thái mở yêu cầu.
- Sau khi trình, quy trình chạy lại như luồng chuẩn.

### 7.5 API kiểm tra quyền duyệt
- Có API kiểm tra user đăng nhập có quyền duyệt hay không theo cấu hình nghỉ phép hiện hành.
- Dùng cho FE quyết định hiển thị nút nghiệp vụ phù hợp.

## 8. Nghiệp vụ quét QR
### 8.1 Input quét
- API nhận payload data QR từ FE: `POST /api/qr-scan-logs/scan`.
- Có 2 dạng object:
- Quân nhân: object chứa dữ liệu `MilitaryPersonnel`.
- Người dân: object chứa `name`, `birthday`, `address`, `citizenId`, `issueDate`.
- Mỗi lần quét chỉ nhận 1 loại object.

### 8.2 Lưu log quét
- Mỗi lần quét lưu 1 bản ghi log gồm:
- Loại quét (quân nhân/người dân).
- Thời gian quét.
- Trạng thái xử lý.
- Payload dữ liệu đầu vào.
- Lý do kết quả (nếu bị từ chối).

### 8.3 Luồng người dân
- Sau khi quét, trạng thái mặc định: `DANG_XU_LY`.
- Cần gọi API kết thúc luồng:
- Đồng ý -> cập nhật trạng thái `DONG_Y`.
- Từ chối -> cập nhật trạng thái `TU_CHOI`.

### 8.4 Luồng quân nhân
- Khi quét QR quân nhân, hệ thống kiểm tra điều kiện ra cổng:
- Có đơn nghỉ phép hợp lệ của quân nhân.
- Trạng thái đơn/lịch sử duyệt ở mức `DA_DUYET`.
- Ngày hiện tại nằm trong `leaveFrom` đến `leaveTo`.
- Lấy lịch sử duyệt mới nhất theo `round` giảm dần.
- `usedOutCount < allowedOutCount`.
- Kết quả:
- Nếu đạt điều kiện -> `DONG_Y`, đồng thời `usedOutCount = usedOutCount + 1`.
- Nếu không đạt -> `TU_CHOI`, lý do: không có quyền ra.
- Response `QrScanLogResponse` (mọi API trả log quân nhân: `scan`, `approve`, `reject`, chi tiết) kèm thêm ảnh đại diện và phương tiện hiện tại của quân nhân (tra cứu theo `militaryPersonnelId`, không lưu cứng trong log):
- `militaryPersonnelImageUrl`: URL ảnh đại diện (`/api/common/images/personnel/{filename}`), `null` nếu chưa có ảnh.
- `militaryPersonnelVehicle`: thông tin phương tiện (biển số, loại xe, ảnh xe...), `null` nếu quân nhân chưa gắn phương tiện.

### 8.5 Danh sách log ra vào cổng
- API: `GET /api/qr-scan-logs`.
- Tham số: `page`, `size` (mặc định `page=0`, `size=10`), lọc tùy chọn `scanType` (`MILITARY`/`CITIZEN`), `status` (`DANG_XU_LY`/`DONG_Y`/`TU_CHOI`).
- Sắp xếp theo `scannedAt` giảm dần (log quét mới nhất lên đầu).
- Mỗi phần tử trả về cùng cấu trúc `QrScanLogResponse` như API chi tiết (bao gồm ảnh đại diện/phương tiện với log quân nhân).
- Không giới hạn phạm vi theo role/đơn vị, giống các API `scan`/`approve`/`reject`/chi tiết hiện có của module này.

## 9. Quản lý file dùng chung
- Upload ảnh/logo:
- `POST /api/common/upload-image?category=personnel|region|unit`.
- Lấy ảnh/logo:
- `GET /api/common/images/{category}/{filename}`.
- File lưu trên S3 theo prefix cấu hình.

## 10. API dữ liệu combobox cho FE
- `GET /api/common/combobox/ranks`
- `GET /api/common/combobox/positions`
- `GET /api/common/combobox/regions`
- `GET /api/common/combobox/units?regionCode=...`
- `GET /api/common/combobox/users`
- `GET /api/common/combobox/military-personnel`
- Dữ liệu `regions/units` có áp dụng phạm vi theo role đăng nhập.
- Dữ liệu `users` (chỉ trả user có liên kết `MilitaryPersonnel` hợp lệ) áp dụng phạm vi theo role đăng nhập:
- `ROLE_SYSTEM_ADMIN`: xem toàn bộ user.
- `ROLE_ADMIN_UNIT`, `ROLE_USER`: chỉ xem user cùng đơn vị (theo `unitCode` của quân nhân gắn với user đăng nhập).
- Role khác ngoài 3 role trên: bị từ chối (`UNAUTHORIZED`).

## 11. Mô hình lưu trữ dữ liệu (DynamoDB)
- `users`
- `roles`
- `military_regions`
- `military_units`
- `military_personnel`
- `submission_groups`
- `submission_flows`
- `leave_approval_configs`
- `leave_requests`
- `leave_request_histories`
- `qr_scan_logs`

## 12. Triển khai AWS
- Dùng AWS SAM với `template.yaml`.
- Lambda runtime `java17`, handler `com.military.StreamLambdaHandler::handleRequest`.
- IAM policy cấp quyền S3 và DynamoDB phù hợp các bảng nghiệp vụ.

## 13. Đa ngôn ngữ thông báo
- Hệ thống dùng mã lỗi `MILxxxxx`.
- File thông điệp:
- `messages.properties`.
- `messages_vi.properties`.
- Số lượng key tiếng Việt đồng bộ với tiếng Anh.

## 14. Danh sách API theo module (tham chiếu nhanh)
### 14.1 Auth
- `POST /api/auth/signin`
- `POST /api/auth/signup`
- `POST /api/auth/signout`

### 14.2 Common
- `POST /api/common/upload-image`
- `GET /api/common/images/{category}/{filename}`
- `GET /api/common/combobox/*`

### 14.3 Military
- `CRUD /api/military-regions`
- `CRUD /api/military-units`
- `CRUD /api/personnel`

### 14.4 Submission
- `CRUD /api/submission-groups`
- `POST/DELETE /api/submission-groups/{id}/users`
- `CRUD /api/submission-flows`

### 14.5 Leave Approval Config
- `CRUD /api/leave-approval-configs`
- `PATCH /api/leave-approval-configs/{id}/active`
- `GET /api/leave-approval-configs/applicable`

### 14.6 Leave Request
- `POST /api/leave-requests`
- `GET /api/leave-requests/my`
- `GET /api/leave-requests/pending`
- `GET /api/leave-requests/{id}`
- `GET /api/leave-requests/{id}/histories`
- `POST /api/leave-requests/{id}/accept`
- `POST /api/leave-requests/{id}/approve`
- `POST /api/leave-requests/{id}/return`
- `PUT /api/leave-requests/{id}/edit`
- `POST /api/leave-requests/{id}/resubmit`
- `POST /api/leave-requests/{id}/supplement`
- `POST /api/leave-requests/{id}/submit-next`
- API kiểm tra quyền duyệt của user đăng nhập.

### 14.7 QR Scan
- `POST /api/qr-scan-logs/scan`
- `POST /api/qr-scan-logs/{id}/approve`
- `POST /api/qr-scan-logs/{id}/reject`
- `GET /api/qr-scan-logs/{id}`
- `GET /api/qr-scan-logs` (danh sách, lọc `scanType`/`status`, phân trang)
