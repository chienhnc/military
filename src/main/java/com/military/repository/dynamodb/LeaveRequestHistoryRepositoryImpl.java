package com.military.repository.dynamodb;

import com.military.models.ELeaveRequestStatus;
import com.military.models.LeaveRequestHistory;
import com.military.repository.LeaveRequestHistoryRepository;
import com.military.repository.dynamodb.item.LeaveRequestHistoryItem;
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
public class LeaveRequestHistoryRepositoryImpl implements LeaveRequestHistoryRepository {
  private final DynamoDbTable<LeaveRequestHistoryItem> table;

  public LeaveRequestHistoryRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.leave_request_histories:leave_request_histories}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(LeaveRequestHistoryItem.class));
  }

  @Override
  public LeaveRequestHistory save(LeaveRequestHistory history) {
    LeaveRequestHistoryItem item = toItem(history);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<LeaveRequestHistory> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    LeaveRequestHistoryItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public List<LeaveRequestHistory> findAllByLeaveRequestId(Long leaveRequestId) {
    if (leaveRequestId == null) {
      return List.of();
    }
    return table.scan().items().stream()
        .map(this::toModel)
        .filter(item -> leaveRequestId.equals(item.getLeaveRequestId()))
        .sorted(Comparator.comparing(LeaveRequestHistory::getRoundNo, Comparator.nullsLast(String::compareTo)))
        .collect(Collectors.toList());
  }

  private long generateUniqueId() {
    for (int i = 0; i < 10; i++) {
      long id = DynamoIdGenerator.nextId();
      if (table.getItem(r -> r.key(k -> k.partitionValue(id))) == null) {
        return id;
      }
    }
    throw new IllegalStateException("Could not generate unique leave request history id");
  }

  private LeaveRequestHistoryItem toItem(LeaveRequestHistory model) {
    LeaveRequestHistoryItem item = new LeaveRequestHistoryItem();
    item.setId(model.getId());
    item.setLeaveRequestId(model.getLeaveRequestId());
    item.setRoundNo(model.getRoundNo());
    item.setMilitaryPersonnelId(model.getMilitaryPersonnelId());
    item.setUserId(model.getUserId());
    item.setCreatedAt(model.getCreatedAt());
    item.setLeaveFrom(model.getLeaveFrom());
    item.setLeaveTo(model.getLeaveTo());
    item.setStatus(model.getStatus() == null ? null : model.getStatus().name());
    item.setAssignee(model.getAssignee());
    item.setFlowId(model.getFlowId());
    item.setOrderNo(model.getOrderNo());
    item.setReason(model.getReason());
    return item;
  }

  private LeaveRequestHistory toModel(LeaveRequestHistoryItem item) {
    LeaveRequestHistory model = new LeaveRequestHistory();
    model.setId(item.getId());
    model.setLeaveRequestId(item.getLeaveRequestId());
    model.setRoundNo(item.getRoundNo());
    model.setMilitaryPersonnelId(item.getMilitaryPersonnelId());
    model.setUserId(item.getUserId());
    model.setCreatedAt(item.getCreatedAt());
    model.setLeaveFrom(item.getLeaveFrom());
    model.setLeaveTo(item.getLeaveTo());
    model.setStatus(parseStatus(item.getStatus()));
    model.setAssignee(item.getAssignee());
    model.setFlowId(item.getFlowId());
    model.setOrderNo(item.getOrderNo());
    model.setReason(item.getReason());
    return model;
  }

  private ELeaveRequestStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    try {
      return ELeaveRequestStatus.valueOf(status);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
