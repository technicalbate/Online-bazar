package com.khojo.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private static final Duration OTP_LIFETIME = Duration.ofMinutes(5);
	private static final Duration OTP_RESEND_DELAY = Duration.ofSeconds(30);
	private static final int MAX_OTP_ATTEMPTS = 5;

	private final AppUserRepository users;
	private final OtpChallengeRepository otpChallenges;
	private final PasswordEncoder passwordEncoder;
	private final byte[] jwtSecret;
	private final long jwtExpirationMs;
	private final String googleClientId;
	private final String adminSignupCode;
	private final boolean smsEnabled;
	private final String twilioAccountSid;
	private final String twilioAuthToken;
	private final String twilioFromNumber;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(
			AppUserRepository users,
			OtpChallengeRepository otpChallenges,
			PasswordEncoder passwordEncoder,
			@Value("${app.jwt.secret}") String jwtSecret,
			@Value("${app.jwt.expiration-ms:86400000}") long jwtExpirationMs,
			@Value("${app.google.client-id:}") String googleClientId,
			@Value("${app.auth.admin-signup-code:}") String adminSignupCode,
			@Value("${app.sms.enabled:false}") boolean smsEnabled,
			@Value("${app.sms.account-sid:}") String twilioAccountSid,
			@Value("${app.sms.auth-token:}") String twilioAuthToken,
			@Value("${app.sms.from-number:}") String twilioFromNumber) {
		this.users = users;
		this.otpChallenges = otpChallenges;
		this.passwordEncoder = passwordEncoder;
		this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
		if (this.jwtSecret.length < 32) {
			throw new IllegalStateException("Set JWT_SECRET to a random value of at least 32 characters.");
		}
		this.jwtExpirationMs = jwtExpirationMs;
		this.googleClientId = googleClientId;
		this.adminSignupCode = adminSignupCode;
		this.smsEnabled = smsEnabled;
		this.twilioAccountSid = twilioAccountSid;
		this.twilioAuthToken = twilioAuthToken;
		this.twilioFromNumber = twilioFromNumber;
	}

	@Transactional
	public AuthResponse registerEmail(String name, String email, String password, AuthRole role, String adminCode) {
		validateRequestedRole(role, adminCode);
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes.");
		}
		String normalizedEmail = normalizeEmail(email);
		if (users.existsByEmail(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
		}
		AppUser user = users.save(new AppUser(
				name.trim(), normalizedEmail, null, passwordEncoder.encode(password), null, role));
		return createResponse(user);
	}

	@Transactional(readOnly = true)
	public AuthResponse loginEmail(String email, String password) {
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
		}
		AppUser user = users.findByEmail(normalizeEmail(email))
				.filter(candidate -> candidate.getPasswordHash() != null
						&& passwordEncoder.matches(password, candidate.getPasswordHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
		return createResponse(user);
	}

	@Transactional
	public void sendPhoneOtp(String phone) {
		if (!smsEnabled || isBlank(twilioAccountSid) || isBlank(twilioAuthToken) || isBlank(twilioFromNumber)) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"SMS verification is not configured. Set SMS_ENABLED=true and provide Twilio credentials.");
		}
		Instant now = Instant.now();
		Optional<OtpChallenge> previous = otpChallenges.findById(phone);
		if (previous.isPresent() && previous.get().getSentAt().plus(OTP_RESEND_DELAY).isAfter(now)) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Wait 30 seconds before requesting another code.");
		}

		String code = String.format("%06d", secureRandom.nextInt(1_000_000));
		Twilio.init(twilioAccountSid, twilioAuthToken);
		try {
			Message.creator(
					new PhoneNumber(phone),
					new PhoneNumber(twilioFromNumber),
					"Your Khojo verification code is " + code + ". It expires in 5 minutes.")
					.create();
		} catch (RuntimeException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
					"Could not send the verification code through Twilio.", exception);
		}

		OtpChallenge challenge = previous.orElseGet(() -> new OtpChallenge(phone, "", now, now));
		challenge.setCodeHash(hashCode(phone, code));
		challenge.setExpiresAt(now.plus(OTP_LIFETIME));
		challenge.setSentAt(now);
		otpChallenges.save(challenge);
	}

	@Transactional(noRollbackFor = ResponseStatusException.class)
	public AuthResponse verifyPhoneOtp(
			String phone, String code, String name, AuthRole requestedRole, String adminCode) {
		OtpChallenge challenge = otpChallenges.findById(phone)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Request a verification code first."));
		Instant now = Instant.now();
		if (!challenge.getExpiresAt().isAfter(now)) {
			otpChallenges.delete(challenge);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The verification code has expired.");
		}
		if (challenge.getAttempts() >= MAX_OTP_ATTEMPTS) {
			otpChallenges.delete(challenge);
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect codes. Request a new code.");
		}
		if (!MessageDigest.isEqual(
				challenge.getCodeHash().getBytes(StandardCharsets.US_ASCII),
				hashCode(phone, code).getBytes(StandardCharsets.US_ASCII))) {
			challenge.incrementAttempts();
			otpChallenges.save(challenge);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The verification code is incorrect.");
		}
		otpChallenges.delete(challenge);

		AppUser user = users.findByPhone(phone).orElseGet(() -> {
			if (isBlank(name) || requestedRole == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and role are required for first-time sign-up.");
			}
			validateRequestedRole(requestedRole, adminCode);
			return users.save(new AppUser(name.trim(), null, phone, null, null, requestedRole));
		});
		return createResponse(user);
	}

	@Transactional
	public AuthResponse loginGoogle(String credential, AuthRole requestedRole, String adminCode) {
		if (isBlank(googleClientId)) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google sign-in is not configured. Set GOOGLE_CLIENT_ID.");
		}
		try {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
					new NetHttpTransport(), GsonFactory.getDefaultInstance())
					.setAudience(List.of(googleClientId))
					.build();
			GoogleIdToken idToken = verifier.verify(credential);
			if (idToken == null || !Boolean.TRUE.equals(idToken.getPayload().getEmailVerified())) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google did not verify this account.");
			}

			GoogleIdToken.Payload payload = idToken.getPayload();
			String subject = payload.getSubject();
			String email = normalizeEmail(payload.getEmail());
			AppUser user = users.findByGoogleSubject(subject).orElseGet(() -> {
				AppUser existing = users.findByEmail(email).orElse(null);
				if (existing != null) {
					if (existing.getGoogleSubject() != null) {
						throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is linked to another Google account.");
					}
					existing.setGoogleSubject(subject);
					return users.save(existing);
				}
				if (requestedRole == null) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a role to create your account.");
				}
				validateRequestedRole(requestedRole, adminCode);
				String displayName = (String) payload.get("name");
				return users.save(new AppUser(
						isBlank(displayName) ? email : displayName, email, null, null, subject, requestedRole));
			});
			return createResponse(user);
		} catch (ResponseStatusException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The Google credential could not be verified.", exception);
		}
	}

	private AuthResponse createResponse(AppUser user) {
		Instant now = Instant.now();
		String token = Jwts.builder()
				.subject(user.getId().toString())
				.claim("role", user.getRole().name())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(jwtExpirationMs)))
				.signWith(Keys.hmacShaKeyFor(jwtSecret))
				.compact();
		return new AuthResponse(token, new UserResponse(
				user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole().name()));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private void validateRequestedRole(AuthRole role, String adminCode) {
		if (role == AuthRole.ADMIN) {
			if (isBlank(adminSignupCode) || isBlank(adminCode)
					|| !MessageDigest.isEqual(
							adminSignupCode.getBytes(StandardCharsets.UTF_8),
							adminCode.getBytes(StandardCharsets.UTF_8))) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN,
						"Admin sign-up requires a valid administrator invitation code.");
			}
		}
	}

	private String hashCode(String phone, String code) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(jwtSecret, "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal((phone + ":" + code).getBytes(StandardCharsets.US_ASCII)));
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("HMAC-SHA256 is not available.", exception);
		}
	}

	public record UserResponse(Long id, String name, String email, String phone, String role) {
	}

	public record AuthResponse(String token, UserResponse user) {
	}
}
