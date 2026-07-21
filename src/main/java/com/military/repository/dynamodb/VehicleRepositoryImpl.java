package com.military.repository.dynamodb;

import com.military.models.EVehicleType;
import com.military.models.Vehicle;
import com.military.repository.VehicleRepository;
import com.military.repository.dynamodb.item.VehicleItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class VehicleRepositoryImpl implements VehicleRepository {
  private final DynamoDbTable<VehicleItem> table;

  public VehicleRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.vehicles:military_vehicles}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(VehicleItem.class));
  }

  @Override
  public Vehicle save(Vehicle vehicle) {
    VehicleItem item = toItem(vehicle);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Vehicle createForPersonnel(Vehicle vehicle) {
    VehicleItem item = toItem(vehicle);
    item.setId(vehicle.getPersonnelId());
    table.putItem(PutItemEnhancedRequest.builder(VehicleItem.class)
        .item(item)
        .conditionExpression(Expression.builder().expression("attribute_not_exists(id)").build())
        .build());
    return toModel(item);
  }

  @Override
  public Optional<Vehicle> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    VehicleItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public Optional<Vehicle> findByPersonnelId(Long personnelId) {
    // Vehicle.id is always set equal to its owning personnelId (see createForPersonnel) —
    // this makes the 1:1 lookup an O(1) direct get instead of a table scan.
    return findById(personnelId);
  }

  @Override
  public Page<Vehicle> findAll(Pageable pageable) {
    List<Vehicle> data = table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(Vehicle::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  @Override
  public void delete(Vehicle vehicle) {
    if (vehicle == null || vehicle.getId() == null) {
      return;
    }
    table.deleteItem(r -> r.key(k -> k.partitionValue(vehicle.getId())));
  }

  @Override
  public List<Vehicle> findAllList() {
    return table.scan().items().stream()
        .map(this::toModel)
        .sorted(Comparator.comparing(Vehicle::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
  }

  @Override
  public Page<Vehicle> findByLicensePlateContainingIgnoreCaseOrBrandContainingIgnoreCaseOrModelContainingIgnoreCase(
      String plateKeyword,
      String brandKeyword,
      String modelKeyword,
      Pageable pageable) {
    String plateLower = plateKeyword == null ? "" : plateKeyword.toLowerCase();
    String brandLower = brandKeyword == null ? "" : brandKeyword.toLowerCase();
    String modelLower = modelKeyword == null ? "" : modelKeyword.toLowerCase();
    List<Vehicle> data = table.scan().items().stream()
        .filter(item -> containsIgnoreCase(item.getLicensePlate(), plateLower)
            || containsIgnoreCase(item.getBrand(), brandLower)
            || containsIgnoreCase(item.getModel(), modelLower))
        .map(this::toModel)
        .sorted(Comparator.comparing(Vehicle::getId, Comparator.nullsLast(Long::compareTo)).reversed())
        .collect(Collectors.toList());
    return paginate(data, pageable);
  }

  private boolean containsIgnoreCase(String value, String keywordLower) {
    if (value == null || keywordLower == null || keywordLower.isBlank()) {
      return false;
    }
    return value.toLowerCase().contains(keywordLower);
  }

  private Page<Vehicle> paginate(List<Vehicle> data, Pageable pageable) {
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
    throw new IllegalStateException("Could not generate unique vehicle id");
  }

  private VehicleItem toItem(Vehicle model) {
    VehicleItem item = new VehicleItem();
    item.setId(model.getId());
    item.setPersonnelId(model.getPersonnelId());
    item.setVehicleType(model.getVehicleType() == null ? null : model.getVehicleType().name());
    item.setBrand(model.getBrand());
    item.setModel(model.getModel());
    item.setLicensePlate(model.getLicensePlate());
    item.setImagePaths(model.getImagePaths());
    return item;
  }

  private Vehicle toModel(VehicleItem item) {
    Vehicle model = new Vehicle();
    model.setId(item.getId());
    model.setPersonnelId(item.getPersonnelId());
    model.setVehicleType(parseVehicleType(item.getVehicleType()));
    model.setBrand(item.getBrand());
    model.setModel(item.getModel());
    model.setLicensePlate(item.getLicensePlate());
    model.setImagePaths(item.getImagePaths());
    return model;
  }

  private EVehicleType parseVehicleType(String vehicleType) {
    if (vehicleType == null || vehicleType.isBlank()) {
      return null;
    }
    try {
      return EVehicleType.valueOf(vehicleType);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
