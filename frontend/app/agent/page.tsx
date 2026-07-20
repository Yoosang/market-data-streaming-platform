"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import { useAgentChat } from "@/hooks/useAgentChat";

// AI 답변에 포함된 마크다운(볼드/표/목록)을 다크 테마에 맞게 렌더링
function AssistantContent({ content }: { content: string }) {
  return (
    <div className="text-sm leading-relaxed [&>*:not(:last-child)]:mb-2">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          p: (props) => <p {...props} />,
          strong: (props) => <strong className="font-semibold" {...props} />,
          ul: (props) => <ul className="list-disc pl-4 space-y-1" {...props} />,
          ol: (props) => <ol className="list-decimal pl-4 space-y-1" {...props} />,
          h1: (props) => <p className="font-semibold text-base" {...props} />,
          h2: (props) => <p className="font-semibold text-base" {...props} />,
          h3: (props) => <p className="font-semibold" {...props} />,
          table: (props) => (
            <div className="overflow-x-auto">
              <table className="text-xs border-collapse" {...props} />
            </div>
          ),
          th: (props) => (
            <th className="border border-hairline-on-dark px-2 py-1 text-left" {...props} />
          ),
          td: (props) => <td className="border border-hairline-on-dark px-2 py-1" {...props} />,
          code: (props) => <code className="bg-surface-tile-2 rounded-xs px-1" {...props} />,
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}

export default function AgentChatPage() {
  const authed = useAuthGuard();
  const { messages, sendMessage, loading, error } = useAgentChat();
  const [input, setInput] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed || loading) return;
    setInput("");
    sendMessage(trimmed);
  };

  if (!authed) return null;

  return (
    <main className="min-h-screen bg-surface-black text-body-on-dark flex flex-col items-center py-8 px-4">
      <div className="w-full max-w-sm mb-6">
        <Link href="/watchlist" className="text-xs text-ink-muted-48 hover:text-body-muted transition-colors">
          ← 목록으로
        </Link>
        <h1 className="text-[21px] font-semibold text-body-on-dark mt-2">AI PB 어시스턴트</h1>
        <p className="text-xs text-body-muted mt-1">
          관심종목, 최근 뉴스, 시세 흐름을 바탕으로 답해드려요
        </p>
      </div>

      <div className="w-full max-w-sm flex-1 space-y-3">
        {messages.length === 0 && (
          <p className="text-center text-ink-muted-48 text-sm py-6">
            &ldquo;내 관심종목 어때?&rdquo; 처럼 물어보세요
          </p>
        )}

        {messages.map((m, i) => (
          <div key={i} className={`flex flex-col ${m.role === "user" ? "items-end" : "items-start"}`}>
            {m.role === "assistant" && m.toolCalls && m.toolCalls.length > 0 && (
              <div className="flex flex-wrap gap-1 mb-1">
                {m.toolCalls.map((tool, j) => (
                  <span
                    key={j}
                    className="text-[11px] text-primary-on-dark bg-surface-tile-2 rounded-pill px-2 py-0.5"
                  >
                    🔧 {tool}
                  </span>
                ))}
              </div>
            )}
            <div
              className={`max-w-[85%] rounded-lg px-4 py-2.5 text-body-on-dark ${
                m.role === "user" ? "bg-primary text-sm whitespace-pre-wrap leading-relaxed" : "bg-surface-tile-1"
              }`}
            >
              {m.role === "assistant" ? <AssistantContent content={m.content} /> : m.content}
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex items-start">
            <div className="bg-surface-tile-1 rounded-lg px-4 py-2.5 text-sm text-ink-muted-48">
              생각 중...
            </div>
          </div>
        )}

        {error && <p className="text-red-400 text-xs px-1">{error}</p>}

        <div ref={bottomRef} />
      </div>

      <div className="w-full max-w-sm flex gap-2 sticky bottom-4 mt-6">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSend()}
          placeholder="메시지를 입력하세요"
          disabled={loading}
          className="flex-1 min-w-0 bg-surface-tile-2 text-body-on-dark rounded-pill px-4 py-3 text-sm outline-none placeholder-ink-muted-48 disabled:opacity-50"
        />
        <button
          onClick={handleSend}
          disabled={loading || !input.trim()}
          className="bg-primary hover:bg-primary-focus disabled:opacity-50 text-body-on-dark rounded-pill px-5 py-3 text-sm shrink-0 transition-colors"
        >
          전송
        </button>
      </div>
    </main>
  );
}
