// client/src/pages/Login.js
import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../api/api";
import { toast } from "react-toastify";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!email || !password) {
      toast.error("Email and password are required");
      return;
    }

    try {
      setIsSubmitting(true);
      const res = await api.post("/login", { email, password });
      localStorage.setItem("token", res.data.token);
      toast.success("Logged in successfully");
      navigate("/dashboard");
    } catch (err) {
      const msg =
        err?.response?.data?.message || "Failed to login";
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container">
      <div className="login-box">
        <h2>Smart Finance Assistant</h2>
        <p className="tagline">Track. Plan. Grow your money.</p>

        <form onSubmit={handleSubmit}>
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? "Logging in..." : "Login"}
          </button>
        </form>

        <p className="tagline" style={{ marginTop: 12 }}>
          Don&apos;t have an account?{" "}
          <Link to="/signup" style={{ color: "#60a5fa" }}>
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Login;
