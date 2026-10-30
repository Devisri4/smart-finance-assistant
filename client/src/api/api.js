import axios from "axios";
import { clearToken, getToken } from "../auth";

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || "http://localhost:5000",
});

// Send the login token with every request.
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// If the token is no longer accepted, go back to the login page.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && getToken()) {
      clearToken();
      window.location.assign("/login");
    }
    return Promise.reject(error);
  }
);

export const errorMessage = (err, fallback) =>
  err?.response?.data?.message || fallback;

export default api;
