import { useState } from "react";
import { FaMoon, FaSun } from "react-icons/fa";
import { applyTheme } from "../theme";

function ThemeToggle({ className = "" }) {
  const [theme, setTheme] = useState(
    () => document.documentElement.dataset.theme || "light"
  );

  const toggle = () => {
    const next = theme === "dark" ? "light" : "dark";
    applyTheme(next);
    setTheme(next);
  };

  return (
    <button
      type="button"
      className={`icon-btn ${className}`}
      onClick={toggle}
      aria-label={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
      title={theme === "dark" ? "Light mode" : "Dark mode"}
    >
      {theme === "dark" ? <FaSun /> : <FaMoon />}
    </button>
  );
}

export default ThemeToggle;
