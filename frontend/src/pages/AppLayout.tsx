import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function AppLayout() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <NavLink className="sidebar-brand" to="/" aria-label="CollegeOps home">
          <span className="brand-mark brand-mark-small" aria-hidden="true">
            C
          </span>
          <span>CollegeOps</span>
        </NavLink>
        <p className="nav-label">WORKSPACE</p>
        <nav aria-label="Main navigation">
          <NavLink
            className={({ isActive }) =>
              `nav-item${isActive ? " nav-item-active" : ""}`
            }
            end
            to="/"
          >
            <span aria-hidden="true">◈</span>
            Overview
          </NavLink>
          {user?.roles.some((role) =>
            ["INSTITUTION_ADMIN", "IQAC_COORDINATOR", "FACULTY"].includes(role),
          ) && (
            <NavLink
              className={({ isActive }) =>
                `nav-item${isActive ? " nav-item-active" : ""}`
              }
              to="/documents"
            >
              <span aria-hidden="true">▤</span>
              Documents
            </NavLink>
          )}
          <NavLink
            className={({ isActive }) =>
              `nav-item${isActive ? " nav-item-active" : ""}`
            }
            to="/training/skills"
          >
            <span aria-hidden="true">✦</span>
            Skill catalog
          </NavLink>
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
        <Outlet />
      </main>
    </div>
  );
}
