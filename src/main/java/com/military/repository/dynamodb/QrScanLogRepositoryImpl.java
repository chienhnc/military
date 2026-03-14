package com.military.repository.dynamodb;

import com.military.models.EQrScanStatus;
import com.military.models.EQrScanType;
import com.military.models.QrScanLog;
import com.military.repository.QrScanLogRepository;
import com.military.repository.dynamodb.item.QrScanLogItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class QrScanLogRepositoryImpl implements QrScanLogRepository {
  private final DynamoDbTable<QrScanLogItem> table;

  public QrScanLogRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.qr_scan_logs:qr_scan_logs}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(QrScanLogItem.class));
  }

  @Override
  public QrScanLog save(QrScanLog log) {
    QrScanLogItem item = toItem(log);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<QrScanLog> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    QrScanLogItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public List<QrScanLog> findAllList() {
    return table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(QrScanLog::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
  }

  private long generateUniqueId() {
    for (int i = 0; i < 10; i++) {
      long id = DynamoIdGenerator.nextId();
      if (table.getItem(r -> r.key(k -> k.partitionValue(id))) == null) {
        return id;
      }
    }
    throw new IllegalStateException("Could not generate unique qr scan log id");
  }

  private QrScanLogItem toItem(QrScanLog model) {
    QrScanLogItem item = new QrScanLogItem();
    item.setId(model.getId());
    item.setScanType(model.getScanType() == null ? null : model.getScanType().name());
    item.setScannedAt(model.getScannedAt());
    item.setStatus(model.getStatus() == null ? null : model.getStatus().name());
    item.setReason(model.getReason());
    item.setMilitaryPersonnelId(model.getMilitaryPersonnelId());
    item.setMilitaryPersonnelCode(model.getMilitaryPersonnelCode());
    item.setMilitaryPersonnelFullName(model.getMilitaryPersonnelFullName());
    item.setCitizenName(model.getCitizenName());
    item.setCitizenBirthday(model.getCitizenBirthday());
    item.setCitizenAddress(model.getCitizenAddress());
    item.setCitizenId(model.getCitizenId());
    item.setCitizenIssueDate(model.getCitizenIssueDate());
    item.setLeaveRequestId(model.getLeaveRequestId());
    item.setApprovedRoundNo(model.getApprovedRoundNo());
    item.setPayloadJson(model.getPayloadJson());
    return item;
  }

  private QrScanLog toModel(QrScanLogItem item) {
    QrScanLog model = new QrScanLog();
    model.setId(item.getId());
    model.setScanType(parseType(item.getScanType()));
    model.setScannedAt(item.getScannedAt());
    model.setStatus(parseStatus(item.getStatus()));
    model.setReason(item.getReason());
    model.setMilitaryPersonnelId(item.getMilitaryPersonnelId());
    model.setMilitaryPersonnelCode(item.getMilitaryPersonnelCode());
    model.setMilitaryPersonnelFullName(item.getMilitaryPersonnelFullName());
    model.setCitizenName(item.getCitizenName());
    model.setCitizenBirthday(item.getCitizenBirthday());
    model.setCitizenAddress(item.getCitizenAddress());
    model.setCitizenId(item.getCitizenId());
    model.setCitizenIssueDate(item.getCitizenIssueDate());
    model.setLeaveRequestId(item.getLeaveRequestId());
    model.setApprovedRoundNo(item.getApprovedRoundNo());
    model.setPayloadJson(item.getPayloadJson());
    return model;
  }

  private EQrScanType parseType(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return EQrScanType.valueOf(value);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private EQrScanStatus parseStatus(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return EQrScanStatus.valueOf(value);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
