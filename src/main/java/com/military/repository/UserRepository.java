package com.military.repository;

import com.military.models.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository {
  User save(User user);

  Optional<User> findById(Long id);

  Optional<User> findByUsername(String username);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);

  Boolean existsByMilitaryPersonnel_Id(Long militaryPersonnelId);
}
