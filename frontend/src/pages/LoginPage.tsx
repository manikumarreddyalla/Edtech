import { useState, type FormEvent } from "react";
import { useAuth } from "../auth/AuthContext";

export function LoginPage() {
  const { login } = useAuth();
  const [institutionCode, setInstitutionCode] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await login({ institutionCode, email, password });
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Sign-in failed. Please try again.",
      );
    } finally {
      setSubmitting(false);
      setPassword("");
    }
  }

  return (
    <main className="login-layout">
      <section className="login-panel" aria-labelledby="login-title">
        <div className="brand-mark" aria-hidden="true">
          C
        </div>
        <p className="eyebrow">COLLEGEOPS</p>
        <h1 id="login-title">Welcome back</h1>
        <p className="muted">
          Sign in with your institution account to continue.
        </p>
        <form className="login-form" onSubmit={handleSubmit}>
          <label>
            Institution code
            <input
              autoComplete="organization"
              name="institutionCode"
              required
              value={institutionCode}
              onChange={(event) => setInstitutionCode(event.target.value)}
            />
          </label>
          <label>
            Email address
            <input
              autoComplete="username"
              name="email"
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </label>
          <label>
            Password
            <input
              autoComplete="current-password"
              name="password"
              type="password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <button className="primary-button" disabled={submitting} type="submit">
            {submitting ? "Signing in…" : "Sign in"}
          </button>
        </form>
        <p className="login-footnote">
          Access is managed by your institution administrator.
        </p>
      </section>
      <aside className="login-aside">
        <div className="aside-content">
          <span className="aside-label">INSTITUTIONAL INTELLIGENCE</span>
          <h2>One connected view of academic quality and student growth.</h2>
          <p>
            Keep institutional records, accreditation evidence, and industry
            readiness work connected through a shared platform.
          </p>
        </div>
        <div className="aside-footer">Built for better institutional outcomes.</div>
      </aside>
    </main>
  );
}
