# Smart Finance Assistant 💰

[![CI](https://github.com/Devisri4/smart-finance-assistant/actions/workflows/ci.yml/badge.svg)](https://github.com/Devisri4/smart-finance-assistant/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)
![React](https://img.shields.io/badge/React-19-61DAFB)
![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248)

A full-stack personal finance app for tracking expenses, setting budgets and getting smart spending insights.

**👉 [Live demo](https://smart-finance-assistant.vercel.app):** click **"Try the demo"** (no sign-up). The first load can take up to a minute while the free server wakes up.

## Features

- Secure sign-up and login (Spring Security, JWT, BCrypt)
- Add, edit, delete, search and filter expenses, with CSV export
- Monthly budgets per category, with alerts at 80% and when over budget
- Smart insights: month-over-month change, top category, month-end projection
- Charts: 6-month trend, category split, this month vs. last month
- Responsive UI with light and dark mode
- One-click demo account with sample data

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring Data MongoDB, JJWT |
| Frontend | React 19, React Router, Axios, Recharts |
| Database | MongoDB Atlas |
| Testing / CI | JUnit 5, Mockito, MockMvc, Jest, GitHub Actions |
| Hosting | Render (Docker) for the API, Vercel for the frontend |

## Project structure

```
backend/   Spring Boot REST API (auth, expenses, budgets, insights, demo)
client/    React frontend (dashboard, expenses, budgets, insights)
.github/   CI workflow that builds and tests both on every push
```

## Author

**Devi Sri Mudududla** · [GitHub @Devisri4](https://github.com/Devisri4)
