package com.military.models;

import com.military.payload.request.VehicleRequest;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
@NoArgsConstructor
public class Vehicle {
  private Long id;
  private Long personnelId;
  private EVehicleType vehicleType;
  private String brand;
  private String model;
  private String licensePlate;
  private List<String> imagePaths;

  public Vehicle(VehicleRequest request) {
    BeanUtils.copyProperties(request, this);
  }
}
