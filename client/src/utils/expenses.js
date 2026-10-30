import { toInputDate } from "./format";

/** Keeps only the expenses that match the search text, category and month. */
export function filterExpenses(expenses, { search = "", category = "All", month = "All" }) {
  const text = search.trim().toLowerCase();
  return expenses.filter(
    (e) =>
      (!text || e.title.toLowerCase().includes(text)) &&
      (category === "All" || e.category === category) &&
      (month === "All" || toInputDate(e.date).startsWith(month))
  );
}

/** The months that have expenses, newest first, like ["2026-03", "2026-02"]. */
export function expenseMonths(expenses) {
  const months = new Set(expenses.map((e) => toInputDate(e.date).slice(0, 7)).filter(Boolean));
  return [...months].sort().reverse();
}

export const sumAmounts = (expenses) =>
  expenses.reduce((sum, e) => sum + Number(e.amount || 0), 0);

export function toCsv(expenses) {
  const header = ["Title", "Amount", "Category", "Date"];
  const rows = expenses.map((e) => [e.title, e.amount, e.category, toInputDate(e.date)]);
  return [header, ...rows]
    .map((row) => row.map((field) => `"${String(field ?? "").replace(/"/g, '""')}"`).join(","))
    .join("\n");
}

export function downloadCsv(expenses, filename = "expenses.csv") {
  const blob = new Blob([toCsv(expenses)], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
