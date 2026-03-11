package com.military.repository.dynamodb;

import com.military.models.SubmissionGroup;
import com.military.repository.SubmissionGroupRepository;
import com.military.repository.dynamodb.item.SubmissionGroupItem;
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
public class SubmissionGroupRepositoryImpl implements SubmissionGroupRepository {
  private final DynamoDbTable<SubmissionGroupItem> table;

  public SubmissionGroupRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.submission_groups:submission_groups}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(SubmissionGroupItem.class));
  }

  @Override
  public SubmissionGroup save(SubmissionGroup group) {
    SubmissionGroupItem item = toItem(group);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<SubmissionGroup> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    SubmissionGroupItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public Page<SubmissionGroup> findAll(Pageable pageable) {
    List<SubmissionGroup> data = table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(SubmissionGroup::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public Page<SubmissionGroup> findByNameContainingIgnoreCase(String keyword, Pageable pageable) {
    String lower = keyword == null ? "" : keyword.toLowerCase();
    List<SubmissionGroup> data = table.scan().items().stream()
        .filter(item -> containsIgnoreCase(item.getName(), lower))
        .map(this::toModel)
        .sorted(Comparator.comparing(SubmissionGroup::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public void delete(SubmissionGroup group) {
    if (group == null || group.getId() == null) {
      return;
    }
    table.deleteItem(r -> r.key(k -> k.partitionValue(group.getId())));
  }

  private boolean containsIgnoreCase(String value, String keywordLower) {
    if (value == null || keywordLower == null || keywordLower.isBlank()) {
      return false;
    }
    return value.toLowerCase().contains(keywordLower);
  }

  private Page<SubmissionGroup> paginate(List<SubmissionGroup> data, Pageable pageable) {
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
    throw new IllegalStateException("Could not generate unique submission group id");
  }

  private SubmissionGroupItem toItem(SubmissionGroup model) {
    SubmissionGroupItem item = new SubmissionGroupItem();
    item.setId(model.getId());
    item.setName(model.getName());
    item.setDescription(model.getDescription());
    item.setUserIds(model.getUserIds() == null ? new ArrayList<>() : new ArrayList<>(model.getUserIds()));
    return item;
  }

  private SubmissionGroup toModel(SubmissionGroupItem item) {
    SubmissionGroup model = new SubmissionGroup();
    model.setId(item.getId());
    model.setName(item.getName());
    model.setDescription(item.getDescription());
    model.setUserIds(item.getUserIds() == null ? new ArrayList<>() : new ArrayList<>(item.getUserIds()));
    return model;
  }
}
