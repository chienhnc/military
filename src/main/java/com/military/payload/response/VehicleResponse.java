package com.military.payload.response;

import com.military.models.EVehicleType;
import com.military.models.Vehicle;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
public class VehicleResponse {
  private Long id;
  private Long personnelId;
  private EVehicleType vehicleType;
  private String brand;
  private String model;
  private String licensePlate;
  private List<String> imageUrls;

  public VehicleResponse(Vehicle vehicle) {
    BeanUtils.copyProperties(vehicle, this);
  }
}
