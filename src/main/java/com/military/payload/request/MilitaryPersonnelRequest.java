package com.military.payload.request;

import com.military.models.EMilitaryPosition;
import com.military.models.EMilitaryRank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @Schema(description = "Ma quan khu (tu dong lay theo ma don vi)", example = "QK7")
    private String regionCode;

    @Schema(description = "Cap bac (enum EMilitaryRank)", example = "DAI_UY")
    @NotNull
    private EMilitaryRank rankCode;

    @Schema(description = "Ma don vi (bat buoc ton tai trong MilitaryUnit)", example = "DV001")
    @NotBlank
    @Size(max = 150)
    private String unitCode;

    @Schema(description = "Chuc vu (enum EMilitaryPosition)", example = "TRUNG_DOI_TRUONG")
    @NotNull
    private EMilitaryPosition positionCode;

    @Schema(description = "QR code base64 (he thong tu sinh, bo qua khi tao)", example = "")
    private String qrCode;

    @Schema(description = "Ten file anh da upload qua API /api/common/upload-image?category=personnel", example = "e7de9ec1-f08d-4f34-bf6b-5f2f0a73c8ca.jpg")
    private String imagePath;
}
