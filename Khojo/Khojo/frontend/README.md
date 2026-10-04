# Khojo frontend and authentication

The React/Vite app is in this folder. It supports email/password registration and sign-in, Google sign-in, and Twilio mobile OTP. New accounts can choose `User`, `Owner`, or `Admin`; existing accounts keep their saved role when signing in with another method linked to the same email or phone. User records and one-time challenges are stored in the configured MySQL database.

## Start the app

1. Configure the backend database settings in `src/main/resources/application.properties`.
2. Set `JWT_SECRET` to a random secret of at least 32 characters.
3. Set `ADMIN_SIGNUP_CODE` to an administrator invitation code. Admin can be selected at sign-up, but the backend rejects the role unless this code matches.
4. For Google sign-in, create a Google OAuth web client, allow `http://localhost:5173`, and set `GOOGLE_CLIENT_ID` for the backend and `VITE_GOOGLE_CLIENT_ID` in a copied `.env` file in this folder.
5. For phone verification, set `SMS_ENABLED=true` and configure `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_FROM_NUMBER` in the backend environment. OTP delivery fails explicitly if Twilio is not configured.
6. Set `FRONTEND_ORIGIN` to the deployed frontend origin when not using localhost.
7. Start the Spring Boot backend on port 8081, then run `npm install` and `npm run dev` in this folder.

Copy `.env.example` to `.env` and adjust `VITE_API_BASE_URL` if the backend uses a different address. Email passwords are stored as BCrypt hashes; Google credentials are verified by the backend; SMS OTPs are short-lived, hashed before storage, and limited to five verification attempts.
