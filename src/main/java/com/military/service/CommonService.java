package com.military.service;

import com.military.payload.response.ComboboxOptionResponse;
import com.military.service.dto.CommonImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CommonService {
  String uploadImage(String category, MultipartFile multipartFile);

  CommonImage loadImage(String category, String filename);

  List<ComboboxOptionResponse> getRankCombobox();

  List<ComboboxOptionResponse> getPositionCombobox();

  List<ComboboxOptionResponse> getRegionComboboxByCurrentUser();

  List<ComboboxOptionResponse> getUnitComboboxByCurrentUser(String regionCode);
}
