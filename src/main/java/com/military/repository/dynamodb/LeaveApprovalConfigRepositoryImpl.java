package com.military.repository.dynamodb;

import com.military.models.EMilitaryPosition;
import com.military.models.LeaveApprovalConfig;
import com.military.repository.LeaveApprovalConfigRepository;
import com.military.repository.dynamodb.item.LeaveApprovalConfigItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class LeaveApprovalConfigRepositoryImpl implements LeaveApprovalConfigRepository {
  private final DynamoDbTable<LeaveApprovalConfigItem> table;

  public LeaveApprovalConfigRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.leave_approval_configs:leave_approval_configs}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(LeaveApprovalConfigItem.class));
  }

  @Override
  public LeaveApprovalConfig save(LeaveApprovalConfig config) {
    LeaveApprovalConfigItem item = toItem(config);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<LeaveApprovalConfig> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    LeaveApprovalConfigItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public Page<LeaveApprovalConfig> findAll(Pageable pageable) {
    List<LeaveApprovalConfig> data = table.scan().items().stream()
        .map(this::toModel)
        .sorted(byLatestFirst())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public void delete(LeaveApprovalConfig config) {
    if (config == null || config.getId() == null) {
      return;
    }
    table.deleteItem(r -> r.key(k -> k.partitionValue(config.getId())));
  }

  @Override
  public Page<LeaveApprovalConfig> findByFilters(EMilitaryPosition militaryPosition,
                                                 Boolean active,
                                                 LocalDate effectiveFrom,
                                                 LocalDate effectiveTo,
                                                 Pageable pageable) {
    List<LeaveApprovalConfig> data = table.scan().items().stream()
        .map(this::toModel)
        .filter(item -> militaryPosition == null || militaryPosition == item.getMilitaryPosition())
        .filter(item -> active == null || active.equals(item.getActive()))
        .filter(item -> effectiveFrom == null
            || item.getEffectiveTo() == null
            || !item.getEffectiveTo().isBefore(effectiveFrom))
        .filter(item -> effectiveTo == null
            || item.getEffectiveFrom() == null
            || !item.getEffectiveFrom().isAfter(effectiveTo))
        .sorted(byLatestFirst())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public Optional<LeaveApprovalConfig> findApplicable(EMilitaryPosition militaryPosition, LocalDate applyDate) {
    if (militaryPosition == null || applyDate == null) {
      return Optional.empty();
    }
    return table.scan().items().stream()
        .map(this::toModel)
        .filter(item -> item.getMilitaryPosition() == militaryPosition)
        .filter(item -> Boolean.TRUE.equals(item.getActive()))
        .filter(item -> item.getEffectiveFrom() != null && item.getEffectiveTo() != null)
        .filter(item -> !applyDate.isBefore(item.getEffectiveFrom()) && !applyDate.isAfter(item.getEffectiveTo()))
        .sorted(byLatestFirst())
        .findFirst();
  }

  private Comparator<LeaveApprovalConfig> byLatestFirst() {
    return Comparator.comparing(LeaveApprovalConfig::getEffectiveFrom, Comparator.nullsLast(LocalDate::compareTo))
        .thenComparing(LeaveApprovalConfig::getId, Comparator.nullsLast(Long::compareTo))
        .reversed();
  }

  private Page<LeaveApprovalConfig> paginate(List<LeaveApprovalConfig> data, Pageable pageable) {
    int start = (int) pageable.getOffset();
    if (start >= data.size()) {
      return new PageImpl<>(List.of(), pageable, data.size());
    }
    int end = Math.min(start + pageable.getPageSize(), data.size());
    return new PageImpl<>(data.subList(start, end), pageable, data.size());
  }

  private long generateUniqueId() {
    for (int i = 0; i < 10; i++) {
      long id = DynamoIdGenerator.nextId();
      if (table.getItem(r -> r.key(k -> k.partitionValue(id))) == null) {
        return id;
      }
    }
    throw new IllegalStateException("Could not generate unique leave approval config id");
  }

  private LeaveApprovalConfigItem toItem(LeaveApprovalConfig model) {
    LeaveApprovalConfigItem item = new LeaveApprovalConfigItem();
    item.setId(model.getId());
    item.setMilitaryPosition(model.getMilitaryPosition() == null ? null : model.getMilitaryPosition().name());
    item.setMaxApprovalDays(model.getMaxApprovalDays());
    item.setEffectiveFrom(model.getEffectiveFrom());
    item.setEffectiveTo(model.getEffectiveTo());
    item.setActive(model.getActive());
    return item;
  }

  private LeaveApprovalConfig toModel(LeaveApprovalConfigItem item) {
    LeaveApprovalConfig model = new LeaveApprovalConfig();
    model.setId(item.getId());
    model.setMilitaryPosition(parsePosition(item.getMilitaryPosition()));
    model.setMaxApprovalDays(item.getMaxApprovalDays());
    model.setEffectiveFrom(item.getEffectiveFrom());
    model.setEffectiveTo(item.getEffectiveTo());
    model.setActive(item.getActive());
    return model;
  }

  private EMilitaryPosition parsePosition(String position) {
    if (position == null || position.isBlank()) {
      return null;
    }
    try {
      return EMilitaryPosition.valueOf(position);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
