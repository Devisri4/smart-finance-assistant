import { Link, useOutletContext } from "react-router-dom";
import { FaArrowRight, FaLightbulb } from "react-icons/fa";
import useFetch from "../hooks/useFetch";
import Alerts from "../components/Alerts";
import { CategoryDonut, TrendChart } from "../components/Charts";
import {
  CategoryIcon,
  EmptyState,
  ErrorBox,
  Loader,
  PageHeader,
  ProgressBar,
  StatCard,
} from "../components/ui";
import { formatDate, formatMoney, monthLabel } from "../utils/format";

const greeting = () => {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 18) return "Good afternoon";
  return "Good evening";
};

function Dashboard() {
  const { user } = useOutletContext();
  const insights = useFetch("/insights");
  const expenses = useFetch("/expenses");

  if (insights.error || expenses.error) {
    return (
      <ErrorBox
        message={insights.error || expenses.error}
        onRetry={() => {
          insights.reload();
          expenses.reload();
        }}
      />
    );
  }
  if (!insights.data || !expenses.data) return <Loader text="Loading your dashboard…" />;

  const data = insights.data;
  const recent = expenses.data.slice(0, 5);
  const change = data.changePercent;

  return (
    <>
      <PageHeader
        title={`${greeting()}, ${user.name.split(" ")[0]}`}
        subtitle={`Here is your money in ${monthLabel(data.month)}.`}
      >
        <Link to="/expenses" className="btn btn-primary">
          + Add expense
        </Link>
      </PageHeader>

      <Alerts alerts={data.alerts} />

      <div className="stat-grid">
        <StatCard
          label="Spent this month"
          value={formatMoney(data.totalThisMonth)}
          hint={
            change === null
              ? "No data from last month yet"
              : `${change > 0 ? "▲" : "▼"} ${Math.abs(change)}% vs. same time last month`
          }
          tone={change === null ? "" : change > 0 ? "bad" : "good"}
        />
        <StatCard label="Last month" value={formatMoney(data.totalLastMonth)} />
        <StatCard label="Daily average" value={formatMoney(data.dailyAverage)} hint="This month" />
        <StatCard
          label="Projected month end"
          value={formatMoney(data.projectedMonthTotal)}
          hint="If you keep this pace"
        />
      </div>

      <div className="grid-2">
        <section className="card">
          <h3>Last 6 months</h3>
          {data.monthlyTrend.some((m) => m.total > 0) ? (
            <TrendChart data={data.monthlyTrend} />
          ) : (
            <EmptyState title="No spending yet">Add expenses to see your trend.</EmptyState>
          )}
        </section>
        <section className="card">
          <h3>Where it goes this month</h3>
          {data.totalThisMonth > 0 ? (
            <CategoryDonut data={data.categories} />
          ) : (
            <EmptyState title="Nothing this month yet">Your category split will appear here.</EmptyState>
          )}
        </section>
      </div>

      <div className="grid-2">
        <section className="card">
          <div className="card-header">
            <h3>Recent expenses</h3>
            <Link to="/expenses" className="link">
              View all <FaArrowRight aria-hidden="true" />
            </Link>
          </div>
          {recent.length === 0 ? (
            <EmptyState title="No expenses yet">Your latest spending will show up here.</EmptyState>
          ) : (
            <ul className="mini-list">
              {recent.map((e) => (
                <li key={e.id}>
                  <CategoryIcon name={e.category} />
                  <div className="grow">
                    <strong>{e.title}</strong>
                    <span className="muted small">
                      {e.category} · {formatDate(e.date)}
                    </span>
                  </div>
                  <strong>{formatMoney(e.amount)}</strong>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card">
          <div className="card-header">
            <h3>Budgets</h3>
            <Link to="/budgets" className="link">
              Manage <FaArrowRight aria-hidden="true" />
            </Link>
          </div>
          {data.budgets.length === 0 ? (
            <EmptyState title="No budgets yet">
              Set a monthly limit per category and we will warn you before you go over.
            </EmptyState>
          ) : (
            <ul className="budget-mini">
              {data.budgets.map((b) => (
                <li key={b.id}>
                  <div className="row-between">
                    <span>{b.category}</span>
                    <span className="muted small">
                      {formatMoney(b.spent)} / {formatMoney(b.monthlyLimit)}
                    </span>
                  </div>
                  <ProgressBar percent={b.percentUsed} level={b.level} />
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section className="card">
        <div className="card-header">
          <h3>
            <FaLightbulb aria-hidden="true" className="accent" /> Smart insights
          </h3>
          <Link to="/insights" className="link">
            See more <FaArrowRight aria-hidden="true" />
          </Link>
        </div>
        <ul className="tips">
          {data.tips.slice(0, 3).map((tip) => (
            <li key={tip}>{tip}</li>
          ))}
        </ul>
      </section>
    </>
  );
}

export default Dashboard;
