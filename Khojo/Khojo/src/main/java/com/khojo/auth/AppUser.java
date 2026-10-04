package com.khojo.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(unique = true, length = 254)
	private String email;

	@Column(unique = true, length = 16)
	private String phone;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(name = "google_subject", unique = true)
	private String googleSubject;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AuthRole role;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	protected AppUser() {
	}

	public AppUser(String name, String email, String phone, String passwordHash, String googleSubject, AuthRole role) {
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.passwordHash = passwordHash;
		this.googleSubject = googleSubject;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getGoogleSubject() {
		return googleSubject;
	}

	public AuthRole getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setGoogleSubject(String googleSubject) {
		this.googleSubject = googleSubject;
	}
}
