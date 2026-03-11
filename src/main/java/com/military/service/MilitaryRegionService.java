package com.military.service;

import com.military.payload.request.MilitaryRegionRequest;
import com.military.payload.response.MilitaryRegionResponse;
import com.military.service.dto.RegionLogo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface MilitaryRegionService {
  MilitaryRegionResponse create(MilitaryRegionRequest request);

  MilitaryRegionResponse update(Long id, MilitaryRegionRequest request);

  MilitaryRegionResponse getById(Long id);

  Page<MilitaryRegionResponse> list(String keyword, Pageable pageable);

  void delete(Long id);

  String storeLogo(MultipartFile imageFile);

  RegionLogo loadLogo(String filename);
}
