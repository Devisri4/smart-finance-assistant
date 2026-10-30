import { useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import {
  FaBars,
  FaChartPie,
  FaLightbulb,
  FaListUl,
  FaSignOutAlt,
  FaTimes,
  FaWallet,
} from "react-icons/fa";
import useFetch from "../hooks/useFetch";
import { clearToken } from "../auth";
import ThemeToggle from "./ThemeToggle";
import { ErrorBox, Loader } from "./ui";

const NAV = [
  { to: "/dashboard", label: "Dashboard", Icon: FaChartPie },
  { to: "/expenses", label: "Expenses", Icon: FaListUl },
  { to: "/budgets", label: "Budgets", Icon: FaWallet },
  { to: "/insights", label: "Insights", Icon: FaLightbulb },
];

function Layout() {
  const { data: user, error, reload } = useFetch("/profile");
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const logout = () => {
    clearToken();
    navigate("/login", { replace: true });
  };

  const initials = (user?.name || "?")
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? "open" : ""}`}>
        <div className="brand">
          <span className="brand-logo">₹</span>
          <span>Smart Finance</span>
          <button
            type="button"
            className="icon-btn sidebar-close"
            onClick={() => setMenuOpen(false)}
            aria-label="Close menu"
          >
            <FaTimes />
          </button>
        </div>

        <nav className="nav">
          {NAV.map(({ to, label, Icon }) => (
            <NavLink
              key={to}
              to={to}
              className="nav-link"
              onClick={() => setMenuOpen(false)}
            >
              <Icon aria-hidden="true" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          {user && (
            <div className="user-card">
              <span className="avatar">{initials}</span>
              <div>
                <strong>{user.name}</strong>
                <span className="muted small">{user.occupation || user.email}</span>
              </div>
            </div>
          )}
          <div className="sidebar-actions">
            <ThemeToggle />
            <button type="button" className="btn btn-ghost" onClick={logout}>
              <FaSignOutAlt aria-hidden="true" /> Log out
            </button>
          </div>
        </div>
      </aside>

      {menuOpen && <div className="backdrop" onClick={() => setMenuOpen(false)} />}

      <div className="main">
        <header className="mobile-bar">
          <button
            type="button"
            className="icon-btn"
            onClick={() => setMenuOpen(true)}
            aria-label="Open menu"
          >
            <FaBars />
          </button>
          <span className="brand">
            <span className="brand-logo">₹</span> Smart Finance
          </span>
          <ThemeToggle />
        </header>

        {user?.demo && (
          <div className="demo-banner">
            You are exploring a <strong>demo account</strong> with example data. It is reset
            every time someone clicks &quot;Try the demo&quot;.
          </div>
        )}

        <main className="content">
          {user ? (
            <Outlet key={location.pathname} context={{ user }} />
          ) : error ? (
            <ErrorBox message={error} onRetry={reload} />
          ) : (
            <Loader text="Loading your account… (the free server can take up to a minute to wake up)" />
          )}
        </main>
      </div>
    </div>
  );
}

export default Layout;
