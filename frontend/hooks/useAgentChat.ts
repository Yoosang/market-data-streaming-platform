import { useState } from "react";
import { authHeaders } from "@/lib/auth";

const API_BASE = "http://localhost:8080";

export type ChatMessage = {
  role: "user" | "assistant";
  content: string;
  toolCalls?: string[];
};

type ChatResponse = { reply: string; toolCalls: string[] };

export function useAgentChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const sendMessage = async (message: string) => {
    // 서버에는 role/content만 필요 — 지금까지의 대화(이번 메시지 제외)를 history로 전송
    const history = messages.map(({ role, content }) => ({ role, content }));
    setMessages((prev) => [...prev, { role: "user", content: message }]);
    setLoading(true);
    setError(null);

    try {
      const res = await fetch(`${API_BASE}/api/agent/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...authHeaders() },
        body: JSON.stringify({ message, history }),
      });
      if (!res.ok) throw new Error("요청 실패");

      const data: ChatResponse = await res.json();
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: data.reply, toolCalls: data.toolCalls },
      ]);
    } catch {
      setError("메시지를 보내지 못했습니다. 다시 시도해주세요.");
      // 실패 시 낙관적으로 추가했던 사용자 메시지를 되돌림
      setMessages((prev) => prev.slice(0, -1));
    } finally {
      setLoading(false);
    }
  };

  return { messages, sendMessage, loading, error };
}
