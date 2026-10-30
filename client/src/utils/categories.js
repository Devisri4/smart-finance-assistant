import {
  FaCar,
  FaFileInvoice,
  FaFilm,
  FaQuestionCircle,
  FaShoppingBag,
  FaUtensils,
} from "react-icons/fa";

export const CATEGORIES = [
  "Food",
  "Transport",
  "Bills",
  "Shopping",
  "Entertainment",
  "Other",
];

const META = {
  Food: { Icon: FaUtensils, color: "#10b981" },
  Transport: { Icon: FaCar, color: "#0ea5e9" },
  Bills: { Icon: FaFileInvoice, color: "#f59e0b" },
  Shopping: { Icon: FaShoppingBag, color: "#8b5cf6" },
  Entertainment: { Icon: FaFilm, color: "#f43f5e" },
  Other: { Icon: FaQuestionCircle, color: "#64748b" },
};

export const categoryMeta = (name) => META[name] || META.Other;
