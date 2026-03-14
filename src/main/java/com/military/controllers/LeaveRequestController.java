package com.military.controllers;

import com.military.payload.request.LeaveRequestActionRequest;
import com.military.payload.request.LeaveRequestCreateRequest;
import com.military.payload.request.LeaveRequestUpdateRequest;
import com.military.payload.response.BaseResponse;
import com.military.payload.response.LeaveApprovalCapabilityResponse;
import com.military.payload.response.LeaveRequestHistoryResponse;
import com.military.payload.response.LeaveRequestResponse;
import com.military.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/leave-requests")
@Tag(name = "Leave Request", description = "API nghiep vu yeu cau nghi phep")
public class LeaveRequestController {
  private final LeaveRequestService leaveRequestService;

  public LeaveRequestController(LeaveRequestService leaveRequestService) {
    this.leaveRequestService = leaveRequestService;
  }

  @PostMapping
  @Operation(summary = "Tao yeu cau nghi phep")
  public ResponseEntity<BaseResponse<LeaveRequestResponse>> create(
      @Valid @RequestBody LeaveRequestCreateRequest request,
      HttpServletRequest httpRequest) {
    LeaveRequestResponse response = leaveRequestService.create(request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Chi tiet yeu cau nghi phep")
  public ResponseEntity<BaseResponse<LeaveRequestResponse>> detail(
      @PathVariable Long id,
      HttpServletRequest request) {
    LeaveRequestResponse response = leaveRequestService.getById(id);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping("/my")
  @Operation(summary = "Danh sach don nghi phep cua toi")
  public ResponseEntity<BaseResponse<Page<LeaveRequestResponse>>> myList(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size,
      HttpServletRequest request) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    Page<LeaveRequestResponse> response = leaveRequestService.listMine(pageRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping("/pending")
  @Operation(summary = "Danh sach don can toi xu ly duyet")
  public ResponseEntity<BaseResponse<Page<LeaveRequestResponse>>> pendingList(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) int size,
      HttpServletRequest request) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    Page<LeaveRequestResponse> response = leaveRequestService.listPendingApproval(pageRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping("/{id}/histories")
  @Operation(summary = "Lich su round xu ly cua yeu cau")
  public ResponseEntity<BaseResponse<List<LeaveRequestHistoryResponse>>> histories(
      @PathVariable Long id,
      HttpServletRequest request) {
    List<LeaveRequestHistoryResponse> response = leaveRequestService.getHistories(id);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @PostMapping("/{id}/accept")
  @Operation(summary = "Tiep nhan don")
  public ResponseEntity<BaseResponse<LeaveRequestResponse>> accept(
      @PathVariable Long id,
      @RequestBody(required = false) LeaveRequestActionRequest actionRequest,
      HttpServletRequest request) {
    LeaveRequestResponse response = leaveRequestService.accept(id, actionRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @PostMapping("/{id}/approve")
  @Operation(summary = "Duyet don")
  public ResponseEntity<BaseResponse<LeaveRequestResponse>> approve(
      @PathVariable Long id,
      @RequestBody(required = false) LeaveRequestActionRequest actionRequest,
      HttpServletRequest request) {
    LeaveRequestResponse response = leaveRequestService.approve(id, actionRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @PostMapping("/{id}/return")
  @Operation(summary = "Tra ve don")
  public ResponseEntity<BaseResponse<LeaveRequestResponse>> sendBack(
      @PathVariable Long id,
      @RequestBody(required = false) LeaveRequestActionRequest actionRequest,
      HttpServletRequest request) {
    LeaveRequestResponse response = leaveRequestService.sendBack(id, actionRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @PutMapping("/{id}/edit")
  @Operation(summary = "Nguoi tao sua don de trinh lai")
  public ResponseEntity<BaseResponse<LeaveRequestResponse>> edit(
      @PathVariable Long id,
      @Valid @RequestBody LeaveRequestUpdateRequest updateRequest,
      HttpServletRequest request) {
    LeaveRequestResponse response = leaveRequestService.updateForResubmit(id, updateRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @PostMapping("/{id}/resubmit")
  @Operation(summary = "Nguoi tao trinh lai don sau khi sua")
  public ResponseEntity<BaseResponse<LeaveRequestResponse>> resubmit(
      @PathVariable Long id,
      @RequestBody(required = false) LeaveRequestActionRequest actionRequest,
      HttpServletRequest request) {
    LeaveRequestResponse response = leaveRequestService.resubmit(id, actionRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @PostMapping("/{id}/supplement")
  @Operation(summary = "Mo sua doi bo sung, tao round moi x.0001")
  public ResponseEntity<BaseResponse<LeaveRequestResponse>> supplement(
      @PathVariable Long id,
      @RequestBody(required = false) LeaveRequestActionRequest actionRequest,
      HttpServletRequest request) {
    LeaveRequestResponse response = leaveRequestService.openSupplement(id, actionRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @PostMapping("/{id}/submit-next")
  @Operation(summary = "Trinh tiep len cap cao hon")
  public ResponseEntity<BaseResponse<LeaveRequestResponse>> submitNext(
      @PathVariable Long id,
      @RequestBody(required = false) LeaveRequestActionRequest actionRequest,
      HttpServletRequest request) {
    LeaveRequestResponse response = leaveRequestService.submitNext(id, actionRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping("/approval-capability")
  @Operation(summary = "Kiem tra user hien tai co quyen duyet theo cau hinh nghi phep hay khong")
  public ResponseEntity<BaseResponse<LeaveApprovalCapabilityResponse>> checkApprovalCapability(
      @RequestParam(required = false) Long leaveRequestId,
      HttpServletRequest request) {
    LeaveApprovalCapabilityResponse response = leaveRequestService
        .checkApprovalCapability(leaveRequestId);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }
}
