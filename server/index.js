// server/index.js
require("dotenv").config();
const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");

const User = require("./models/User");
const Expense = require("./models/Expense");
const auth = require("./middlewares/auth");

const app = express();

// ===== MIDDLEWARES =====
app.use(
  cors({
    origin: "*", // later restrict to your frontend
  })
);
app.use(express.json());

// ===== DB CONNECTION =====
const MONGO_URI = process.env.MONGO_URI || "mongodb://127.0.0.1:27017/finance";
mongoose
  .connect(MONGO_URI)
  .then(() => console.log("MongoDB connected"))
  .catch((err) => {
    console.error("MongoDB connection error", err);
    process.exit(1);
  });

// ===== AUTH ROUTES =====

// Register
app.post("/register", async (req, res) => {
  try {
    const { name, occupation, email, password } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ message: "Name, email, password are required" });
    }

    const existing = await User.findOne({ email });
    if (existing) {
      return res.status(409).json({ message: "Email already registered" });
    }

    const hash = await bcrypt.hash(password, 10);

    const user = await User.create({
      name,
      occupation: occupation || "",
      email,
      password: hash,
    });

    return res.status(201).json({ message: "User registered successfully" });
  } catch (err) {
    console.error("REGISTER ERROR", err);
    return res.status(500).json({ message: "Server error" });
  }
});

// Login
app.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: "Email and password required" });
    }

    const user = await User.findOne({ email });
    if (!user) return res.status(401).json({ message: "Invalid credentials" });

    const match = await bcrypt.compare(password, user.password);
    if (!match) return res.status(401).json({ message: "Invalid credentials" });

    const token = jwt.sign({ id: user._id }, process.env.JWT_SECRET, {
      expiresIn: "7d",
    });

    return res.json({ token });
  } catch (err) {
    console.error("LOGIN ERROR", err);
    return res.status(500).json({ message: "Server error" });
  }
});

// Profile (used by dashboard)
app.get("/profile", auth, async (req, res) => {
  try {
    const user = await User.findById(req.userId).select("-password");
    if (!user) return res.status(404).json({ message: "User not found" });
    return res.json(user);
  } catch (err) {
    console.error("PROFILE ERROR", err);
    return res.status(500).json({ message: "Server error" });
  }
});

// ===== EXPENSE ROUTES =====

// Create
app.post("/expenses", auth, async (req, res) => {
  try {
    const { title, amount, category, date } = req.body;

    const expense = await Expense.create({
      title,
      amount,
      category,
      date,
      user: req.userId,
    });

    return res.status(201).json(expense);
  } catch (err) {
    console.error("CREATE EXPENSE ERROR", err);
    return res.status(500).json({ message: "Server error" });
  }
});

// List (only user's expenses)
app.get("/expenses", auth, async (req, res) => {
  try {
    const expenses = await Expense.find({ user: req.userId })
      .sort({ date: -1, createdAt: -1 });
    return res.json(expenses);
  } catch (err) {
    console.error("GET EXPENSES ERROR", err);
    return res.status(500).json({ message: "Server error" });
  }
});

// Update
app.patch("/expenses/:id", auth, async (req, res) => {
  try {
    const { id } = req.params;

    const updated = await Expense.findOneAndUpdate(
      { _id: id, user: req.userId },
      req.body,
      { new: true }
    );

    if (!updated) {
      return res.status(404).json({ message: "Expense not found" });
    }

    return res.json(updated);
  } catch (err) {
    console.error("UPDATE EXPENSE ERROR", err);
    return res.status(500).json({ message: "Server error" });
  }
});

// Delete
app.delete("/expenses/:id", auth, async (req, res) => {
  try {
    const { id } = req.params;

    const deleted = await Expense.findOneAndDelete({
      _id: id,
      user: req.userId,
    });

    if (!deleted) {
      return res.status(404).json({ message: "Expense not found" });
    }

    return res.json({ message: "Expense deleted" });
  } catch (err) {
    console.error("DELETE EXPENSE ERROR", err);
    return res.status(500).json({ message: "Server error" });
  }
});

// Health
app.get("/", (req, res) => {
  res.send("API running");
});

// ===== START SERVER =====
const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});
