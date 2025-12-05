// client/src/pages/Dashboard.js
import { useEffect, useState } from "react";
import api from "../api/api";
import { toast } from "react-toastify";
import {
  FaUtensils,
  FaCar,
  FaFileInvoice,
  FaShoppingBag,
  FaFilm,
  FaQuestionCircle,
  FaTrash,
  FaEdit,
  FaCheck,
  FaTimes,
} from "react-icons/fa";
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

function Dashboard() {
  /* ================= STATE ================= */
  const [user, setUser] = useState(null);
  const [expenses, setExpenses] = useState([]);

  const [title, setTitle] = useState("");
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState("Food");
  const [date, setDate] = useState("");
  const [isAdding, setIsAdding] = useState(false);

  // EDIT STATE
  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({
    title: "",
    amount: "",
    category: "Food",
    date: "",
  });

  const token = localStorage.getItem("token");

  /* ================= LOAD DATA ================= */
  useEffect(() => {
    if (!token) {
      handleLogout();
      return;
    }

    api
      .get("/profile", {
        headers: { Authorization: `Bearer ${token}` },
      })
      .then((res) => setUser(res.data))
      .catch(() => {
        toast.error("Session expired. Please login again.");
        handleLogout();
      });

    api
      .get("/expenses", {
        headers: { Authorization: `Bearer ${token}` },
      })
      .then((res) => setExpenses(res.data))
      .catch(() => toast.error("Failed to load expenses"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /* ================= ACTIONS ================= */
  const handleLogout = () => {
    localStorage.removeItem("token");
    window.location.reload();
  };

  const handleAddExpense = async (e) => {
    e.preventDefault();

    if (!title || !amount || !category) {
      toast.error("All fields are required");
      return;
    }

    try {
      setIsAdding(true);

      const res = await api.post(
        "/expenses",
        {
          title,
          amount: Number(amount),
          category,
          date,
        },
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      setExpenses((prev) => [res.data, ...prev]);

      setTitle("");
      setAmount("");
      setCategory("Food");
      setDate("");

      toast.success("Expense added");
    } catch {
      toast.error("Failed to add expense");
    } finally {
      setIsAdding(false);
    }
  };

  // DELETE with toast confirmation
  const confirmDeleteExpense = (id) => {
    toast.warn(
      ({ closeToast }) => (
        <div>
          <p style={{ marginBottom: 8 }}>Delete this expense?</p>
          <div style={{ display: "flex", gap: 10 }}>
            <button
              onClick={async () => {
                try {
                  await api.delete(`/expenses/${id}`, {
                    headers: { Authorization: `Bearer ${token}` },
                  });

                  setExpenses((prev) =>
                    prev.filter((e) => e._id !== id)
                  );

                  toast.success("Expense deleted");
                } catch {
                  toast.error("Failed to delete expense");
                }
                closeToast();
              }}
              style={{
                background: "#ef4444",
                color: "#fff",
                border: "none",
                padding: "6px 12px",
                borderRadius: 6,
                cursor: "pointer",
              }}
            >
              Delete
            </button>

            <button
              onClick={closeToast}
              style={{
                background: "#334155",
                color: "#fff",
                border: "none",
                padding: "6px 12px",
                borderRadius: 6,
                cursor: "pointer",
              }}
            >
              Cancel
            </button>
          </div>
        </div>
      ),
      { autoClose: false, closeOnClick: false }
    );
  };

  // START EDIT
  const startEdit = (expense) => {
    setEditingId(expense._id);
    setEditData({
      title: expense.title,
      amount: expense.amount,
      category: expense.category,
      date: expense.date ? expense.date.slice(0, 10) : "",
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
  };

  const saveEdit = async (id) => {
    if (!editData.title || !editData.amount || !editData.category) {
      toast.error("All fields are required");
      return;
    }

    try {
      const res = await api.patch(
        `/expenses/${id}`,
        {
          title: editData.title,
          amount: Number(editData.amount),
          category: editData.category,
          date: editData.date,
        },
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      setExpenses((prev) =>
        prev.map((e) => (e._id === id ? res.data : e))
      );
      setEditingId(null);
      toast.success("Expense updated");
    } catch {
      toast.error("Failed to update expense");
    }
  };

  // CSV EXPORT (all expenses)
  const handleExportCSV = () => {
    if (!expenses.length) {
      toast.info("No expenses to export");
      return;
    }

    const headers = ["Title", "Amount", "Category", "Date"];

    const rows = expenses.map((e) => [
      e.title || "",
      e.amount ?? "",
      e.category || "",
      e.date ? new Date(e.date).toISOString().slice(0, 10) : "",
    ]);

    const csvContent = [headers, ...rows]
      .map((row) =>
        row
          .map((field) =>
            `"${String(field).replace(/"/g, '""')}"`
          )
          .join(",")
      )
      .join("\n");

    const blob = new Blob([csvContent], {
      type: "text/csv;charset=utf-8;",
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", "expenses.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  /* ================= CALCULATIONS ================= */
  const totalSpent = expenses.reduce(
    (sum, e) => sum + Number(e.amount),
    0
  );

  const now = new Date();
  const thisMonthSpent = expenses
    .filter((e) => {
      if (!e.date) return false;
      const d = new Date(e.date);
      return (
        d.getMonth() === now.getMonth() &&
        d.getFullYear() === now.getFullYear()
      );
    })
    .reduce((sum, e) => sum + Number(e.amount), 0);

  const categoryTotals = expenses.reduce((acc, e) => {
    acc[e.category] = (acc[e.category] || 0) + Number(e.amount);
    return acc;
  }, {});

  const categoryMeta = {
    Food: { icon: <FaUtensils />, color: "#22c55e" },
    Transport: { icon: <FaCar />, color: "#38bdf8" },
    Bills: { icon: <FaFileInvoice />, color: "#facc15" },
    Shopping: { icon: <FaShoppingBag />, color: "#a78bfa" },
    Entertainment: { icon: <FaFilm />, color: "#fb7185" },
    Other: { icon: <FaQuestionCircle />, color: "#94a3b8" },
  };

  const categoryBarData = Object.entries(categoryTotals).map(
    ([k, v]) => ({ category: k, total: v })
  );

  const maxAmount = Math.max(
    ...categoryBarData.map((c) => c.total),
    1
  );

  const pieData = Object.entries(categoryTotals).map(([k, v]) => ({
    name: k,
    value: v,
  }));

  /* ================= RENDER ================= */
  if (!user) {
    return (
      <div className="loading-screen">
        Loading dashboard…
      </div>
    );
  }

  return (
    <div className="dashboard">
      {/* TOP BAR */}
      <div className="top-bar">
        <div>
          <h2>
            Welcome, {user.name} <span>👋</span>
          </h2>
          <p>Your personal finance assistant dashboard</p>
        </div>
        <button className="logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </div>

      {/* MAIN GRID: ADD + LIST */}
      <div className="main-grid">
        {/* ADD EXPENSE */}
        <div className="card">
          <h3>Add Expense</h3>

          <form onSubmit={handleAddExpense}>
            <label>Title</label>
            <input
              placeholder="Groceries, Rent..."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />

            <label>Amount (₹)</label>
            <input
              type="number"
              placeholder="Amount"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              min="0"
            />

            <label>Category</label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              <option>Food</option>
              <option>Transport</option>
              <option>Bills</option>
              <option>Shopping</option>
              <option>Entertainment</option>
              <option>Other</option>
            </select>

            <label>Date</label>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />

            <button
              className="primary-btn"
              disabled={isAdding || !title || !amount}
            >
              {isAdding ? "Adding..." : "+ Add Expense"}
            </button>
          </form>
        </div>

        {/* RECENT EXPENSES */}
        <div className="card">
          <h3>Recent Expenses</h3>

          {expenses.length === 0 ? (
            <p className="muted">
              No expenses yet. Add your first expense on the
              left to see your spending here.
            </p>
          ) : (
            <>
              {expenses.map((e) => (
                <div key={e._id} className="expense-row">
                  {editingId === e._id ? (
                    <>
                      {/* EDIT MODE */}
                      <div className="edit-left">
                        <input
                          className="edit-input"
                          value={editData.title}
                          onChange={(ev) =>
                            setEditData({
                              ...editData,
                              title: ev.target.value,
                            })
                          }
                        />
                        <select
                          className="edit-input"
                          value={editData.category}
                          onChange={(ev) =>
                            setEditData({
                              ...editData,
                              category: ev.target.value,
                            })
                          }
                        >
                          <option>Food</option>
                          <option>Transport</option>
                          <option>Bills</option>
                          <option>Shopping</option>
                          <option>Entertainment</option>
                          <option>Other</option>
                        </select>
                      </div>

                      <input
                        type="number"
                        className="edit-amount"
                        value={editData.amount}
                        onChange={(ev) =>
                          setEditData({
                            ...editData,
                            amount: ev.target.value,
                          })
                        }
                      />

                      <div className="edit-actions">
                        <button
                          type="button"
                          className="edit-save"
                          onClick={() => saveEdit(e._id)}
                        >
                          <FaCheck />
                        </button>
                        <button
                          type="button"
                          className="edit-cancel"
                          onClick={cancelEdit}
                        >
                          <FaTimes />
                        </button>
                      </div>
                    </>
                  ) : (
                    <>
                      {/* VIEW MODE */}
                      <div>
                        <strong>{e.title}</strong>
                        <span className="expense-category">
                          {e.category}
                        </span>
                      </div>

                      <div className="expense-actions">
                        <span className="amount">
                          ₹{e.amount}
                        </span>

                        <button
                          className="icon-btn"
                          title="Edit"
                          onClick={() => startEdit(e)}
                        >
                          <FaEdit />
                        </button>

                        <button
                          className="icon-btn delete"
                          title="Delete"
                          onClick={() =>
                            confirmDeleteExpense(e._id)
                          }
                        >
                          <FaTrash />
                        </button>
                      </div>
                    </>
                  )}
                </div>
              ))}

              {/* CSV EXPORT BUTTON */}
              <button
                type="button"
                className="primary-btn"
                style={{ marginTop: 16 }}
                onClick={handleExportCSV}
              >
                Export CSV
              </button>
            </>
          )}
        </div>
      </div>

      {/* SUMMARY CARDS */}
      <div className="summary-grid">
        <div className="summary-card">
          <p>Total Spent</p>
          <h3>₹{totalSpent}</h3>
        </div>
        <div className="summary-card">
          <p>This Month</p>
          <h3>₹{thisMonthSpent}</h3>
        </div>
        <div className="summary-card">
          <p>Transactions</p>
          <h3>{expenses.length}</h3>
        </div>
      </div>

      {/* PIE CHART */}
      <div className="card" style={{ marginTop: 24 }}>
        <h3>Spending Overview</h3>

        {pieData.length === 0 ? (
          <p className="muted">
            Not enough data yet to show chart. Add a few
            expenses to see your spending distribution.
          </p>
        ) : (
          <div style={{ height: 280 }}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieData}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  label
                >
                  {pieData.map((entry) => (
                    <Cell
                      key={entry.name}
                      fill={
                        categoryMeta[entry.name]?.color ||
                        categoryMeta.Other.color
                      }
                    />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      {/* CATEGORY BARS */}
      <div className="category-card">
        <h3>Spending by Category</h3>

        {categoryBarData.length === 0 ? (
          <p className="muted">
            Add a few expenses to see category breakdown.
          </p>
        ) : (
          categoryBarData.map((c) => {
            const meta =
              categoryMeta[c.category] || categoryMeta.Other;
            return (
              <div key={c.category} className="category-row">
                <div className="category-header">
                  <div className="category-left">
                    <span style={{ color: meta.color }}>
                      {meta.icon}
                    </span>
                    <span>{c.category}</span>
                  </div>
                  <strong>₹{c.total}</strong>
                </div>

                <div className="progress-bar">
                  <div
                    className="progress-fill"
                    style={{
                      width: `${
                        (c.total / maxAmount) * 100
                      }%`,
                      background: meta.color,
                    }}
                  />
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

export default Dashboard;
