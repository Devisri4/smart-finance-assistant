import { expenseMonths, filterExpenses, sumAmounts, toCsv } from "./expenses";

const expenses = [
  { id: "1", title: "Groceries", amount: 1200, category: "Food", date: "2026-03-10T00:00:00Z" },
  { id: "2", title: "Cab to office", amount: 300, category: "Transport", date: "2026-03-02T00:00:00Z" },
  { id: "3", title: "Dinner", amount: 900, category: "Food", date: "2026-02-20T00:00:00Z" },
];

test("filters by search text, ignoring capital letters", () => {
  expect(filterExpenses(expenses, { search: "GROC" }).map((e) => e.id)).toEqual(["1"]);
});

test("filters by category and month together", () => {
  const result = filterExpenses(expenses, { category: "Food", month: "2026-02" });
  expect(result.map((e) => e.id)).toEqual(["3"]);
});

test("lists months newest first without repeats", () => {
  expect(expenseMonths(expenses)).toEqual(["2026-03", "2026-02"]);
});

test("adds up amounts", () => {
  expect(sumAmounts(expenses)).toBe(2400);
});

test("CSV has a header and escapes quotes", () => {
  const csv = toCsv([{ title: 'The "big" shop', amount: 50, category: "Shopping", date: "2026-03-01T00:00:00Z" }]);
  expect(csv).toBe('"Title","Amount","Category","Date"\n"The ""big"" shop","50","Shopping","2026-03-01"');
});
