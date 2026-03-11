package com.military.controllers;

import com.military.payload.response.BaseResponse;
import com.military.payload.response.ComboboxOptionResponse;
import com.military.service.CommonService;
import com.military.service.dto.CommonImage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/common")
@Tag(name = "Common File", description = "API dung chung de upload va lay anh/logo tren S3")
public class CommonController {
  private final CommonService commonService;

  public CommonController(CommonService commonService) {
    this.commonService = commonService;
  }

  @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload anh len S3", description = "category ho tro: personnel, region, unit")
  @ApiResponse(responseCode = "200", description = "Upload thanh cong")
  public ResponseEntity<BaseResponse<String>> uploadImage(
      @Parameter(description = "Danh muc anh: personnel hoac region", required = true)
      @RequestParam String category,
      @Parameter(description = "File anh/logo", required = true)
      @RequestParam MultipartFile multipartFile,
      HttpServletRequest request) {
    String fileName = commonService.uploadImage(category, multipartFile);
    return ResponseEntity.ok(BaseResponse.of(200, fileName, request.getServletPath()));
  }

  @GetMapping("/images/{category}/{filename:.+}")
  @Operation(summary = "Lay anh/logo tu S3", description = "category ho tro: personnel, region, unit")
  @ApiResponse(responseCode = "200", description = "Lay anh thanh cong")
  public ResponseEntity<ByteArrayResource> getImage(@PathVariable String category, @PathVariable String filename) {
    CommonImage image = commonService.loadImage(category, filename);
    ByteArrayResource resource = new ByteArrayResource(image.content());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.filename() + "\"")
        .contentType(MediaType.parseMediaType(image.contentType()))
        .contentLength(image.content().length)
        .body(resource);
  }

  @GetMapping("/combobox/ranks")
  @Operation(summary = "Combobox cap bac", description = "Tra danh sach cap bac tu enum EMilitaryRank")
  @ApiResponse(responseCode = "200", description = "Lay du lieu thanh cong")
  public ResponseEntity<BaseResponse<List<ComboboxOptionResponse>>> getRankCombobox(HttpServletRequest request) {
    List<ComboboxOptionResponse> data = commonService.getRankCombobox();
    return ResponseEntity.ok(BaseResponse.of(200, data, request.getServletPath()));
  }

  @GetMapping("/combobox/positions")
  @Operation(summary = "Combobox chuc vu", description = "Tra danh sach chuc vu tu enum EMilitaryPosition")
  @ApiResponse(responseCode = "200", description = "Lay du lieu thanh cong")
  public ResponseEntity<BaseResponse<List<ComboboxOptionResponse>>> getPositionCombobox(HttpServletRequest request) {
    List<ComboboxOptionResponse> data = commonService.getPositionCombobox();
    return ResponseEntity.ok(BaseResponse.of(200, data, request.getServletPath()));
  }

  @GetMapping("/combobox/regions")
  @Operation(summary = "Combobox quan khu theo quyen user dang nhap")
  @ApiResponse(responseCode = "200", description = "Lay du lieu thanh cong")
  public ResponseEntity<BaseResponse<List<ComboboxOptionResponse>>> getRegionCombobox(HttpServletRequest request) {
    List<ComboboxOptionResponse> data = commonService.getRegionComboboxByCurrentUser();
    return ResponseEntity.ok(BaseResponse.of(200, data, request.getServletPath()));
  }

  @GetMapping("/combobox/units")
  @Operation(summary = "Combobox don vi theo quyen user dang nhap")
  @ApiResponse(responseCode = "200", description = "Lay du lieu thanh cong")
  public ResponseEntity<BaseResponse<List<ComboboxOptionResponse>>> getUnitCombobox(
      @Parameter(description = "Ma quan khu can loc", example = "QK7")
      @RequestParam(required = false) String regionCode,
      HttpServletRequest request) {
    List<ComboboxOptionResponse> data = commonService.getUnitComboboxByCurrentUser(regionCode);
    return ResponseEntity.ok(BaseResponse.of(200, data, request.getServletPath()));
  }
}
