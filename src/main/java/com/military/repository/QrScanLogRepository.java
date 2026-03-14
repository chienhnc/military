package com.military.repository;

import com.military.models.QrScanLog;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrScanLogRepository {
  QrScanLog save(QrScanLog log);

  Optional<QrScanLog> findById(Long id);

  List<QrScanLog> findAllList();
}
