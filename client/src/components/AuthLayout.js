import { FaBell, FaChartLine, FaLightbulb, FaWallet } from "react-icons/fa";
import ThemeToggle from "./ThemeToggle";

const FEATURES = [
  { Icon: FaChartLine, text: "Track every rupee with clear charts" },
  { Icon: FaWallet, text: "Set monthly budgets per category" },
  { Icon: FaBell, text: "Get alerts before you overspend" },
  { Icon: FaLightbulb, text: "Smart insights about your habits" },
];

/** The two-column look shared by the login and sign-up pages. */
function AuthLayout({ children }) {
  return (
    <div className="auth-page">
      <section className="auth-hero">
        <div className="brand">
          <span className="brand-logo">₹</span> Smart Finance
        </div>
        <h1>Take control of your money.</h1>
        <p>A personal finance assistant that tracks, plans and explains your spending.</p>
        <ul className="feature-list">
          {FEATURES.map(({ Icon, text }) => (
            <li key={text}>
              <Icon aria-hidden="true" /> {text}
            </li>
          ))}
        </ul>
      </section>

      <section className="auth-panel">
        <ThemeToggle className="auth-theme" />
        <div className="auth-card">{children}</div>
      </section>
    </div>
  );
}

export default AuthLayout;
