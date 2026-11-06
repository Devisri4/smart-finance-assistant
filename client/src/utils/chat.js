// Small helpers for the AI Assistant chat page.

/** Same limit as the server (AssistantController.MAX_MESSAGE_LENGTH). */
export const MAX_MESSAGE_LENGTH = 500;

export const STARTER_QUESTIONS = [
  "Show my spending breakdown this month",
  "Suggest a budget",
  "Where can I save money?",
];

/** A random id for one chat, so the server can remember the conversation. Letters, digits and "-" only. */
export function newConversationId() {
  // randomUUID only exists on https pages and newer browsers, so there is a simple fallback.
  const uuid = window.crypto?.randomUUID?.();
  if (uuid) return uuid;
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
}

/**
 * Turns the assistant's plain-text reply into paragraphs and bullet lists.
 * We never insert the reply as HTML, so a reply can't inject code into the page.
 */
export function toBlocks(text) {
  const blocks = [];
  String(text || "")
    .replace(/\*\*/g, "")
    .split(/\r?\n/)
    .forEach((raw) => {
      const line = raw.trim().replace(/^#+\s*/, "");
      if (!line) return;
      const bullet = line.match(/^(?:[-*•]|\d+[.)])\s+(.*)$/);
      if (bullet) {
        const last = blocks[blocks.length - 1];
        if (last && last.type === "list") {
          last.items.push(bullet[1]);
        } else {
          blocks.push({ type: "list", items: [bullet[1]] });
        }
      } else {
        blocks.push({ type: "p", text: line });
      }
    });
  return blocks;
}
