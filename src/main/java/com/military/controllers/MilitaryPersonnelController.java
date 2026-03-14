package com.military.controllers;

import com.military.payload.request.MilitaryPersonnelRequest;
import com.military.payload.response.BaseResponse;
import com.military.payload.response.MilitaryPersonnelResponse;
import com.military.service.MilitaryPersonnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/personnel")
@Tag(name = "Military Personnel", description = "API quan ly quan nhan: them, sua, xoa, chi tiet, danh sach")
public class MilitaryPersonnelController {
  private final MilitaryPersonnelService militaryPersonnelService;

  public MilitaryPersonnelController(MilitaryPersonnelService militaryPersonnelService) {
    this.militaryPersonnelService = militaryPersonnelService;
  }

  @PostMapping()
  @Operation(
      summary = "Them quan nhan",
      description = "Tao moi quan nhan, he thong tu sinh code dinh dang REGION|DONVI|CAPBAC|CHUCVU|00001 va QR code."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tao thanh cong"),
      @ApiResponse(responseCode = "400", description = "Du lieu khong hop le",
          content = @Content(schema = @Schema(implementation = BaseResponse.class)))
  })
  public ResponseEntity<BaseResponse<MilitaryPersonnelResponse>> create(@RequestBody MilitaryPersonnelRequest militaryPersonnelRequest,
                                                                        HttpServletRequest request) {
    MilitaryPersonnelResponse response = militaryPersonnelService.create(militaryPersonnelRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Cap nhat quan nhan",
      description = "Cap nhat thong tin quan nhan theo id. Neu don vi/cap bac/chuc vu thay doi thi code duoc sinh lai."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cap nhat thanh cong"),
      @ApiResponse(responseCode = "404", description = "Khong tim thay quan nhan")
  })
  public ResponseEntity<BaseResponse<MilitaryPersonnelResponse>> update(@PathVariable Long id,
                                                                        @RequestBody MilitaryPersonnelRequest militaryPersonnelRequest,
                                                                        HttpServletRequest request) {
    MilitaryPersonnelResponse response = militaryPersonnelService.update(id, militaryPersonnelRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Xoa quan nhan",
      description = "Xoa ban ghi quan nhan theo id."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Xoa thanh cong"),
      @ApiResponse(responseCode = "404", description = "Khong tim thay quan nhan")
  })
  public ResponseEntity<BaseResponse<String>> delete(@PathVariable Long id, HttpServletRequest request) {
    militaryPersonnelService.delete(id);
    return ResponseEntity.ok(BaseResponse.of(200, "Deleted successfully", request.getServletPath()));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Chi tiet quan nhan",
      description = "Lay thong tin chi tiet quan nhan theo id."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lay du lieu thanh cong"),
      @ApiResponse(responseCode = "404", description = "Khong tim thay quan nhan")
  })
  public ResponseEntity<BaseResponse<MilitaryPersonnelResponse>> detail(@PathVariable Long id, HttpServletRequest request) {
    MilitaryPersonnelResponse response = militaryPersonnelService.getById(id);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

  @GetMapping
  @Operation(
      summary = "Danh sach quan nhan co phan trang",
      description = "Lay danh sach quan nhan theo trang va tim kiem theo ho ten hoac code."
  )
  @ApiResponse(responseCode = "200", description = "Lay danh sach thanh cong")
  public ResponseEntity<BaseResponse<Page<MilitaryPersonnelResponse>>> list(
      @Parameter(description = "So trang, bat dau tu 0", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "So ban ghi moi trang", example = "10")
      @RequestParam(defaultValue = "10") @Min(1) int size,
      @Parameter(description = "Tu khoa tim kiem theo fullName hoac code", example = "NGUYEN")
      @RequestParam(required = false) String keyword,
      HttpServletRequest request) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    Page<MilitaryPersonnelResponse> response = militaryPersonnelService.list(keyword, pageRequest);
    return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
  }

}
