package com.military.controllers;

import com.military.models.EMilitaryPosition;
import com.military.payload.request.LeaveApprovalConfigActiveRequest;
import com.military.payload.request.LeaveApprovalConfigRequest;
import com.military.payload.response.BaseResponse;
import com.military.payload.response.LeaveApprovalConfigResponse;
import com.military.service.LeaveApprovalConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/leave-approval-configs")
@Tag(name = "Leave Approval Config", description = "API quan ly cau hinh phe duyet ngay nghi phep")
public class LeaveApprovalConfigController {
  private final LeaveApprovalConfigService leaveApprovalConfigService;

  public LeaveApprovalConfigController(LeaveApprovalConfigService leaveApprovalConfigService) {
    this.leaveApprovalConfigService = leaveApprovalConfigService;
  }

  @PostMapping
  @Operation(summary = "Them cau hinh phe duyet nghi phep")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tao thanh cong"),
      @ApiResponse(responseCode = "400", description = "Du lieu khong hop le",
          content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  public ResponseEntity<BaseResponse<LeaveApprovalConfigResponse>> create(
      @Valid @RequestBody LeaveApprovalConfigRequest request,
      HttpServletRequest httpRequest) {
    LeaveApprovalConfigResponse response = leaveApprovalConfigService.create(request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Cap nhat cau hinh phe duyet nghi phep")
  public ResponseEntity<BaseResponse<LeaveApprovalConfigResponse>> update(
      @PathVariable Long id,
      @Valid @RequestBody LeaveApprovalConfigRequest request,
      HttpServletRequest httpRequest) {
    LeaveApprovalConfigResponse response = leaveApprovalConfigService.update(id, request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @PatchMapping("/{id}/active")
  @Operation(summary = "Bat/tat ap dung cau hinh")
  public ResponseEntity<BaseResponse<LeaveApprovalConfigResponse>> updateActive(
      @PathVariable Long id,
      @Valid @RequestBody LeaveApprovalConfigActiveRequest request,
      HttpServletRequest httpRequest) {
    LeaveApprovalConfigResponse response = leaveApprovalConfigService.updateActive(id, request.getActive());
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Xoa cau hinh phe duyet nghi phep")
  public ResponseEntity<BaseResponse<String>> delete(@PathVariable Long id, HttpServletRequest request) {
    leaveApprovalConfigService.delete(id);
    return ResponseEntity.ok(BaseResponse.of(200, "Deleted successfully", request.getServletPath()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Chi tiet cau hinh phe duyet nghi phep")
  public ResponseEntity<BaseResponse<LeaveApprovalConfigResponse>> detail(
      @PathVariable Long id,
      HttpServletRequest request) {
    LeaveApprovalConfigResponse response = leaveApprovalConfigService.getById(id);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping
  @Operation(summary = "Danh sach cau hinh phe duyet nghi phep co phan trang")
  public ResponseEntity<BaseResponse<Page<LeaveApprovalConfigResponse>>> list(
      @Parameter(description = "So trang, bat dau tu 0", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "So ban ghi moi trang", example = "10")
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @Parameter(description = "Loc theo chuc vu duyet", example = "CHI_HUY_TRUONG")
      @RequestParam(required = false) EMilitaryPosition militaryPosition,
      @Parameter(description = "Loc theo cờ ap dung", example = "true")
      @RequestParam(required = false) Boolean active,
      @Parameter(description = "Loc khoang hieu luc tu ngay", example = "2026-01-01")
      @RequestParam(required = false) LocalDate effectiveFrom,
      @Parameter(description = "Loc khoang hieu luc den ngay", example = "2026-12-31")
      @RequestParam(required = false) LocalDate effectiveTo,
      HttpServletRequest request) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    Page<LeaveApprovalConfigResponse> response = leaveApprovalConfigService.list(
        militaryPosition, active, effectiveFrom, effectiveTo, pageRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping("/applicable")
  @Operation(summary = "Lay cau hinh dang ap dung theo chuc vu va ngay")
  public ResponseEntity<BaseResponse<LeaveApprovalConfigResponse>> applicable(
      @Parameter(description = "Chuc vu can kiem tra", required = true, example = "CHI_HUY_TRUONG")
      @RequestParam EMilitaryPosition militaryPosition,
      @Parameter(description = "Ngay can kiem tra", required = true, example = "2026-03-14")
      @RequestParam LocalDate applyDate,
      HttpServletRequest request) {
    LeaveApprovalConfigResponse response = leaveApprovalConfigService.getApplicable(militaryPosition, applyDate);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }
}
