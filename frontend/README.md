# Khojo frontend

React/Vite sign-in and sign-up interface for email/password, Google OAuth, and mobile OTP. New users choose User, Owner, or Admin. Admin sign-up requires the administrator invitation code configured on the backend.

## Run locally

1. Install Node.js 20.19+ or 22.12+.
2. Copy `.env.example` to `.env`; set `VITE_API_BASE_URL` to the backend URL and `VITE_GOOGLE_CLIENT_ID` to a Google OAuth web client ID.
3. Allow `http://localhost:5173` as an authorized JavaScript origin in the Google OAuth client.
4. Run `npm install` and `npm run dev`.

The backend verifies Google credentials and Twilio SMS codes and stores registered users. See the backend repository for its setup and required environment variables.
