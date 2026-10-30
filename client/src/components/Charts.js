import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { categoryMeta } from "../utils/categories";
import { formatMoney, monthLabel } from "../utils/format";

const AXIS = { fill: "#94a3b8", fontSize: 12 };
const GRID = "rgba(148, 163, 184, 0.2)";
const TOOLTIP = {
  contentStyle: {
    background: "var(--surface)",
    border: "1px solid var(--border)",
    borderRadius: 10,
    color: "var(--text)",
  },
  formatter: (value) => formatMoney(value),
  cursor: { fill: "rgba(148, 163, 184, 0.12)" },
};

const compactMoney = (v) => (v >= 1000 ? `₹${Math.round(v / 1000)}k` : `₹${v}`);

/** Bars for the last six months. */
export function TrendChart({ data, height = 260 }) {
  const rows = data.map((d) => ({ ...d, label: monthLabel(d.month) }));
  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={rows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid vertical={false} stroke={GRID} />
        <XAxis dataKey="label" tick={AXIS} axisLine={false} tickLine={false} />
        <YAxis tick={AXIS} axisLine={false} tickLine={false} tickFormatter={compactMoney} width={48} />
        <Tooltip {...TOOLTIP} />
        <Bar dataKey="total" name="Spent" fill="#6366f1" radius={[6, 6, 0, 0]} maxBarSize={44} />
      </BarChart>
    </ResponsiveContainer>
  );
}

/** Doughnut of how this month's spending splits across categories. */
export function CategoryDonut({ data, height = 260 }) {
  const rows = data.filter((d) => d.thisMonth > 0);
  return (
    <ResponsiveContainer width="100%" height={height}>
      <PieChart>
        <Pie
          data={rows}
          dataKey="thisMonth"
          nameKey="category"
          innerRadius="58%"
          outerRadius="85%"
          paddingAngle={2}
          stroke="none"
        >
          {rows.map((d) => (
            <Cell key={d.category} fill={categoryMeta(d.category).color} />
          ))}
        </Pie>
        <Tooltip {...TOOLTIP} cursor={false} />
        <Legend iconType="circle" wrapperStyle={{ fontSize: 13 }} />
      </PieChart>
    </ResponsiveContainer>
  );
}

/** Side-by-side bars: this month against last month, per category. */
export function CategoryCompareChart({ data, height = 300 }) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid vertical={false} stroke={GRID} />
        <XAxis dataKey="category" tick={AXIS} axisLine={false} tickLine={false} />
        <YAxis tick={AXIS} axisLine={false} tickLine={false} tickFormatter={compactMoney} width={48} />
        <Tooltip {...TOOLTIP} />
        <Legend iconType="circle" wrapperStyle={{ fontSize: 13 }} />
        <Bar dataKey="lastMonth" name="Last month" fill="#cbd5e1" radius={[6, 6, 0, 0]} maxBarSize={32} />
        <Bar dataKey="thisMonth" name="This month" fill="#6366f1" radius={[6, 6, 0, 0]} maxBarSize={32} />
      </BarChart>
    </ResponsiveContainer>
  );
}
