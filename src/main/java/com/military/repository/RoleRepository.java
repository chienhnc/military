package com.military.repository;

import com.military.models.ERole;
import com.military.models.Role;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository {
  Role save(Role role);

  Optional<Role> findByName(ERole name);
}
