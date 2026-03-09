package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Thong tin quan nhan")
public class MilitaryPersonnelRequest {

    @Schema(description = "Ma quan nhan (he thong tu sinh, bo qua khi tao)", example = "SU-DOAN-1-DAI-UY-CHI-HUY-00001")
    private String code;

    @Schema(description = "Ho ten quan nhan", example = "Nguyen Van A")
    @NotBlank
    @Size(max = 200)
    private String fullName;

    @Schema(description = "Cap bac", example = "Dai uy")
    @NotBlank
    @Size(max = 100)
    private String rankCode;

    @Schema(description = "Don vi", example = "Su doan 1")
    @NotBlank
    @Size(max = 150)
    private String unitCode;

    @Schema(description = "Chuc vu", example = "Dai doi truong")
    @NotBlank
    @Size(max = 150)
    private String positionCode;

    @Schema(description = "QR code base64 (he thong tu sinh, bo qua khi tao)", example = "")
    private String qrCode;

    @Schema(description = "Ten file anh da upload qua API /api/personnel/upload-image", example = "e7de9ec1-f08d-4f34-bf6b-5f2f0a73c8ca.jpg")
    private String imagePath;
}
