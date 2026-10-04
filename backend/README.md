# Khojo backend

Spring Boot REST API for the Khojo frontend. It stores accounts and OTP challenges with Spring Data JPA, provides email/password sign-in, verifies Google OAuth ID tokens, and sends/validates mobile verification codes through Twilio.

## Requirements

- Java 17 or newer
- MySQL for persistent deployment, or the included H2 runtime for local development/tests

## Configure

Set configuration through environment variables rather than committing credentials:

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Random signing secret, at least 32 characters; required to start the service |
| `DB_URL` | JDBC connection URL; set a MySQL URL for persistent data |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials |
| `DB_DRIVER`, `JPA_DATABASE_PLATFORM` | Override database driver and Hibernate dialect when using MySQL |
| `GOOGLE_CLIENT_ID` | Google OAuth web client ID; must match the frontend client |
| `ADMIN_SIGNUP_CODE` | Invitation code required for Admin registration |
| `SMS_ENABLED` | Set to `true` to enable Twilio OTP delivery |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` | Twilio SMS credentials and sender |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Optional SMTP credentials |
| `FRONTEND_ORIGIN` | Allowed frontend origin for CORS; defaults to `http://localhost:5173` |

The default in-memory H2 database is for local development and does not persist across restarts. For production, set `DB_URL` to your MySQL JDBC URL, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER=com.mysql.cj.jdbc.Driver`, and `JPA_DATABASE_PLATFORM=org.hibernate.dialect.MySQLDialect`.

## Run and test

From this repository:

```powershell
$env:JWT_SECRET = "<random-secret-of-at-least-32-characters>"
.\mvnw.cmd spring-boot:run
```

The API listens on port 8081. Run tests with `.\mvnw.cmd test`. JPA creates/updates the user and OTP tables.

## Authentication endpoints

- `POST /api/auth/register` — create an email/password account
- `POST /api/auth/login` — sign in with email/password
- `POST /api/auth/google` — verify a Google credential and sign in/up
- `POST /api/auth/phone/send-code` — send a Twilio OTP
- `POST /api/auth/phone/verify-code` — verify the OTP and sign in/up

Passwords are stored as BCrypt hashes. Google ID tokens are verified by the backend. Phone codes expire after five minutes, are stored as keyed hashes, and are limited to five verification attempts.
