package com.military.repository.dynamodb;

import com.military.models.User;
import com.military.repository.UserRepository;
import com.military.repository.dynamodb.item.UserItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.HashSet;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {
  private final DynamoDbTable<UserItem> table;

  public UserRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.users:users}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(UserItem.class));
  }

  @Override
  public User save(User user) {
    UserItem item = toItem(user);
    if (item.getId() == null) {
      item.setId(generateUniqueId());
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<User> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    UserItem item = table.getItem(r -> r.key(k -> k.partitionValue(id)));
    return Optional.ofNullable(item).map(this::toModel);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }
    return table.scan().items().stream()
        .filter(item -> item.getUsername() != null && item.getUsername().equalsIgnoreCase(username))
        .findFirst()
        .map(this::toModel);
  }

  @Override
  public Boolean existsByUsername(String username) {
    return findByUsername(username).isPresent();
  }

  @Override
  public Boolean existsByEmail(String email) {
    if (email == null || email.isBlank()) {
      return false;
    }
    return table.scan().items().stream()
        .anyMatch(item -> item.getEmail() != null && item.getEmail().equalsIgnoreCase(email));
  }

  @Override
  public Boolean existsByMilitaryPersonnel_Id(Long militaryPersonnelId) {
    if (militaryPersonnelId == null) {
      return false;
    }
    return table.scan().items().stream()
        .anyMatch(item -> militaryPersonnelId.equals(item.getMilitaryPersonnelId()));
  }

  private long generateUniqueId() {
    for (int i = 0; i < 10; i++) {
      long id = DynamoIdGenerator.nextId();
      if (table.getItem(r -> r.key(k -> k.partitionValue(id))) == null) {
        return id;
      }
    }
    throw new IllegalStateException("Could not generate unique user id");
  }

  private UserItem toItem(User model) {
    UserItem item = new UserItem();
    item.setId(model.getId());
    item.setUsername(model.getUsername());
    item.setEmail(model.getEmail());
    item.setPassword(model.getPassword());
    item.setMilitaryPersonnelId(model.getMilitaryPersonnelId());
    item.setRoleNames(model.getRoleNames() == null ? new HashSet<>() : new HashSet<>(model.getRoleNames()));
    return item;
  }

  private User toModel(UserItem item) {
    User model = new User();
    model.setId(item.getId());
    model.setUsername(item.getUsername());
    model.setEmail(item.getEmail());
    model.setPassword(item.getPassword());
    model.setMilitaryPersonnelId(item.getMilitaryPersonnelId());
    model.setRoleNames(item.getRoleNames() == null ? new HashSet<>() : new HashSet<>(item.getRoleNames()));
    return model;
  }
}
