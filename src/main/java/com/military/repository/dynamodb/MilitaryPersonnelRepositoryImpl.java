package com.military.repository.dynamodb;

import com.military.models.EMilitaryPosition;
import com.military.models.EMilitaryRank;
import com.military.models.MilitaryPersonnel;
import com.military.repository.MilitaryPersonnelRepository;
import com.military.repository.dynamodb.item.MilitaryPersonnelItem;
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
public class MilitaryPersonnelRepositoryImpl implements MilitaryPersonnelRepository {
  private final DynamoDbTable<MilitaryPersonnelItem> table;

  public MilitaryPersonnelRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.personnel:military_personnel}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(MilitaryPersonnelItem.class));
  }

  @Override
  public MilitaryPersonnel save(MilitaryPersonnel personnel) {
    MilitaryPersonnelItem item = toItem(personnel);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<MilitaryPersonnel> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    MilitaryPersonnelItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public Page<MilitaryPersonnel> findAll(Pageable pageable) {
    List<MilitaryPersonnel> data = table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(MilitaryPersonnel::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public void delete(MilitaryPersonnel personnel) {
    if (personnel == null || personnel.getId() == null) {
      return;
    }
    table.deleteItem(r -> r.key(k -> k.partitionValue(personnel.getId())));
  }

  @Override
  public Optional<MilitaryPersonnel> findFirstByCodeStartingWithOrderByCodeDesc(String prefix) {
    return table.scan().items().stream()
        .filter(item -> item.getCode() != null && item.getCode().startsWith(prefix))
        .max(Comparator.comparing(MilitaryPersonnelItem::getCode))
        .map(this::toModel);
  }

  @Override
  public boolean existsByCode(String code) {
    if (code == null || code.isBlank()) {
      return false;
    }
    return table.scan().items().stream()
        .anyMatch(item -> item.getCode() != null && item.getCode().equalsIgnoreCase(code));
  }

  @Override
  public Page<MilitaryPersonnel> findByFullNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
      String fullNameKeyword,
      String codeKeyword,
      Pageable pageable) {
    String fullNameLower = fullNameKeyword == null ? "" : fullNameKeyword.toLowerCase();
    String codeLower = codeKeyword == null ? "" : codeKeyword.toLowerCase();
    List<MilitaryPersonnel> data = table.scan().items().stream()
        .filter(item -> containsIgnoreCase(item.getFullName(), fullNameLower)
            || containsIgnoreCase(item.getCode(), codeLower))
        .map(this::toModel)
        .sorted(Comparator.comparing(MilitaryPersonnel::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  private boolean containsIgnoreCase(String value, String keywordLower) {
    if (value == null || keywordLower == null || keywordLower.isBlank()) {
      return false;
    }
    return value.toLowerCase().contains(keywordLower);
  }

  private Page<MilitaryPersonnel> paginate(List<MilitaryPersonnel> data, Pageable pageable) {
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
    throw new IllegalStateException("Could not generate unique personnel id");
  }

  private MilitaryPersonnelItem toItem(MilitaryPersonnel model) {
    MilitaryPersonnelItem item = new MilitaryPersonnelItem();
    item.setId(model.getId());
    item.setCode(model.getCode());
    item.setFullName(model.getFullName());
    item.setRegionCode(model.getRegionCode());
    item.setRankCode(model.getRankCode() == null ? null : model.getRankCode().name());
    item.setUnitCode(model.getUnitCode());
    item.setPositionCode(model.getPositionCode() == null ? null : model.getPositionCode().name());
    item.setQrCode(model.getQrCode());
    item.setImagePath(model.getImagePath());
    return item;
  }

  private MilitaryPersonnel toModel(MilitaryPersonnelItem item) {
    MilitaryPersonnel model = new MilitaryPersonnel();
    model.setId(item.getId());
    model.setCode(item.getCode());
    model.setFullName(item.getFullName());
    model.setRegionCode(item.getRegionCode());
    model.setRankCode(parseRank(item.getRankCode()));
    model.setUnitCode(item.getUnitCode());
    model.setPositionCode(parsePosition(item.getPositionCode()));
    model.setQrCode(item.getQrCode());
    model.setImagePath(item.getImagePath());
    return model;
  }

  private EMilitaryRank parseRank(String rankCode) {
    if (rankCode == null || rankCode.isBlank()) {
      return null;
    }
    try {
      return EMilitaryRank.valueOf(rankCode);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private EMilitaryPosition parsePosition(String positionCode) {
    if (positionCode == null || positionCode.isBlank()) {
      return null;
    }
    try {
      return EMilitaryPosition.valueOf(positionCode);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
