import { useAuth } from "../auth/AuthContext";

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <>
      <header className="page-header">
        <div>
          <p className="eyebrow">OVERVIEW</p>
          <h1>Good day, {user?.displayName.split(" ")[0]}</h1>
        </div>
        <span className="institution-chip">Institution workspace</span>
      </header>
      <section className="welcome-card">
        <div>
          <p className="eyebrow">YOUR INSTITUTION</p>
          <h2>Welcome to CollegeOps</h2>
          <p>
            Your secure workspace is ready. Institutional records,
            accreditation evidence, and training modules will appear here as
            they are made available to your role.
          </p>
        </div>
        <div className="welcome-illustration" aria-hidden="true">
          <span className="illustration-dot dot-one" />
          <span className="illustration-dot dot-two" />
          <span className="illustration-card">
            <span />
            <span />
            <span />
          </span>
        </div>
      </section>
      <p className="dashboard-note">
        Signed in as <strong>{user?.email}</strong>
      </p>
    </>
  );
}
