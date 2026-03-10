package com.military.config;

import com.military.models.ERole;
import com.military.models.Role;
import com.military.repository.RoleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoleBootstrapConfig {

  @Bean
  public ApplicationRunner roleBootstrapRunner(RoleRepository roleRepository) {
    return args -> {
      for (ERole value : ERole.values()) {
        roleRepository.findByName(value).orElseGet(() -> roleRepository.save(new Role(value)));
      }
    };
  }
}
