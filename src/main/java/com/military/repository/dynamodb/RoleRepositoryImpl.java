package com.military.repository.dynamodb;

import com.military.models.ERole;
import com.military.models.Role;
import com.military.repository.RoleRepository;
import com.military.repository.dynamodb.item.RoleItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {
  private final DynamoDbTable<RoleItem> table;

  public RoleRepositoryImpl(
      DynamoDbEnhancedClient enhancedClient,
      @Value("${military.app.dynamodb.tables.roles:roles}") String tableName) {
    this.table = enhancedClient.table(tableName, TableSchema.fromBean(RoleItem.class));
  }

  @Override
  public Role save(Role role) {
    RoleItem item = toItem(role);
    if (item.getId() == null) {
      item.setId(defaultRoleId(role.getName()));
    }
    table.putItem(item);
    return toModel(item);
  }

  @Override
  public Optional<Role> findByName(ERole name) {
    if (name == null) {
      return Optional.empty();
    }
    RoleItem item = table.getItem(r -> r.key(k -> k.partitionValue(name.name())));
    return Optional.ofNullable(item).map(this::toModel);
  }

  private Integer defaultRoleId(ERole role) {
    if (role == null) {
      return 0;
    }
    return switch (role) {
      case ROLE_USER -> 1;
      case ROLE_MODERATOR -> 2;
      case ROLE_ADMIN -> 3;
    };
  }

  private RoleItem toItem(Role role) {
    RoleItem item = new RoleItem();
    item.setId(role.getId());
    item.setName(role.getName() == null ? null : role.getName().name());
    return item;
  }

  private Role toModel(RoleItem item) {
    Role role = new Role();
    role.setId(item.getId());
    role.setName(item.getName() == null ? null : ERole.valueOf(item.getName()));
    return role;
  }
}
