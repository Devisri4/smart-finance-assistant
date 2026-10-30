import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import api, { errorMessage } from "../api/api";
import AuthLayout from "../components/AuthLayout";

function Signup() {
  const [form, setForm] = useState({ name: "", occupation: "", email: "", password: "" });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.name.trim() || !form.email.trim() || !form.password) {
      toast.error("Name, email and password are required");
      return;
    }
    if (form.password.length < 6) {
      toast.error("Password must be at least 6 characters");
      return;
    }
    try {
      setIsSubmitting(true);
      await api.post("/register", {
        name: form.name.trim(),
        occupation: form.occupation.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      toast.success("Account created. Please log in.");
      navigate("/login");
    } catch (err) {
      toast.error(errorMessage(err, "Failed to create account"));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthLayout>
      <h2>Create your account</h2>
      <p className="muted">It takes less than a minute.</p>

      <form onSubmit={handleSubmit} className="form">
        <label className="field">
          <span>Full name</span>
          <input autoComplete="name" placeholder="Devi Sri" value={form.name} onChange={update("name")} />
        </label>
        <label className="field">
          <span>Occupation (optional)</span>
          <input
            placeholder="Student, developer…"
            value={form.occupation}
            onChange={update("occupation")}
          />
        </label>
        <label className="field">
          <span>Email</span>
          <input
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            value={form.email}
            onChange={update("email")}
          />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            autoComplete="new-password"
            placeholder="At least 6 characters"
            value={form.password}
            onChange={update("password")}
          />
        </label>
        <button type="submit" className="btn btn-primary btn-block" disabled={isSubmitting}>
          {isSubmitting ? "Creating account…" : "Sign up"}
        </button>
      </form>

      <p className="muted center switch-link">
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </AuthLayout>
  );
}

export default Signup;
