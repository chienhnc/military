package com.military.repository.dynamodb;

import com.military.models.MilitaryRegion;
import com.military.repository.MilitaryRegionRepository;
import com.military.repository.dynamodb.item.MilitaryRegionItem;
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
public class MilitaryRegionRepositoryImpl implements MilitaryRegionRepository {
  private final DynamoDbTable<MilitaryRegionItem> table;

  public MilitaryRegionRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.regions:military_regions}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(MilitaryRegionItem.class));
  }

  @Override
  public MilitaryRegion save(MilitaryRegion region) {
    MilitaryRegionItem item = toItem(region);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<MilitaryRegion> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    MilitaryRegionItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public Page<MilitaryRegion> findAll(Pageable pageable) {
    List<MilitaryRegion> data = table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(MilitaryRegion::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public void delete(MilitaryRegion region) {
    if (region == null || region.getId() == null) {
      return;
    }
    table.deleteItem(r -> r.key(k -> k.partitionValue(region.getId())));
  }

  @Override
  public boolean existsByRegionCode(String regionCode) {
    if (regionCode == null || regionCode.isBlank()) {
      return false;
    }
    return table.scan().items().stream()
        .anyMatch(item -> item.getRegionCode() != null && item.getRegionCode().equalsIgnoreCase(regionCode));
  }

  @Override
  public Optional<MilitaryRegion> findByRegionCodeIgnoreCase(String regionCode) {
    if (regionCode == null || regionCode.isBlank()) {
      return Optional.empty();
    }
    return table.scan().items().stream()
        .filter(item -> item.getRegionCode() != null && item.getRegionCode().equalsIgnoreCase(regionCode))
        .findFirst()
        .map(this::toModel);
  }

  @Override
  public List<MilitaryRegion> findAllList() {
    return table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(MilitaryRegion::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public Page<MilitaryRegion> findByRegionCodeContainingIgnoreCaseOrRegionNameContainingIgnoreCase(
      String regionCodeKeyword,
      String regionNameKeyword,
      Pageable pageable) {
    String codeLower = regionCodeKeyword == null ? "" : regionCodeKeyword.toLowerCase();
    String nameLower = regionNameKeyword == null ? "" : regionNameKeyword.toLowerCase();
    List<MilitaryRegion> data = table.scan().items().stream()
        .filter(item -> containsIgnoreCase(item.getRegionCode(), codeLower)
            || containsIgnoreCase(item.getRegionName(), nameLower))
        .map(this::toModel)
        .sorted(Comparator.comparing(MilitaryRegion::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  private boolean containsIgnoreCase(String value, String keywordLower) {
    if (value == null || keywordLower == null || keywordLower.isBlank()) {
      return false;
    }
    return value.toLowerCase().contains(keywordLower);
  }

  private Page<MilitaryRegion> paginate(List<MilitaryRegion> data, Pageable pageable) {
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
    throw new IllegalStateException("Could not generate unique military region id");
  }

  private MilitaryRegionItem toItem(MilitaryRegion model) {
    MilitaryRegionItem item = new MilitaryRegionItem();
    item.setId(model.getId());
    item.setRegionCode(model.getRegionCode());
    item.setRegionName(model.getRegionName());
    item.setEstablishedDate(model.getEstablishedDate());
    item.setDescription(model.getDescription());
    item.setLogoPath(model.getLogoPath());
    return item;
  }

  private MilitaryRegion toModel(MilitaryRegionItem item) {
    MilitaryRegion model = new MilitaryRegion();
    model.setId(item.getId());
    model.setRegionCode(item.getRegionCode());
    model.setRegionName(item.getRegionName());
    model.setEstablishedDate(item.getEstablishedDate());
    model.setDescription(item.getDescription());
    model.setLogoPath(item.getLogoPath());
    return model;
  }
}
