import { Navigate } from "react-router-dom";
import { getToken } from "../auth";

const ProtectedRoute = ({ children }) =>
  getToken() ? children : <Navigate to="/login" replace />;

export default ProtectedRoute;
