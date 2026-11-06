import { newConversationId, toBlocks } from "./chat";

test("splits a reply into paragraphs and bullet lists", () => {
  const reply = "You spent **₹1,500** this month.\n\n- Food: ₹1,200\n- Transport: ₹300\nKeep it up!";
  expect(toBlocks(reply)).toEqual([
    { type: "p", text: "You spent ₹1,500 this month." },
    { type: "list", items: ["Food: ₹1,200", "Transport: ₹300"] },
    { type: "p", text: "Keep it up!" },
  ]);
});

test("treats numbered lines as a list and ignores empty replies", () => {
  expect(toBlocks("1. One\n2) Two")).toEqual([{ type: "list", items: ["One", "Two"] }]);
  expect(toBlocks(null)).toEqual([]);
});

test("conversation ids only use characters the server accepts", () => {
  const id = newConversationId();
  expect(id).toMatch(/^[A-Za-z0-9-]{1,64}$/);
  expect(newConversationId()).not.toBe(id);
});
