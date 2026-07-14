"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { setToken } from "@/lib/auth";

const API_BASE = "http://localhost:8080";

export default function LoginPage() {
  const router = useRouter();
  const [tab, setTab] = useState<"login" | "signup">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleTab = (tab: "login" | "signup") => {
    setTab(tab); setError("");
    setUsername("");
    setPassword("");
  }

  const handleSubmit = async () => {
    if (!username.trim() || !password.trim()) return;
    setError("");

    const endpoint = tab === "login" ? "/api/auth/login" : "/api/auth/signup";
    const res = await fetch(`${API_BASE}${endpoint}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });

    if (!res.ok) {
      const msg = await res.text();
      setError(msg || "요청에 실패했습니다.");
      return;
    }

    const { token } = await res.json();
    setToken(token);
    router.push("/watchlist");
  };

  return (
    <main className="min-h-screen bg-surface-black text-body-on-dark flex flex-col items-center justify-center px-4">
      <h1 className="text-[21px] font-semibold text-body-muted mb-8 tracking-widest uppercase">
        Stock Market · Live
      </h1>

      <div className="w-full max-w-sm">
        {/* 탭 */}
        <div className="flex gap-1 bg-surface-tile-2 rounded-pill p-1 mb-6">
          {(["login", "signup"] as const).map((t) => (
            <button
              key={t}
              onClick={() => { handleTab(t) }}
              className={`flex-1 py-2 text-sm font-semibold rounded-pill transition-colors ${
                tab === t ? "bg-primary text-body-on-dark" : "text-body-muted hover:text-body-on-dark"
              }`}
            >
              {t === "login" ? "로그인" : "회원가입"}
            </button>
          ))}
        </div>

        {/* 입력 폼 */}
        <div className="space-y-3">
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="아이디"
            className="w-full bg-surface-tile-2 text-body-on-dark rounded-pill px-5 py-3 text-[17px] outline-none placeholder-ink-muted-48"
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
            placeholder="비밀번호"
            className="w-full bg-surface-tile-2 text-body-on-dark rounded-pill px-5 py-3 text-[17px] outline-none placeholder-ink-muted-48"
          />

          {error && (
            <p className="text-red-400 text-xs px-1">{error}</p>
          )}

          <button
            onClick={handleSubmit}
            className="w-full bg-primary hover:bg-primary-focus text-body-on-dark rounded-pill py-3 text-[17px] font-normal transition-colors"
          >
            {tab === "login" ? "로그인" : "회원가입"}
          </button>
        </div>
      </div>
    </main>
  );
}
