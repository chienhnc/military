package com.military.service;

import com.military.models.EQrScanStatus;
import com.military.models.EQrScanType;
import com.military.payload.request.QrScanDecisionRequest;
import com.military.payload.request.QrScanRequest;
import com.military.payload.response.QrScanLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QrScanLogService {
  QrScanLogResponse scan(QrScanRequest request);

  QrScanLogResponse approveCitizen(Long id, QrScanDecisionRequest request);

  QrScanLogResponse rejectCitizen(Long id, QrScanDecisionRequest request);

  QrScanLogResponse getById(Long id);

  Page<QrScanLogResponse> list(EQrScanType scanType, EQrScanStatus status, Pageable pageable);
}
