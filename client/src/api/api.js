// client/src/api/api.js
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:5000", // change to Render URL after deploy
});

export default api;

