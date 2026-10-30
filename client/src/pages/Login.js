import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { FaUserAstronaut } from "react-icons/fa";
import api, { errorMessage } from "../api/api";
import { setToken } from "../auth";
import AuthLayout from "../components/AuthLayout";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(null); // "login", "demo" or null
  const navigate = useNavigate();

  const finish = (token, message) => {
    setToken(token);
    toast.success(message);
    navigate("/dashboard", { replace: true });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      toast.error("Email and password are required");
      return;
    }
    try {
      setBusy("login");
      const res = await api.post("/login", { email: email.trim(), password });
      finish(res.data.token, "Welcome back!");
    } catch (err) {
      toast.error(errorMessage(err, "Failed to log in"));
      setBusy(null);
    }
  };

  const handleDemo = async () => {
    try {
      setBusy("demo");
      const res = await api.post("/demo-login");
      finish(res.data.token, "Welcome to the demo!");
    } catch (err) {
      toast.error(errorMessage(err, "Could not start the demo. Please try again."));
      setBusy(null);
    }
  };

  return (
    <AuthLayout>
      <h2>Log in</h2>
      <p className="muted">Good to see you again.</p>

      <form onSubmit={handleSubmit} className="form">
        <label className="field">
          <span>Email</span>
          <input
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            autoComplete="current-password"
            placeholder="Your password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        <button type="submit" className="btn btn-primary btn-block" disabled={busy !== null}>
          {busy === "login" ? "Logging in…" : "Log in"}
        </button>
      </form>

      <div className="divider">
        <span>or</span>
      </div>

      <button
        type="button"
        className="btn btn-secondary btn-block"
        onClick={handleDemo}
        disabled={busy !== null}
      >
        <FaUserAstronaut aria-hidden="true" />
        {busy === "demo" ? "Preparing the demo…" : "Try the demo, no sign-up needed"}
      </button>
      {busy && (
        <p className="muted small center">
          The free server may need up to a minute to wake up.
        </p>
      )}

      <p className="muted center switch-link">
        New here? <Link to="/signup">Create an account</Link>
      </p>
    </AuthLayout>
  );
}

export default Login;
