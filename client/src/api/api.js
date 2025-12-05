// client/src/api/api.js
import axios from "axios";

const api = axios.create({
  baseURL: "https://smart-finance-api-qb1c.onrender.com",

});

export default api;

