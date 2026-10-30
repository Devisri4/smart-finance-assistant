import { useState } from "react";
import { toast } from "react-toastify";
import { FaEdit, FaTrash } from "react-icons/fa";
import api, { errorMessage } from "../api/api";
import useFetch from "../hooks/useFetch";
import { CategoryIcon, EmptyState, ErrorBox, Loader, PageHeader, ProgressBar } from "../components/ui";
import { CATEGORIES } from "../utils/categories";
import { formatMoney } from "../utils/format";

const STATUS_TEXT = {
  OK: "On track",
  WARNING: "Almost full",
  OVER: "Over budget",
};

function Budgets() {
  const { data, error, reload } = useFetch("/budgets");
  const [form, setForm] = useState({ category: "Food", monthlyLimit: "" });
  const [saving, setSaving] = useState(false);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);

  if (error) return <ErrorBox message={error} onRetry={reload} />;
  if (!data) return <Loader text="Loading budgets…" />;

  const hasBudget = (category) => data.some((b) => b.category === category);

  const save = async (e) => {
    e.preventDefault();
    if (!(Number(form.monthlyLimit) > 0)) {
      toast.error("Please enter a monthly limit above 0");
      return;
    }
    try {
      setSaving(true);
      await api.put("/budgets", { category: form.category, monthlyLimit: Number(form.monthlyLimit) });
      toast.success(`${form.category} budget saved`);
      setForm({ ...form, monthlyLimit: "" });
      reload();
    } catch (err) {
      toast.error(errorMessage(err, "Failed to save budget"));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (budget) => {
    try {
      await api.delete(`/budgets/${budget.id}`);
      toast.success(`${budget.category} budget removed`);
      reload();
    } catch (err) {
      toast.error(errorMessage(err, "Failed to delete budget"));
    } finally {
      setConfirmDeleteId(null);
    }
  };

  return (
    <>
      <PageHeader
        title="Budgets"
        subtitle="Set a monthly limit for each category. We warn you at 80% and when you go over."
      />

      <section className="card">
        <h3>Set a budget</h3>
        <form className="form-row" onSubmit={save}>
          <label className="field">
            <span>Category</span>
            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {`${c}${hasBudget(c) ? " (change limit)" : ""}`}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Monthly limit (₹)</span>
            <input
              type="number"
              min="1"
              placeholder="5000"
              value={form.monthlyLimit}
              onChange={(e) => setForm({ ...form, monthlyLimit: e.target.value })}
            />
          </label>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? "Saving…" : "Save budget"}
          </button>
        </form>
      </section>

      {data.length === 0 ? (
        <section className="card">
          <EmptyState title="No budgets yet">
            Pick a category above and choose how much you want to spend on it each month.
          </EmptyState>
        </section>
      ) : (
        <div className="budget-grid">
          {data.map((b) => (
            <section key={b.id} className={`card budget-card budget-${b.level.toLowerCase()}`}>
              <div className="row-between">
                <div className="row">
                  <CategoryIcon name={b.category} />
                  <h3>{b.category}</h3>
                </div>
                <span className={`badge badge-${b.level.toLowerCase()}`}>{STATUS_TEXT[b.level]}</span>
              </div>

              <p className="budget-amount">
                <strong>{formatMoney(b.spent)}</strong>
                <span className="muted"> of {formatMoney(b.monthlyLimit)}</span>
              </p>
              <ProgressBar percent={b.percentUsed} level={b.level} />
              <div className="row-between small">
                <span className="muted">{b.percentUsed}% used</span>
                <span className={b.remaining < 0 ? "bad" : "muted"}>
                  {b.remaining < 0
                    ? `${formatMoney(-b.remaining)} over`
                    : `${formatMoney(b.remaining)} left`}
                </span>
              </div>

              <div className="card-actions">
                {confirmDeleteId === b.id ? (
                  <>
                    <span className="small">Remove this budget?</span>
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => remove(b)}>
                      Yes
                    </button>
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => setConfirmDeleteId(null)}>
                      No
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      onClick={() => setForm({ category: b.category, monthlyLimit: String(b.monthlyLimit) })}
                    >
                      <FaEdit aria-hidden="true" /> Change
                    </button>
                    <button type="button" className="btn btn-ghost btn-sm bad" onClick={() => setConfirmDeleteId(b.id)}>
                      <FaTrash aria-hidden="true" /> Remove
                    </button>
                  </>
                )}
              </div>
            </section>
          ))}
        </div>
      )}
    </>
  );
}

export default Budgets;
