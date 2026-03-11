package com.military.controllers;

import com.military.payload.request.MilitaryRegionRequest;
import com.military.payload.response.BaseResponse;
import com.military.payload.response.MilitaryRegionResponse;
import com.military.service.MilitaryRegionService;
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
@RequestMapping("/api/military-regions")
@Tag(name = "Military Region", description = "API quan ly quan khu: them, sua, xoa, chi tiet, danh sach")
public class MilitaryRegionController {
  private final MilitaryRegionService militaryRegionService;

  public MilitaryRegionController(MilitaryRegionService militaryRegionService) {
    this.militaryRegionService = militaryRegionService;
  }

  @PostMapping
  @Operation(summary = "Them quan khu")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tao thanh cong"),
      @ApiResponse(responseCode = "400", description = "Du lieu khong hop le",
          content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  public ResponseEntity<BaseResponse<MilitaryRegionResponse>> create(@Valid @RequestBody MilitaryRegionRequest request,
                                                                     HttpServletRequest httpRequest) {
    MilitaryRegionResponse response = militaryRegionService.create(request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Cap nhat quan khu")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cap nhat thanh cong"),
      @ApiResponse(responseCode = "404", description = "Khong tim thay quan khu")
  })
  public ResponseEntity<BaseResponse<MilitaryRegionResponse>> update(@PathVariable Long id,
                                                                     @Valid @RequestBody MilitaryRegionRequest request,
                                                                     HttpServletRequest httpRequest) {
    MilitaryRegionResponse response = militaryRegionService.update(id, request);
    return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Xoa quan khu")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Xoa thanh cong"),
      @ApiResponse(responseCode = "404", description = "Khong tim thay quan khu")
  })
  public ResponseEntity<BaseResponse<String>> delete(@PathVariable Long id, HttpServletRequest request) {
    militaryRegionService.delete(id);
    return ResponseEntity.ok(BaseResponse.of(200, "Deleted successfully", request.getServletPath()));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Chi tiet quan khu")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lay du lieu thanh cong"),
      @ApiResponse(responseCode = "404", description = "Khong tim thay quan khu")
  })
  public ResponseEntity<BaseResponse<MilitaryRegionResponse>> detail(@PathVariable Long id,
                                                                     HttpServletRequest request) {
    MilitaryRegionResponse response = militaryRegionService.getById(id);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping
  @Operation(summary = "Danh sach quan khu co phan trang")
  @ApiResponse(responseCode = "200", description = "Lay danh sach thanh cong")
  public ResponseEntity<BaseResponse<Page<MilitaryRegionResponse>>> list(
      @Parameter(description = "So trang, bat dau tu 0", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "So ban ghi moi trang", example = "10")
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @Parameter(description = "Tu khoa tim kiem theo ma hoac ten quan khu", example = "QK7")
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    Page<MilitaryRegionResponse> response = militaryRegionService.list(keyword, pageRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

}
