package com.military.controllers;

import com.military.payload.request.VehicleRequest;
import com.military.payload.response.BaseResponse;
import com.military.payload.response.VehicleResponse;
import com.military.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicle", description = "API quan ly phuong tien cua quan nhan: danh sach, chi tiet, sua, xoa anh, xoa")
public class VehicleController {
  private final VehicleService vehicleService;

  public VehicleController(VehicleService vehicleService) {
    this.vehicleService = vehicleService;
  }

  @PostMapping
  @Operation(summary = "Gan phuong tien cho quan nhan chua co phuong tien")
  public ResponseEntity<BaseResponse<VehicleResponse>> attach(@RequestParam Long personnelId,
                                                               @Valid @RequestBody VehicleRequest request,
                                                               HttpServletRequest httpRequest) {
    VehicleResponse response = vehicleService.attachToPersonnel(personnelId, request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @GetMapping
  @Operation(summary = "Danh sach phuong tien co phan trang")
  public ResponseEntity<BaseResponse<Page<VehicleResponse>>> list(
      @Parameter(description = "So trang, bat dau tu 0", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "So ban ghi moi trang", example = "10")
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @Parameter(description = "Tu khoa tim kiem theo bien so, hang xe hoac hieu xe", example = "51H")
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    Page<VehicleResponse> response = vehicleService.list(keyword, pageRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Chi tiet phuong tien")
  public ResponseEntity<BaseResponse<VehicleResponse>> detail(@PathVariable Long id, HttpServletRequest request) {
    VehicleResponse response = vehicleService.getById(id);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping("/by-personnel/{personnelId}")
  @Operation(summary = "Lay phuong tien theo id quan nhan")
  public ResponseEntity<BaseResponse<VehicleResponse>> byPersonnel(@PathVariable Long personnelId,
                                                                    HttpServletRequest request) {
    VehicleResponse response = vehicleService.getByPersonnelId(personnelId);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Cap nhat phuong tien")
  public ResponseEntity<BaseResponse<VehicleResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody VehicleRequest request,
                                                               HttpServletRequest httpRequest) {
    VehicleResponse response = vehicleService.update(id, request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @DeleteMapping("/{id}/images")
  @Operation(summary = "Xoa mot anh phuong tien")
  public ResponseEntity<BaseResponse<String>> deleteImage(@PathVariable Long id,
                                                           @RequestParam @NotBlank String imagePath,
                                                           HttpServletRequest request) {
    vehicleService.deleteImage(id, imagePath);
    return ResponseEntity.ok(BaseResponse.of(200, "Deleted successfully", request.getServletPath()));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Xoa phuong tien")
  public ResponseEntity<BaseResponse<String>> delete(@PathVariable Long id, HttpServletRequest request) {
    vehicleService.delete(id);
    return ResponseEntity.ok(BaseResponse.of(200, "Deleted successfully", request.getServletPath()));
  }
}
