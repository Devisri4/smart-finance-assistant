import { useMemo, useState } from "react";
import { toast } from "react-toastify";
import { FaCheck, FaDownload, FaEdit, FaSearch, FaTimes, FaTrash } from "react-icons/fa";
import api, { errorMessage } from "../api/api";
import useFetch from "../hooks/useFetch";
import { CategoryChip, EmptyState, ErrorBox, Loader, PageHeader } from "../components/ui";
import { CATEGORIES } from "../utils/categories";
import { downloadCsv, expenseMonths, filterExpenses, sumAmounts } from "../utils/expenses";
import { formatDate, formatMoney, monthLabel, toInputDate, todayInputDate } from "../utils/format";

const emptyForm = () => ({ title: "", amount: "", category: "Food", date: todayInputDate() });

const toPayload = (form) => ({
  title: form.title.trim(),
  amount: Number(form.amount),
  category: form.category,
  date: form.date || null,
});

const isValid = (form) => form.title.trim() && Number(form.amount) > 0 && form.category;

function ExpenseFields({ form, onChange, compact = false }) {
  const set = (field) => (e) => onChange({ ...form, [field]: e.target.value });
  return (
    <>
      <label className="field">
        {!compact && <span>Title</span>}
        <input placeholder="Groceries, rent…" value={form.title} onChange={set("title")} aria-label="Title" />
      </label>
      <label className="field">
        {!compact && <span>Amount (₹)</span>}
        <input
          type="number"
          min="0"
          step="0.01"
          placeholder="0"
          value={form.amount}
          onChange={set("amount")}
          aria-label="Amount"
        />
      </label>
      <label className="field">
        {!compact && <span>Category</span>}
        <select value={form.category} onChange={set("category")} aria-label="Category">
          {CATEGORIES.map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
      </label>
      <label className="field">
        {!compact && <span>Date</span>}
        <input type="date" value={form.date} onChange={set("date")} aria-label="Date" />
      </label>
    </>
  );
}

function Expenses() {
  const { data, error, reload, setData } = useFetch("/expenses");
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [filters, setFilters] = useState({ search: "", category: "All", month: "All" });
  const [editing, setEditing] = useState(null); // { id, form }
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);

  const expenses = useMemo(() => data || [], [data]);
  const months = useMemo(() => expenseMonths(expenses), [expenses]);
  const visible = useMemo(() => filterExpenses(expenses, filters), [expenses, filters]);

  if (error) return <ErrorBox message={error} onRetry={reload} />;
  if (!data) return <Loader text="Loading expenses…" />;

  const addExpense = async (e) => {
    e.preventDefault();
    if (!isValid(form)) {
      toast.error("Please fill in a title and an amount above 0");
      return;
    }
    try {
      setSaving(true);
      const res = await api.post("/expenses", toPayload(form));
      setData((list) => [res.data, ...list].sort((a, b) => b.date.localeCompare(a.date)));
      setForm(emptyForm());
      toast.success("Expense added");
    } catch (err) {
      toast.error(errorMessage(err, "Failed to add expense"));
    } finally {
      setSaving(false);
    }
  };

  const saveEdit = async () => {
    if (!isValid(editing.form)) {
      toast.error("Please fill in a title and an amount above 0");
      return;
    }
    try {
      const res = await api.patch(`/expenses/${editing.id}`, toPayload(editing.form));
      setData((list) => list.map((x) => (x.id === editing.id ? res.data : x)));
      setEditing(null);
      toast.success("Expense updated");
    } catch (err) {
      toast.error(errorMessage(err, "Failed to update expense"));
    }
  };

  const deleteExpense = async (id) => {
    try {
      await api.delete(`/expenses/${id}`);
      setData((list) => list.filter((x) => x.id !== id));
      toast.success("Expense deleted");
    } catch (err) {
      toast.error(errorMessage(err, "Failed to delete expense"));
    } finally {
      setConfirmDeleteId(null);
    }
  };

  const exportCsv = () => {
    if (!visible.length) {
      toast.info("No expenses to export");
      return;
    }
    downloadCsv(visible);
  };

  const setFilter = (field) => (e) => setFilters({ ...filters, [field]: e.target.value });

  return (
    <>
      <PageHeader title="Expenses" subtitle="Add, search and manage everything you spend." />

      <section className="card">
        <h3>Add an expense</h3>
        <form className="form-row" onSubmit={addExpense}>
          <ExpenseFields form={form} onChange={setForm} />
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? "Adding…" : "+ Add"}
          </button>
        </form>
      </section>

      <section className="card">
        <div className="toolbar">
          <label className="search">
            <FaSearch aria-hidden="true" />
            <input
              placeholder="Search by title"
              value={filters.search}
              onChange={setFilter("search")}
              aria-label="Search by title"
            />
          </label>
          <select value={filters.category} onChange={setFilter("category")} aria-label="Filter by category">
            <option value="All">All categories</option>
            {CATEGORIES.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
          <select value={filters.month} onChange={setFilter("month")} aria-label="Filter by month">
            <option value="All">All months</option>
            {months.map((m) => (
              <option key={m} value={m}>
                {monthLabel(m)}
              </option>
            ))}
          </select>
          <button type="button" className="btn btn-secondary" onClick={exportCsv}>
            <FaDownload aria-hidden="true" /> Export CSV
          </button>
        </div>

        <p className="muted small">
          {visible.length} expense{visible.length === 1 ? "" : "s"} · {formatMoney(sumAmounts(visible))}
        </p>

        {visible.length === 0 ? (
          <EmptyState title={expenses.length ? "No matches" : "No expenses yet"}>
            {expenses.length
              ? "Try a different search or filter."
              : "Add your first expense above to get started."}
          </EmptyState>
        ) : (
          <ul className="expense-list">
            {visible.map((e) =>
              editing?.id === e.id ? (
                <li key={e.id} className="expense-item editing">
                  <ExpenseFields
                    compact
                    form={editing.form}
                    onChange={(f) => setEditing({ ...editing, form: f })}
                  />
                  <div className="row-actions">
                    <button type="button" className="icon-btn good" onClick={saveEdit} aria-label="Save">
                      <FaCheck />
                    </button>
                    <button type="button" className="icon-btn" onClick={() => setEditing(null)} aria-label="Cancel">
                      <FaTimes />
                    </button>
                  </div>
                </li>
              ) : (
                <li key={e.id} className="expense-item">
                  <div className="grow">
                    <strong>{e.title}</strong>
                    <span className="muted small">{formatDate(e.date)}</span>
                  </div>
                  <CategoryChip name={e.category} />
                  <strong className="amount">{formatMoney(e.amount)}</strong>
                  <div className="row-actions">
                    {confirmDeleteId === e.id ? (
                      <>
                        <span className="small">Delete?</span>
                        <button type="button" className="btn btn-danger btn-sm" onClick={() => deleteExpense(e.id)}>
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
                          className="icon-btn"
                          aria-label={`Edit ${e.title}`}
                          onClick={() =>
                            setEditing({
                              id: e.id,
                              form: {
                                title: e.title,
                                amount: String(e.amount),
                                category: e.category,
                                date: toInputDate(e.date),
                              },
                            })
                          }
                        >
                          <FaEdit />
                        </button>
                        <button
                          type="button"
                          className="icon-btn bad"
                          aria-label={`Delete ${e.title}`}
                          onClick={() => setConfirmDeleteId(e.id)}
                        >
                          <FaTrash />
                        </button>
                      </>
                    )}
                  </div>
                </li>
              )
            )}
          </ul>
        )}
      </section>
    </>
  );
}

export default Expenses;
