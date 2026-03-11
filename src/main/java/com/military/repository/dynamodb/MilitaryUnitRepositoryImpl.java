package com.military.repository.dynamodb;

import com.military.models.MilitaryUnit;
import com.military.repository.MilitaryUnitRepository;
import com.military.repository.dynamodb.item.MilitaryUnitItem;
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
public class MilitaryUnitRepositoryImpl implements MilitaryUnitRepository {
  private final DynamoDbTable<MilitaryUnitItem> table;

  public MilitaryUnitRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.units:military_units}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(MilitaryUnitItem.class));
  }

  @Override
  public MilitaryUnit save(MilitaryUnit unit) {
    MilitaryUnitItem item = toItem(unit);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<MilitaryUnit> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    MilitaryUnitItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public Page<MilitaryUnit> findAll(Pageable pageable) {
    List<MilitaryUnit> data = table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(MilitaryUnit::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public void delete(MilitaryUnit unit) {
    if (unit == null || unit.getId() == null) {
      return;
    }
    table.deleteItem(r -> r.key(k -> k.partitionValue(unit.getId())));
  }

  @Override
  public boolean existsByUnitCode(String unitCode) {
    if (unitCode == null || unitCode.isBlank()) {
      return false;
    }
    return table.scan().items().stream()
        .anyMatch(item -> item.getUnitCode() != null && item.getUnitCode().equalsIgnoreCase(unitCode));
  }

  @Override
  public Optional<MilitaryUnit> findByUnitCodeIgnoreCase(String unitCode) {
    if (unitCode == null || unitCode.isBlank()) {
      return Optional.empty();
    }
    return table.scan().items().stream()
        .filter(item -> item.getUnitCode() != null && item.getUnitCode().equalsIgnoreCase(unitCode))
        .findFirst()
        .map(this::toModel);
  }

  @Override
  public List<MilitaryUnit> findAllList() {
    return table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(MilitaryUnit::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public List<MilitaryUnit> findByRegionCodeIgnoreCase(String regionCode) {
    if (regionCode == null || regionCode.isBlank()) {
      return List.of();
    }
    return table.scan().items().stream()
        .filter(item -> item.getRegionCode() != null && item.getRegionCode().equalsIgnoreCase(regionCode))
        .map(this::toModel)
        .sorted(Comparator.comparing(MilitaryUnit::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public Page<MilitaryUnit> findByUnitCodeContainingIgnoreCaseOrUnitNameContainingIgnoreCase(
      String unitCodeKeyword,
      String unitNameKeyword,
      Pageable pageable) {
    String codeLower = unitCodeKeyword == null ? "" : unitCodeKeyword.toLowerCase();
    String nameLower = unitNameKeyword == null ? "" : unitNameKeyword.toLowerCase();
    List<MilitaryUnit> data = table.scan().items().stream()
        .filter(item -> containsIgnoreCase(item.getUnitCode(), codeLower)
            || containsIgnoreCase(item.getUnitName(), nameLower))
        .map(this::toModel)
        .sorted(Comparator.comparing(MilitaryUnit::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  private boolean containsIgnoreCase(String value, String keywordLower) {
    if (value == null || keywordLower == null || keywordLower.isBlank()) {
      return false;
    }
    return value.toLowerCase().contains(keywordLower);
  }

  private Page<MilitaryUnit> paginate(List<MilitaryUnit> data, Pageable pageable) {
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
    throw new IllegalStateException("Could not generate unique military unit id");
  }

  private MilitaryUnitItem toItem(MilitaryUnit model) {
    MilitaryUnitItem item = new MilitaryUnitItem();
    item.setId(model.getId());
    item.setRegionCode(model.getRegionCode());
    item.setUnitCode(model.getUnitCode());
    item.setUnitName(model.getUnitName());
    item.setAddress(model.getAddress());
    item.setEstablishedDate(model.getEstablishedDate());
    item.setDescription(model.getDescription());
    item.setLogoPath(model.getLogoPath());
    return item;
  }

  private MilitaryUnit toModel(MilitaryUnitItem item) {
    MilitaryUnit model = new MilitaryUnit();
    model.setId(item.getId());
    model.setRegionCode(item.getRegionCode());
    model.setUnitCode(item.getUnitCode());
    model.setUnitName(item.getUnitName());
    model.setAddress(item.getAddress());
    model.setEstablishedDate(item.getEstablishedDate());
    model.setDescription(item.getDescription());
    model.setLogoPath(item.getLogoPath());
    return model;
  }
}
