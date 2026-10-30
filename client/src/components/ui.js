// Small building blocks used on several pages.
import { categoryMeta } from "../utils/categories";

export function Loader({ text = "Loading…" }) {
  return (
    <div className="loader" role="status">
      <span className="spinner" aria-hidden="true" />
      {text}
    </div>
  );
}

export function ErrorBox({ message, onRetry }) {
  return (
    <div className="card error-box">
      <p>{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title, children }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      {children && <p>{children}</p>}
    </div>
  );
}

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {children && <div className="page-actions">{children}</div>}
    </div>
  );
}

export function StatCard({ label, value, hint, tone }) {
  return (
    <div className="card stat-card">
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
      {hint && <span className={`stat-hint ${tone || ""}`}>{hint}</span>}
    </div>
  );
}

export function CategoryChip({ name }) {
  const { Icon, color } = categoryMeta(name);
  return (
    <span className="chip" style={{ "--chip": color }}>
      <Icon aria-hidden="true" />
      {name}
    </span>
  );
}

export function CategoryIcon({ name }) {
  const { Icon, color } = categoryMeta(name);
  return (
    <span className="category-icon" style={{ "--chip": color }} aria-hidden="true">
      <Icon />
    </span>
  );
}

/** level is "OK", "WARNING" or "OVER", as sent by the API. */
export function ProgressBar({ percent, level = "OK" }) {
  const width = Math.min(Math.max(percent, 0), 100);
  return (
    <div
      className="progress"
      role="progressbar"
      aria-valuenow={Math.round(percent)}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <div className={`progress-fill level-${level.toLowerCase()}`} style={{ width: `${width}%` }} />
    </div>
  );
}
