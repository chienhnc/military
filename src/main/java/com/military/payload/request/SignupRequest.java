package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

import java.util.Set;
 
@Schema(description = "Thong tin dang ky user, bao gom thong tin quan nhan")
public class SignupRequest {
    @Schema(description = "Ten dang nhap", example = "soldier01")
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;
 
    @Schema(description = "Email", example = "soldier01@military.vn")
    @NotBlank
    @Size(max = 50)
    @Email
    private String email;
    
    @Schema(description = "Danh sach role. Neu bo qua se gan ROLE_USER", example = "[\"user\"]")
    private Set<String> role;
    
    @Schema(description = "Mat khau", example = "123456")
    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

    @NotNull
    @Valid
    @Schema(description = "Thong tin quan nhan de tao moi va gan 1-1 cho user")
    private MilitaryPersonnelRequest militaryPersonnel;
  
    public String getUsername() {
        return username;
    }
 
    public void setUsername(String username) {
        this.username = username;
    }
 
    public String getEmail() {
        return email;
    }
 
    public void setEmail(String email) {
        this.email = email;
    }
 
    public String getPassword() {
        return password;
    }
 
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Set<String> getRole() {
      return this.role;
    }
    
    public void setRole(Set<String> role) {
      this.role = role;
    }

    public MilitaryPersonnelRequest getMilitaryPersonnel() {
      return militaryPersonnel;
    }

    public void setMilitaryPersonnel(MilitaryPersonnelRequest militaryPersonnel) {
      this.militaryPersonnel = militaryPersonnel;
    }
}
