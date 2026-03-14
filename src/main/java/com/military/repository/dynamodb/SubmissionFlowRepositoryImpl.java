package com.military.repository.dynamodb;

import com.military.models.SubmissionFlow;
import com.military.models.SubmissionFlowGroup;
import com.military.repository.SubmissionFlowRepository;
import com.military.repository.dynamodb.item.SubmissionFlowGroupItem;
import com.military.repository.dynamodb.item.SubmissionFlowItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SubmissionFlowRepositoryImpl implements SubmissionFlowRepository {
  private final DynamoDbTable<SubmissionFlowItem> table;

  public SubmissionFlowRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.submission_flows:submission_flows}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(SubmissionFlowItem.class));
  }

  @Override
  public SubmissionFlow save(SubmissionFlow flow) {
    SubmissionFlowItem item = toItem(flow);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<SubmissionFlow> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    SubmissionFlowItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public Page<SubmissionFlow> findAll(Pageable pageable) {
    List<SubmissionFlow> data = table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(SubmissionFlow::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public void delete(SubmissionFlow flow) {
    if (flow == null || flow.getId() == null) {
      return;
    }
    table.deleteItem(r -> r.key(k -> k.partitionValue(flow.getId())));
  }

  @Override
  public Page<SubmissionFlow> findByNameContainingIgnoreCase(String nameKeyword, Pageable pageable) {
    String nameLower = nameKeyword == null ? "" : nameKeyword.toLowerCase();
    List<SubmissionFlow> data = table.scan().items().stream()
        .filter(item -> containsIgnoreCase(item.getName(), nameLower))
        .map(this::toModel)
        .sorted(Comparator.comparing(SubmissionFlow::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public boolean existsByGroupId(Long groupId) {
    if (groupId == null) {
      return false;
    }
    return table.scan().items().stream()
        .anyMatch(item -> item.getGroups() != null
            && item.getGroups().stream().anyMatch(group -> groupId.equals(group.getGroupId())));
  }

  @Override
  public boolean existsByCodeIgnoreCase(String code, Long excludeId) {
    if (code == null || code.isBlank()) {
      return false;
    }
    return table.scan().items().stream()
        .filter(item -> excludeId == null || !excludeId.equals(item.getId()))
        .anyMatch(item -> item.getCode() != null && item.getCode().equalsIgnoreCase(code.trim()));
  }

  private boolean containsIgnoreCase(String value, String keywordLower) {
    if (value == null || keywordLower == null || keywordLower.isBlank()) {
      return false;
    }
    return value.toLowerCase().contains(keywordLower);
  }

  private Page<SubmissionFlow> paginate(List<SubmissionFlow> data, Pageable pageable) {
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
    throw new IllegalStateException("Could not generate unique submission flow id");
  }

  private SubmissionFlowItem toItem(SubmissionFlow model) {
    SubmissionFlowItem item = new SubmissionFlowItem();
    item.setId(model.getId());
    item.setCode(model.getCode());
    item.setName(model.getName());
    item.setDescription(model.getDescription());
    item.setGroups(model.getGroups() == null ? new ArrayList<>() : model.getGroups().stream()
        .map(this::toGroupItem)
        .collect(Collectors.toList()));
    return item;
  }

  private SubmissionFlowGroupItem toGroupItem(SubmissionFlowGroup group) {
    SubmissionFlowGroupItem item = new SubmissionFlowGroupItem();
    item.setOrderNo(group.getOrderNo());
    item.setGroupId(group.getGroupId());
    return item;
  }

  private SubmissionFlow toModel(SubmissionFlowItem item) {
    SubmissionFlow model = new SubmissionFlow();
    model.setId(item.getId());
    model.setCode(item.getCode());
    model.setName(item.getName());
    model.setDescription(item.getDescription());
    model.setGroups(item.getGroups() == null ? new ArrayList<>() : item.getGroups().stream()
        .map(this::toGroupModel)
        .collect(Collectors.toList()));
    return model;
  }

  private SubmissionFlowGroup toGroupModel(SubmissionFlowGroupItem item) {
    SubmissionFlowGroup model = new SubmissionFlowGroup();
    model.setOrderNo(item.getOrderNo());
    model.setGroupId(item.getGroupId());
    return model;
  }
}
