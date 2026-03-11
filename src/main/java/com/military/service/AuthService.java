package com.military.service;

import com.military.payload.request.LoginRequest;
import com.military.payload.request.SignupRequest;
import com.military.payload.response.UserInfoResponse;

public interface AuthService {
  UserInfoResponse authenticateUser(LoginRequest loginRequest);

  String registerUser(SignupRequest signUpRequest);

  String logoutUser();
}
