import { formatDate, formatMoney, monthLabel, toInputDate } from "./format";

test("formats rupees the Indian way", () => {
  expect(formatMoney(123456)).toBe("₹1,23,456");
  expect(formatMoney("abc")).toBe("₹0");
});

test("shows dates in UTC so they never move by a day", () => {
  expect(formatDate("2026-03-01T00:00:00Z")).toBe("1 Mar 2026");
  expect(toInputDate("2026-03-01T00:00:00Z")).toBe("2026-03-01");
});

test("turns a month key into a label", () => {
  expect(monthLabel("2026-03")).toBe("Mar 2026");
});
