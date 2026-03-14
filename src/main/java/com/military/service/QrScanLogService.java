package com.military.service;

import com.military.payload.request.QrScanDecisionRequest;
import com.military.payload.request.QrScanRequest;
import com.military.payload.response.QrScanLogResponse;

public interface QrScanLogService {
  QrScanLogResponse scan(QrScanRequest request);

  QrScanLogResponse approveCitizen(Long id, QrScanDecisionRequest request);

  QrScanLogResponse rejectCitizen(Long id, QrScanDecisionRequest request);

  QrScanLogResponse getById(Long id);
}
