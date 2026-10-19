import { useAuth } from "../auth/AuthContext";

export function DashboardPage() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="sidebar-brand" href="/" aria-label="CollegeOps home">
          <span className="brand-mark brand-mark-small" aria-hidden="true">
            C
          </span>
          <span>CollegeOps</span>
        </a>
        <p className="nav-label">WORKSPACE</p>
        <nav aria-label="Main navigation">
          <a className="nav-item nav-item-active" href="/">
            <span aria-hidden="true">◈</span>
            Overview
          </a>
        </nav>
        <div className="sidebar-bottom">
          <div className="user-summary">
            <div className="avatar" aria-hidden="true">
              {user?.displayName.slice(0, 1).toUpperCase()}
            </div>
            <div className="user-summary-text">
              <strong>{user?.displayName}</strong>
              <span>{user?.roles.join(", ")}</span>
            </div>
          </div>
          <button className="text-button" onClick={logout} type="button">
            Sign out
          </button>
        </div>
      </aside>
      <main className="main-content">
        <header className="page-header">
          <div>
            <p className="eyebrow">OVERVIEW</p>
            <h1>Good day, {user?.displayName.split(" ")[0]}</h1>
          </div>
          <span className="institution-chip">
            Institution workspace
          </span>
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
      </main>
    </div>
  );
}
