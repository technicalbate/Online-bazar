package com.khojo.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthService.AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.registerEmail(
				request.name(), request.email(), request.password(), request.role(), request.adminCode());
	}

	@PostMapping("/login")
	public AuthService.AuthResponse login(@Valid @RequestBody EmailLoginRequest request) {
		return authService.loginEmail(request.email(), request.password());
	}

	@PostMapping("/phone/send-code")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void sendPhoneCode(@Valid @RequestBody PhoneRequest request) {
		authService.sendPhoneOtp(request.phone());
	}

	@PostMapping("/phone/verify-code")
	public AuthService.AuthResponse verifyPhoneCode(@Valid @RequestBody PhoneVerifyRequest request) {
		return authService.verifyPhoneOtp(
				request.phone(), request.code(), request.name(), request.role(), request.adminCode());
	}

	@PostMapping("/google")
	public AuthService.AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
		return authService.loginGoogle(request.credential(), request.role(), request.adminCode());
	}

	public record RegisterRequest(
			@NotBlank @Size(max = 120) String name,
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(min = 8, max = 72) String password,
			@NotNull AuthRole role,
			String adminCode) {
	}

	public record EmailLoginRequest(
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(max = 72) String password) {
	}

	public record PhoneRequest(
			@NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone) {
	}

	public record PhoneVerifyRequest(
			@NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
			@NotBlank @Pattern(regexp = "^\\d{6}$") String code,
			@Size(max = 120) String name,
			AuthRole role,
			String adminCode) {
	}

	public record GoogleLoginRequest(@NotBlank String credential, AuthRole role, String adminCode) {
	}
}
