package com.military.service;

import com.military.payload.request.MilitaryPersonnelRequest;
import com.military.payload.response.MilitaryPersonnelResponse;
import com.military.service.dto.PersonnelImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface MilitaryPersonnelService {

  MilitaryPersonnelResponse create(MilitaryPersonnelRequest militaryPersonnelRequest);

  MilitaryPersonnelResponse update(Long id, MilitaryPersonnelRequest militaryPersonnelRequest);

  MilitaryPersonnelResponse getById(Long id);

  Page<MilitaryPersonnelResponse> list(String keyword, Pageable pageable);

  void delete(Long id);

  PersonnelImage loadImage(String filename);

  String storeImage(MultipartFile imageFile);
}
