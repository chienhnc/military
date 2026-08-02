package com.military.controllers;

import com.military.models.EQrScanStatus;
import com.military.models.EQrScanType;
import com.military.payload.request.QrScanDecisionRequest;
import com.military.payload.request.QrScanRequest;
import com.military.payload.response.BaseResponse;
import com.military.payload.response.QrScanLogResponse;
import com.military.service.QrScanLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/qr-scan-logs")
@Tag(name = "QR Scan Log", description = "API quet QR va luu log ra vao")
public class QrScanLogController {
  private final QrScanLogService qrScanLogService;

  public QrScanLogController(QrScanLogService qrScanLogService) {
    this.qrScanLogService = qrScanLogService;
  }

  @PostMapping("/scan")
  @Operation(summary = "Nhan du lieu QR va xu ly")
  public ResponseEntity<BaseResponse<QrScanLogResponse>> scan(
      @Valid @RequestBody QrScanRequest request,
      HttpServletRequest httpRequest) {
    QrScanLogResponse response = qrScanLogService.scan(request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @PostMapping("/{id}/approve")
  @Operation(summary = "Dong y cho log scan nguoi dan")
  public ResponseEntity<BaseResponse<QrScanLogResponse>> approveCitizen(
      @PathVariable Long id,
      @RequestBody(required = false) QrScanDecisionRequest request,
      HttpServletRequest httpRequest) {
    QrScanLogResponse response = qrScanLogService.approveCitizen(id, request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @PostMapping("/{id}/reject")
  @Operation(summary = "Tu choi cho log scan nguoi dan")
  public ResponseEntity<BaseResponse<QrScanLogResponse>> rejectCitizen(
      @PathVariable Long id,
      @RequestBody(required = false) QrScanDecisionRequest request,
      HttpServletRequest httpRequest) {
    QrScanLogResponse response = qrScanLogService.rejectCitizen(id, request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Chi tiet log scan")
  public ResponseEntity<BaseResponse<QrScanLogResponse>> detail(
      @PathVariable Long id,
      HttpServletRequest httpRequest) {
    QrScanLogResponse response = qrScanLogService.getById(id);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @GetMapping
  @Operation(summary = "Danh sach log ra vao cong")
  public ResponseEntity<BaseResponse<Page<QrScanLogResponse>>> list(
      @RequestParam(required = false) EQrScanType scanType,
      @RequestParam(required = false) EQrScanStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size,
      HttpServletRequest httpRequest) {
    Page<QrScanLogResponse> response = qrScanLogService.list(scanType, status, PageRequest.of(page, size));
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }
}
