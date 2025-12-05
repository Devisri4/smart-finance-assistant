// client/src/pages/Signup.js
import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../api/api";
import { toast } from "react-toastify";

function Signup() {
  const [name, setName] = useState("");
  const [occupation, setOccupation] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!name || !email || !password) {
      toast.error("Name, email and password are required");
      return;
    }

    try {
      setIsSubmitting(true);
      await api.post("/register", {
        name,
        occupation,
        email,
        password,
      });

      toast.success("Account created. Please login.");
      navigate("/login");
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        "Failed to create account";
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container">
      <div className="login-box">
        <h2>Create your account</h2>
        <p className="tagline">
          Enter your details to start tracking expenses.
        </p>

        <form onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Full name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          <input
            type="text"
            placeholder="Occupation (student, developer...)"
            value={occupation}
            onChange={(e) => setOccupation(e.target.value)}
          />

          <input
            type="email"
            placeholder="Email address"
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
            {isSubmitting ? "Creating account..." : "Sign up"}
          </button>
        </form>

        <p className="tagline" style={{ marginTop: 12 }}>
          Already have an account?{" "}
          <Link to="/login" style={{ color: "#60a5fa" }}>
            Login
          </Link>
        </p>
      </div>
    </div>
  );
}

export default Signup;
