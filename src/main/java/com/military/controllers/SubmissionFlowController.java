package com.military.controllers;

import com.military.payload.request.SubmissionFlowRequest;
import com.military.payload.response.BaseResponse;
import com.military.payload.response.SubmissionFlowResponse;
import com.military.service.SubmissionFlowService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/submission-flows")
@Tag(name = "Submission Flow", description = "API quan ly luong trinh va nhom trinh")
public class SubmissionFlowController {
  private final SubmissionFlowService submissionFlowService;

  public SubmissionFlowController(SubmissionFlowService submissionFlowService) {
    this.submissionFlowService = submissionFlowService;
  }

  @PostMapping
  @Operation(summary = "Them luong trinh")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tao thanh cong"),
      @ApiResponse(responseCode = "400", description = "Du lieu khong hop le",
          content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  public ResponseEntity<BaseResponse<SubmissionFlowResponse>> create(@Valid @RequestBody SubmissionFlowRequest request,
                                                                     HttpServletRequest httpRequest) {
    SubmissionFlowResponse response = submissionFlowService.create(request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Cap nhat luong trinh")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cap nhat thanh cong"),
      @ApiResponse(responseCode = "404", description = "Khong tim thay luong trinh")
  })
  public ResponseEntity<BaseResponse<SubmissionFlowResponse>> update(@PathVariable Long id,
                                                                     @Valid @RequestBody SubmissionFlowRequest request,
                                                                     HttpServletRequest httpRequest) {
    SubmissionFlowResponse response = submissionFlowService.update(id, request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Xoa luong trinh")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Xoa thanh cong"),
      @ApiResponse(responseCode = "404", description = "Khong tim thay luong trinh")
  })
  public ResponseEntity<BaseResponse<String>> delete(@PathVariable Long id, HttpServletRequest request) {
    submissionFlowService.delete(id);
    return ResponseEntity.ok(BaseResponse.of(200, "Deleted successfully", request.getServletPath()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Chi tiet luong trinh")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lay du lieu thanh cong"),
      @ApiResponse(responseCode = "404", description = "Khong tim thay luong trinh")
  })
  public ResponseEntity<BaseResponse<SubmissionFlowResponse>> detail(@PathVariable Long id, HttpServletRequest request) {
    SubmissionFlowResponse response = submissionFlowService.getById(id);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping
  @Operation(summary = "Danh sach luong trinh co phan trang")
  @ApiResponse(responseCode = "200", description = "Lay danh sach thanh cong")
  public ResponseEntity<BaseResponse<Page<SubmissionFlowResponse>>> list(
      @Parameter(description = "So trang, bat dau tu 0", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "So ban ghi moi trang", example = "10")
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @Parameter(description = "Tu khoa tim kiem theo ten luong trinh", example = "phe duyet")
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    Page<SubmissionFlowResponse> response = submissionFlowService.list(keyword, pageRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }
}
