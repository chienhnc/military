package com.military.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class User {
  private Long id;
  private String username;
  private String email;
  private String password;
  private Set<String> roleNames = new HashSet<>();
  private Long militaryPersonnelId;

  @JsonIgnore
  private MilitaryPersonnel militaryPersonnel;

  public User(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }

  public Set<Role> getRoles() {
    if (roleNames == null || roleNames.isEmpty()) {
      return new HashSet<>();
    }
    return roleNames.stream()
        .map(ERole::valueOf)
        .map(Role::new)
        .collect(Collectors.toSet());
  }

  public void setRoles(Set<Role> roles) {
    if (roles == null) {
      this.roleNames = new HashSet<>();
      return;
    }
    this.roleNames = roles.stream()
        .map(role -> role.getName().name())
        .collect(Collectors.toSet());
  }

  public void setMilitaryPersonnel(MilitaryPersonnel militaryPersonnel) {
    this.militaryPersonnel = militaryPersonnel;
    this.militaryPersonnelId = militaryPersonnel == null ? null : militaryPersonnel.getId();
  }
}
