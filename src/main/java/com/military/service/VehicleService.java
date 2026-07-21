package com.military.service;

import com.military.payload.request.VehicleRequest;
import com.military.payload.response.VehicleResponse;
import com.military.service.dto.VehicleImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface VehicleService {
  VehicleResponse create(Long personnelId, VehicleRequest request);

  VehicleResponse attachToPersonnel(Long personnelId, VehicleRequest request);

  VehicleResponse update(Long id, VehicleRequest request);

  VehicleResponse getById(Long id);

  VehicleResponse getByPersonnelId(Long personnelId);

  Page<VehicleResponse> list(String keyword, Pageable pageable);

  void delete(Long id);

  void deleteByPersonnelId(Long personnelId);

  void deleteImage(Long id, String imagePath);

  String storeImage(MultipartFile imageFile);

  VehicleImage loadImage(String filename);
}
