import { FaExclamationCircle, FaExclamationTriangle } from "react-icons/fa";

/** Budget warnings from the API. level is "WARNING" or "DANGER". */
function Alerts({ alerts }) {
  if (!alerts?.length) return null;
  return (
    <div className="alerts">
      {alerts.map((alert) => (
        <div key={alert.message} className={`alert alert-${alert.level.toLowerCase()}`} role="alert">
          {alert.level === "DANGER" ? (
            <FaExclamationCircle aria-hidden="true" />
          ) : (
            <FaExclamationTriangle aria-hidden="true" />
          )}
          <span>{alert.message}</span>
        </div>
      ))}
    </div>
  );
}

export default Alerts;
