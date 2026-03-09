package com.military.service;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.military.exception.AppException;
import com.military.exception.ErrorCode;
import com.military.models.MilitaryPersonnel;
import com.military.payload.request.MilitaryPersonnelRequest;
import com.military.payload.response.MilitaryPersonnelResponse;
import com.military.repository.MilitaryPersonnelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public interface MilitaryPersonnelService {

  MilitaryPersonnelResponse create(MilitaryPersonnelRequest militaryPersonnelRequest);

  MilitaryPersonnelResponse update(Long id, MilitaryPersonnelRequest militaryPersonnelRequest);

  MilitaryPersonnelResponse getById(Long id);

  Page<MilitaryPersonnelResponse> list(String keyword, Pageable pageable);

  void delete(Long id);

  Path resolveImagePath(String filename);

  String storeImage(MultipartFile imageFile);
}
