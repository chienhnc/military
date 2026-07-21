package com.military.payload.request;

import com.military.models.EVehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Thong tin phuong tien cua quan nhan")
public class VehicleRequest {
  @Schema(description = "Loai xe", example = "CAR")
  @NotNull
  private EVehicleType vehicleType;

  @Schema(description = "Hang xe", example = "Toyota")
  @NotBlank
  @Size(max = 100)
  private String brand;

  @Schema(description = "Hieu xe", example = "Corolla")
  @NotBlank
  @Size(max = 100)
  private String model;

  @Schema(description = "Bien so xe", example = "51H-123.45")
  @NotBlank
  @Size(max = 20)
  private String licensePlate;

  @Schema(description = "Danh sach ten file anh da upload qua /api/common/upload-image?category=vehicle (toi da 10 anh)")
  @Size(max = 10)
  private List<@Size(max = 255) String> imagePaths;
}
