package com.military.service;

import com.military.payload.request.MilitaryUnitRequest;
import com.military.payload.response.MilitaryUnitResponse;
import com.military.service.dto.UnitLogo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface MilitaryUnitService {
  MilitaryUnitResponse create(MilitaryUnitRequest request);

  MilitaryUnitResponse update(Long id, MilitaryUnitRequest request);

  MilitaryUnitResponse getById(Long id);

  Page<MilitaryUnitResponse> list(String keyword, Pageable pageable);

  void delete(Long id);

  String storeLogo(MultipartFile imageFile);

  UnitLogo loadLogo(String filename);
}
