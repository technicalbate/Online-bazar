import { useState } from "react";
import { GoogleLogin } from "@react-oauth/google";
import khojoMark from "./assets/khojo-mark.svg";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081";
const roles = [
  { value: "USER", label: "User", description: "Discover and shop" },
  { value: "OWNER", label: "Owner", description: "Manage a storefront" },
  { value: "ADMIN", label: "Admin", description: "Manage the platform" },
];

async function request(path, body) {
  const response = await fetch(`${API_BASE_URL}/api/auth${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (response.status === 204) return null;

  const result = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(result.detail || result.message || "Something went wrong. Please try again.");
  }
  return result;
}

function App({ googleEnabled }) {
  const [mode, setMode] = useState("login");
  const [method, setMethod] = useState("email");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [adminCode, setAdminCode] = useState("");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [codeSent, setCodeSent] = useState(false);
  const [role, setRole] = useState("USER");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);
  const [signedInUser, setSignedInUser] = useState(null);

  const isSignup = mode === "signup";

  async function completeSignIn(result) {
    sessionStorage.setItem("khojo-token", result.token);
    setSignedInUser(result.user);
    setNotice({ type: "success", text: `You’re signed in as ${result.user.name}.` });
  }

  async function perform(action) {
    setBusy(true);
    setNotice(null);
    try {
      const result = await action();
      if (result) await completeSignIn(result);
      return result;
    } catch (error) {
      setNotice({ type: "error", text: error.message });
      return null;
    } finally {
      setBusy(false);
    }
  }

  function submitEmail(event) {
    event.preventDefault();
    return perform(() =>
      request(isSignup ? "/register" : "/login", {
        ...(isSignup ? { name, role, adminCode } : {}),
        email,
        password,
      }),
    );
  }

  function sendCode() {
    return perform(async () => {
      await request("/phone/send-code", { phone });
      setCodeSent(true);
      setNotice({ type: "success", text: "Verification code sent. It expires in 5 minutes." });
      return null;
    });
  }

  function verifyCode(event) {
    event.preventDefault();
    return perform(() => request("/phone/verify-code", { phone, code, name, role, adminCode }));
  }

  function googleSignIn(response) {
    if (!response.credential) {
      setNotice({ type: "error", text: "Google did not return a sign-in credential." });
      return;
    }
    return perform(() => request("/google", { credential: response.credential, role, adminCode }));
  }

  function switchMode(nextMode) {
    setMode(nextMode);
    setNotice(null);
    setCodeSent(false);
    setCode("");
  }

  return (
    <main className="page-shell">
      <section className="auth-layout" aria-label="Khojo account access">
        <aside className="welcome-panel">
          <a className="brand" href="/" aria-label="Khojo home">
            <img alt="" className="brand-icon" src={khojoMark} />
            <span className="brand-copy"><strong>KHOJO</strong><small>HAR DUKAAN, AAPKE PHONE PAR</small></span>
          </a>
          <div className="welcome-copy">
            <span className="eyebrow">A better way to find</span>
            <h1>Good things<br />are closer than<br /><em>you think.</em></h1>
            <p>Your local marketplace for the things you love and the people behind them.</p>
          </div>
          <div className="welcome-footer">
            <span className="status-dot" />
            <span>Made for your neighborhood</span>
            <span className="footer-sparkle">✳</span>
          </div>
          <span className="decor decor-one" />
          <span className="decor decor-two" />
        </aside>

        <section className="form-panel">
          <div className="form-wrap">
            <div className="mobile-brand">
              <img alt="" className="brand-icon" src={khojoMark} />
              <span className="brand-copy"><strong>KHOJO</strong><small>HAR DUKAAN, AAPKE PHONE PAR</small></span>
            </div>
            <div className="form-heading">
              <span className="eyebrow">{isSignup ? "YOUR NEXT CHAPTER STARTS HERE" : "WELCOME BACK"}</span>
              <h2>{isSignup ? "Create your account" : "Sign in to Khojo"}</h2>
              <p>{isSignup ? "It only takes a moment to get started." : "Pick up right where you left off."}</p>
            </div>

            <div className="mode-switch" role="tablist" aria-label="Account action">
              <button className={mode === "login" ? "selected" : ""} onClick={() => switchMode("login")} role="tab" aria-selected={mode === "login"} type="button">Sign in</button>
              <button className={mode === "signup" ? "selected" : ""} onClick={() => switchMode("signup")} role="tab" aria-selected={mode === "signup"} type="button">Create account</button>
            </div>

            <div className="method-switch" aria-label="Choose a sign-in method">
              <button className={method === "email" ? "selected" : ""} onClick={() => { setMethod("email"); setNotice(null); }} type="button">Email</button>
              <button className={method === "phone" ? "selected" : ""} onClick={() => { setMethod("phone"); setNotice(null); }} type="button">Mobile number</button>
            </div>

            {method === "email" ? (
              <form className="auth-form" onSubmit={submitEmail}>
                {isSignup && (
                  <>
                    <label className="field-label" htmlFor="name">Your name</label>
                    <input id="name" autoComplete="name" maxLength="120" onChange={(event) => setName(event.target.value)} placeholder="How should we call you?" required value={name} />
                  </>
                )}
                <label className="field-label" htmlFor="email">Email address</label>
                <input id="email" autoComplete="email" onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" required type="email" value={email} />
                <label className="field-label" htmlFor="password">Password</label>
                <input id="password" autoComplete={isSignup ? "new-password" : "current-password"} minLength={isSignup ? 8 : undefined} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" required type="password" value={password} />
                {isSignup && <RolePicker role={role} onChange={setRole} />}
                {isSignup && role === "ADMIN" && <AdminCodeField onChange={setAdminCode} value={adminCode} />}
                <button className="primary-button" disabled={busy} type="submit">
                  {busy ? "Please wait…" : isSignup ? "Create account" : "Sign in"}
                  {!busy && <span aria-hidden="true">↗</span>}
                </button>
              </form>
            ) : (
              <form className="auth-form" onSubmit={verifyCode}>
                {isSignup && (
                  <>
                    <label className="field-label" htmlFor="phone-name">Your name</label>
                    <input id="phone-name" autoComplete="name" maxLength="120" onChange={(event) => setName(event.target.value)} placeholder="How should we call you?" required value={name} />
                  </>
                )}
                <label className="field-label" htmlFor="phone">Mobile number</label>
                <input id="phone" autoComplete="tel" onChange={(event) => { setPhone(event.target.value); setCodeSent(false); }} placeholder="+919876543210" required type="tel" value={phone} />
                <p className="field-hint">Include your country code, for example +91.</p>
                {isSignup && <RolePicker role={role} onChange={setRole} />}
                {isSignup && role === "ADMIN" && <AdminCodeField onChange={setAdminCode} value={adminCode} />}
                {codeSent && (
                  <>
                    <label className="field-label" htmlFor="code">Verification code</label>
                    <input id="code" autoComplete="one-time-code" inputMode="numeric" maxLength="6" onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))} placeholder="6-digit code" required value={code} />
                  </>
                )}
                {codeSent ? (
                  <button className="primary-button" disabled={busy} type="submit">{busy ? "Verifying…" : "Verify & continue"}<span aria-hidden="true">↗</span></button>
                ) : (
                  <button className="primary-button" disabled={busy || !phone} onClick={sendCode} type="button">{busy ? "Sending code…" : "Send verification code"}<span aria-hidden="true">↗</span></button>
                )}
              </form>
            )}

            <div className="divider"><span>or continue with</span></div>
            {googleEnabled ? (
              <div className="google-button">
                <GoogleLogin
                  onError={() => setNotice({ type: "error", text: "Google sign-in could not be started. Please try again." })}
                  onSuccess={googleSignIn}
                  shape="rectangular"
                  size="large"
                  text="continue_with"
                  theme="outline"
                  width="400"
                />
              </div>
            ) : (
              <p className="provider-hint">Google sign-in needs a Google OAuth client ID. Configure it in the frontend environment.</p>
            )}
            {notice && <p className={`notice ${notice.type}`} role="status">{notice.text}</p>}
            {signedInUser && (
              <div className="account-card" aria-live="polite">
                <span className="account-check">✓</span>
                <span><strong>{signedInUser.name}</strong><small>{signedInUser.role} account · signed in</small></span>
              </div>
            )}
            <p className="terms">By continuing, you agree to Khojo’s <a href="#terms">Terms</a> and <a href="#privacy">Privacy Policy</a>.</p>
          </div>
        </section>
      </section>
    </main>
  );
}

function RolePicker({ role, onChange }) {
  return (
    <fieldset className="role-picker">
      <legend>Choose your account type</legend>
      <div className="role-options">
        {roles.map((option) => (
          <button
            aria-pressed={role === option.value}
            className={`role-option ${role === option.value ? "active" : ""}`}
            key={option.value}
            onClick={() => onChange(option.value)}
            type="button"
          >
            <span>{option.label}</span>
            <small>{option.description}</small>
          </button>
        ))}
      </div>
    </fieldset>
  );
}

function AdminCodeField({ onChange, value }) {
  return (
    <>
      <label className="field-label" htmlFor="admin-code">Administrator invitation code</label>
      <input id="admin-code" autoComplete="off" onChange={(event) => onChange(event.target.value)} placeholder="Enter your invitation code" required type="password" value={value} />
      <p className="field-hint">Admin accounts require a code set by the platform administrator.</p>
    </>
  );
}

export default App;
