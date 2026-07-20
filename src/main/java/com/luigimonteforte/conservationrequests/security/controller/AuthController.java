package com.luigimonteforte.conservationrequests.security.controller;

import com.luigimonteforte.conservationrequests.security.controller.openapi.AuthApi;
import com.luigimonteforte.conservationrequests.security.model.LoginRequest;
import com.luigimonteforte.conservationrequests.security.model.LoginResponse;
import com.luigimonteforte.conservationrequests.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {
	private final AuthService authService;

	@Override
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
		return ResponseEntity.ok(authService.performLogin(loginRequest));
	}
}
