const rupees = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0,
});

export const formatMoney = (value) => rupees.format(Number(value) || 0);

// Dates are stored at midnight UTC, so always show them in UTC.
export const formatDate = (iso) =>
  iso
    ? new Date(iso).toLocaleDateString("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
        timeZone: "UTC",
      })
    : "";

export const toInputDate = (iso) => (iso ? String(iso).slice(0, 10) : "");

export const todayInputDate = () => new Date().toISOString().slice(0, 10);

/** "2026-03" -> "Mar 2026" */
export const monthLabel = (key) => {
  const [year, month] = key.split("-").map(Number);
  return new Date(Date.UTC(year, month - 1, 1)).toLocaleDateString("en-IN", {
    month: "short",
    year: "numeric",
    timeZone: "UTC",
  });
};
