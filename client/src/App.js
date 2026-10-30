import "./App.css";
import { Navigate, Route, Routes } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

import { getToken } from "./auth";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import Budgets from "./pages/Budgets";
import Dashboard from "./pages/Dashboard";
import Expenses from "./pages/Expenses";
import Insights from "./pages/Insights";
import Login from "./pages/Login";
import Signup from "./pages/Signup";

const Home = () => <Navigate to={getToken() ? "/dashboard" : "/login"} replace />;

function App() {
  return (
    <>
      <ToastContainer position="top-center" autoClose={2500} theme="colored" />

      <Routes>
        <Route path="/login" element={getToken() ? <Navigate to="/dashboard" replace /> : <Login />} />
        <Route path="/signup" element={<Signup />} />

        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/expenses" element={<Expenses />} />
          <Route path="/budgets" element={<Budgets />} />
          <Route path="/insights" element={<Insights />} />
        </Route>

        <Route path="*" element={<Home />} />
      </Routes>
    </>
  );
}

export default App;
