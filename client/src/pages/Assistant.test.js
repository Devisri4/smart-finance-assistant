import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import api from "../api/api";
import Assistant from "./Assistant";

// Pretend server: no real network calls in tests.
jest.mock("../api/api", () => ({
  __esModule: true,
  default: { post: jest.fn() },
  errorMessage: (err, fallback) => err?.response?.data?.message || fallback,
}));

// Charts need a real browser size; a simple stand-in shows which categories were passed.
jest.mock("../components/Charts", () => ({
  CategoryDonut: ({ data }) =>
    require("react").createElement("div", { "data-testid": "chat-chart" }, data.map((d) => d.category).join(",")),
}));

const breakdownReply = {
  reply: "You spent ₹1,500 this month.\n- Food: ₹1,200\n- Transport: ₹300",
  chart: {
    type: "category-donut",
    month: "2026-03",
    data: [
      { category: "Food", thisMonth: 1200, lastMonth: 900 },
      { category: "Transport", thisMonth: 300, lastMonth: 0 },
    ],
  },
  suggestions: ["Suggest a budget", "How are my budgets doing?"],
};

beforeEach(() => {
  api.post.mockReset();
});

test("shows a welcome message and quick questions before the first message", () => {
  render(<Assistant />);

  expect(screen.getByText("Hi! I can explain your money.")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Show my spending breakdown this month" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Send" })).toBeDisabled();
});

test("a quick question gets an answer with a chart and new suggestions", async () => {
  api.post.mockResolvedValue({ data: breakdownReply });
  render(<Assistant />);

  userEvent.click(screen.getByRole("button", { name: "Show my spending breakdown this month" }));

  expect(await screen.findByText("You spent ₹1,500 this month.")).toBeInTheDocument();
  expect(screen.getByText("Food: ₹1,200")).toBeInTheDocument();
  expect(screen.getByTestId("chat-chart")).toHaveTextContent("Food,Transport");
  expect(screen.getByRole("button", { name: "How are my budgets doing?" })).toBeInTheDocument();
  expect(screen.queryByText("Hi! I can explain your money.")).not.toBeInTheDocument();
  expect(api.post).toHaveBeenCalledWith(
    "/assistant/chat",
    { message: "Show my spending breakdown this month", conversationId: expect.any(String) },
    expect.anything()
  );
});

test("a typed question is sent and the assistant shows it is typing", async () => {
  let answer;
  api.post.mockReturnValue(
    new Promise((resolve) => {
      answer = resolve;
    })
  );
  render(<Assistant />);

  userEvent.type(screen.getByLabelText("Message"), "Where can I save money?");
  userEvent.click(screen.getByRole("button", { name: "Send" }));

  expect(screen.getByText("Where can I save money?")).toBeInTheDocument();
  expect(screen.getByRole("status", { name: "Assistant is typing" })).toBeInTheDocument();
  expect(screen.getByLabelText("Message")).toHaveValue("");

  answer({ data: { reply: "Shopping went up the most.", chart: null, suggestions: [] } });
  expect(await screen.findByText("Shopping went up the most.")).toBeInTheDocument();
  expect(screen.queryByRole("status", { name: "Assistant is typing" })).not.toBeInTheDocument();
  expect(screen.queryByTestId("chat-chart")).not.toBeInTheDocument();
});

test("an error shows the server's friendly message and can be retried", async () => {
  api.post
    .mockRejectedValueOnce({ response: { data: { message: "The AI assistant is having trouble right now." } } })
    .mockResolvedValueOnce({ data: { ...breakdownReply, suggestions: ["How are my budgets doing?"] } });
  render(<Assistant />);

  userEvent.click(screen.getByRole("button", { name: "Suggest a budget" }));
  expect(await screen.findByText("The AI assistant is having trouble right now.")).toBeInTheDocument();

  userEvent.click(screen.getByRole("button", { name: /try again/i }));

  expect(await screen.findByText("You spent ₹1,500 this month.")).toBeInTheDocument();
  expect(screen.queryByText("The AI assistant is having trouble right now.")).not.toBeInTheDocument();
  expect(api.post).toHaveBeenCalledTimes(2);
  // The retry asks the same question again, without showing it twice.
  expect(screen.getAllByText("Suggest a budget")).toHaveLength(1);
});
