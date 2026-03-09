package com.military.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Thong tin dang nhap")
public class LoginRequest {
	@Schema(description = "Ten dang nhap", example = "admin")
	@NotBlank
	private String username;

	@Schema(description = "Mat khau", example = "123456")
	@NotBlank
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
