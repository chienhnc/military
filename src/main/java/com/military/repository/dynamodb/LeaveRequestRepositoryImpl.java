package com.military.repository.dynamodb;

import com.military.models.ELeaveRequestStatus;
import com.military.models.LeaveRequest;
import com.military.repository.LeaveRequestRepository;
import com.military.repository.dynamodb.item.LeaveRequestItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class LeaveRequestRepositoryImpl implements LeaveRequestRepository {
  private final DynamoDbTable<LeaveRequestItem> table;

  public LeaveRequestRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.leave_requests:leave_requests}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(LeaveRequestItem.class));
  }

  @Override
  public LeaveRequest save(LeaveRequest leaveRequest) {
    LeaveRequestItem item = toItem(leaveRequest);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<LeaveRequest> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    LeaveRequestItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public Page<LeaveRequest> findAll(Pageable pageable) {
    List<LeaveRequest> data = findAllList();
    int start = (int) pageable.getOffset();
    if (start >= data.size()) {
      return new PageImpl<>(List.of(), pageable, data.size());
    }
    int end = Math.min(start + pageable.getPageSize(), data.size());
    return new PageImpl<>(data.subList(start, end), pageable, data.size());
  }

  @Override
  public List<LeaveRequest> findAllList() {
    return table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(LeaveRequest::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
  }

  private long generateUniqueId() {
    for (int i = 0; i < 10; i++) {
      long id = DynamoIdGenerator.nextId();
      if (table.getItem(r -> r.key(k -> k.partitionValue(id))) == null) {
        return id;
      }
    }
    throw new IllegalStateException("Could not generate unique leave request id");
  }

  private LeaveRequestItem toItem(LeaveRequest model) {
    LeaveRequestItem item = new LeaveRequestItem();
    item.setId(model.getId());
    item.setMilitaryPersonnelId(model.getMilitaryPersonnelId());
    item.setUserId(model.getUserId());
    item.setCreatedAt(model.getCreatedAt());
    item.setLeaveFrom(model.getLeaveFrom());
    item.setLeaveTo(model.getLeaveTo());
    item.setStatus(model.getStatus() == null ? null : model.getStatus().name());
    item.setFlowId(model.getFlowId());
    item.setCurrentOrderNo(model.getCurrentOrderNo());
    item.setCurrentRound(model.getCurrentRound());
    item.setCurrentAssignee(model.getCurrentAssignee());
    item.setReason(model.getReason());
    return item;
  }

  private LeaveRequest toModel(LeaveRequestItem item) {
    LeaveRequest model = new LeaveRequest();
    model.setId(item.getId());
    model.setMilitaryPersonnelId(item.getMilitaryPersonnelId());
    model.setUserId(item.getUserId());
    model.setCreatedAt(item.getCreatedAt());
    model.setLeaveFrom(item.getLeaveFrom());
    model.setLeaveTo(item.getLeaveTo());
    model.setStatus(parseStatus(item.getStatus()));
    model.setFlowId(item.getFlowId());
    model.setCurrentOrderNo(item.getCurrentOrderNo());
    model.setCurrentRound(item.getCurrentRound());
    model.setCurrentAssignee(item.getCurrentAssignee());
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
