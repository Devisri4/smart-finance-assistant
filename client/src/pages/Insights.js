import { FaLightbulb } from "react-icons/fa";
import useFetch from "../hooks/useFetch";
import Alerts from "../components/Alerts";
import { CategoryCompareChart, TrendChart } from "../components/Charts";
import { CategoryIcon, EmptyState, ErrorBox, Loader, PageHeader, StatCard } from "../components/ui";
import { formatMoney, monthLabel } from "../utils/format";

function Insights() {
  const { data, error, reload } = useFetch("/insights");

  if (error) return <ErrorBox message={error} onRetry={reload} />;
  if (!data) return <Loader text="Working out your insights…" />;

  const hasData = data.monthlyTrend.some((m) => m.total > 0);

  return (
    <>
      <PageHeader
        title="Insights"
        subtitle={`What your spending says about ${monthLabel(data.month)}.`}
      />

      <Alerts alerts={data.alerts} />

      <section className="card">
        <h3>
          <FaLightbulb aria-hidden="true" className="accent" /> Smart tips
        </h3>
        <ul className="tips">
          {data.tips.map((tip) => (
            <li key={tip}>{tip}</li>
          ))}
        </ul>
      </section>

      <div className="stat-grid">
        <StatCard
          label="Top category"
          value={data.topCategory || "-"}
          hint={data.topCategory ? "Biggest share this month" : "No spending yet"}
        />
        <StatCard
          label="Compared with last month"
          value={data.changePercent === null ? "-" : `${data.changePercent > 0 ? "+" : ""}${data.changePercent}%`}
          hint="At the same day of the month"
          tone={data.changePercent > 0 ? "bad" : data.changePercent < 0 ? "good" : ""}
        />
        <StatCard label="Daily average" value={formatMoney(data.dailyAverage)} />
        <StatCard label="Projected month end" value={formatMoney(data.projectedMonthTotal)} />
      </div>

      {hasData ? (
        <div className="grid-2">
          <section className="card">
            <h3>This month vs. last month</h3>
            <CategoryCompareChart data={data.categories} />
          </section>
          <section className="card">
            <h3>Monthly trend</h3>
            <TrendChart data={data.monthlyTrend} height={300} />
          </section>
        </div>
      ) : (
        <section className="card">
          <EmptyState title="Not enough data yet">Add a few expenses to unlock charts.</EmptyState>
        </section>
      )}

      {data.categories.length > 0 && (
        <section className="card">
          <h3>Category breakdown</h3>
          <ul className="mini-list">
            {data.categories.map((c) => {
              const diff = c.thisMonth - c.lastMonth;
              return (
                <li key={c.category}>
                  <CategoryIcon name={c.category} />
                  <div className="grow">
                    <strong>{c.category}</strong>
                    <span className="muted small">Last month {formatMoney(c.lastMonth)}</span>
                  </div>
                  <div className="right">
                    <strong>{formatMoney(c.thisMonth)}</strong>
                    {c.lastMonth > 0 && diff !== 0 && (
                      <span className={`small ${diff > 0 ? "bad" : "good"}`}>
                        {diff > 0 ? "▲" : "▼"} {formatMoney(Math.abs(diff))}
                      </span>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        </section>
      )}
    </>
  );
}

export default Insights;
