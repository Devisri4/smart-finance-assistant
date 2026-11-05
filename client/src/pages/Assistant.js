import { useEffect, useRef, useState } from "react";
import { FaPaperPlane, FaPlus, FaRedo, FaRobot } from "react-icons/fa";
import api, { errorMessage } from "../api/api";
import { CategoryDonut } from "../components/Charts";
import { PageHeader } from "../components/ui";
import { MAX_MESSAGE_LENGTH, STARTER_QUESTIONS, newConversationId, toBlocks } from "../utils/chat";
import { formatMoney, monthLabel } from "../utils/format";

// The AI provider can be slow (and the free server may be waking up), so wait up to a minute.
const CHAT_TIMEOUT_MS = 60000;

function ReplyText({ text }) {
  return toBlocks(text).map((block, i) =>
    block.type === "list" ? (
      <ul key={i}>
        {block.items.map((item, j) => (
          <li key={j}>{item}</li>
        ))}
      </ul>
    ) : (
      <p key={i}>{block.text}</p>
    )
  );
}

/** The numbers here come straight from the server's tool result, not from the AI's text. */
function ReplyChart({ chart }) {
  const total = chart.data.reduce((sum, d) => sum + d.thisMonth, 0);
  return (
    <figure className="chat-chart">
      <figcaption className="muted small">
        Spending by category · {monthLabel(chart.month)} · {formatMoney(total)}
      </figcaption>
      <CategoryDonut data={chart.data} height={220} />
    </figure>
  );
}

function Message({ message, onRetry, busy }) {
  if (message.role === "user") {
    return (
      <div className="chat-row user">
        <div className="chat-bubble">
          <p>{message.text}</p>
        </div>
      </div>
    );
  }
  return (
    <div className={`chat-row ${message.role}`}>
      <span className="chat-avatar" aria-hidden="true">
        <FaRobot />
      </span>
      <div className="chat-bubble">
        {message.role === "error" ? (
          <>
            <p>{message.text}</p>
            <div className="chat-error-actions">
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => onRetry(message)}
                disabled={busy}
              >
                <FaRedo aria-hidden="true" /> Try again
              </button>
            </div>
          </>
        ) : (
          <>
            <ReplyText text={message.text} />
            {message.chart?.type === "category-donut" && message.chart.data?.length > 0 && (
              <ReplyChart chart={message.chart} />
            )}
          </>
        )}
      </div>
    </div>
  );
}

function Assistant() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [suggestions, setSuggestions] = useState(STARTER_QUESTIONS);
  const [conversationId, setConversationId] = useState(newConversationId);
  const nextId = useRef(1);
  const endRef = useRef(null);

  // Keep the newest message in view. (scrollIntoView does not exist in test browsers.)
  useEffect(() => {
    endRef.current?.scrollIntoView?.({ behavior: "smooth", block: "end" });
  }, [messages, sending]);

  const addMessage = (message) =>
    setMessages((list) => [...list, { id: nextId.current++, ...message }]);

  const ask = async (text) => {
    setSending(true);
    try {
      const res = await api.post(
        "/assistant/chat",
        { message: text, conversationId },
        { timeout: CHAT_TIMEOUT_MS }
      );
      addMessage({ role: "assistant", text: res.data.reply, chart: res.data.chart });
      if (res.data.suggestions?.length) setSuggestions(res.data.suggestions);
    } catch (err) {
      addMessage({
        role: "error",
        text: errorMessage(err, "Could not reach the assistant. Please try again."),
        question: text,
      });
    } finally {
      setSending(false);
    }
  };

  const send = (raw) => {
    const text = raw.trim();
    if (!text || sending || text.length > MAX_MESSAGE_LENGTH) return;
    addMessage({ role: "user", text });
    setInput("");
    ask(text);
  };

  const retry = (errorMsg) => {
    setMessages((list) => list.filter((m) => m.id !== errorMsg.id));
    ask(errorMsg.question);
  };

  const newChat = () => {
    setMessages([]);
    setSuggestions(STARTER_QUESTIONS);
    setConversationId(newConversationId());
    setInput("");
  };

  const onSubmit = (e) => {
    e.preventDefault();
    send(input);
  };

  const left = MAX_MESSAGE_LENGTH - input.length;

  return (
    <>
      <PageHeader
        title="AI Assistant"
        subtitle="Ask about your spending and budgets. Answers come only from your own data."
      >
        <button type="button" className="btn btn-ghost" onClick={newChat} disabled={sending}>
          <FaPlus aria-hidden="true" /> New chat
        </button>
      </PageHeader>

      <section className="card chat-card">
        <div className="chat-log" role="log" aria-live="polite">
          {messages.length === 0 && (
            <div className="chat-welcome">
              <span className="chat-welcome-icon" aria-hidden="true">
                <FaRobot />
              </span>
              <strong>Hi! I can explain your money.</strong>
              <p>
                Pick a question below or type your own. I only read your expenses and budgets, and
                I can&apos;t add, change or delete anything.
              </p>
            </div>
          )}

          {messages.map((m) => (
            <Message key={m.id} message={m} onRetry={retry} busy={sending} />
          ))}

          {sending && (
            <div className="chat-row assistant">
              <span className="chat-avatar" aria-hidden="true">
                <FaRobot />
              </span>
              <div className="chat-bubble" role="status" aria-label="Assistant is typing">
                <span className="typing" aria-hidden="true">
                  <span />
                  <span />
                  <span />
                </span>
              </div>
            </div>
          )}
          <div ref={endRef} />
        </div>

        <div className="chat-chips" role="group" aria-label="Suggested questions">
          {suggestions.map((q) => (
            <button key={q} type="button" className="chat-chip" onClick={() => send(q)} disabled={sending}>
              {q}
            </button>
          ))}
        </div>

        <form className="chat-composer" onSubmit={onSubmit}>
          <input
            aria-label="Message"
            placeholder="Ask about your spending…"
            value={input}
            maxLength={MAX_MESSAGE_LENGTH}
            onChange={(e) => setInput(e.target.value)}
          />
          <span className={`chat-counter ${left < 50 ? "near-limit" : ""}`}>{left}</span>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={sending || !input.trim()}
            aria-label="Send"
          >
            <FaPaperPlane aria-hidden="true" />
          </button>
        </form>
      </section>
    </>
  );
}

export default Assistant;
