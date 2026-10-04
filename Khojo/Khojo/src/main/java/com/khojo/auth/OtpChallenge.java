package com.khojo.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

	@Id
	@Column(length = 16)
	private String phone;

	@Column(nullable = false, length = 64)
	private String codeHash;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private Instant sentAt;

	@Column(nullable = false)
	private int attempts;

	protected OtpChallenge() {
	}

	public OtpChallenge(String phone, String codeHash, Instant expiresAt, Instant sentAt) {
		this.phone = phone;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
		this.sentAt = sentAt;
		this.attempts = 0;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setCodeHash(String codeHash) {
		this.codeHash = codeHash;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public void setSentAt(Instant sentAt) {
		this.sentAt = sentAt;
	}

	public void incrementAttempts() {
		this.attempts++;
	}
}
