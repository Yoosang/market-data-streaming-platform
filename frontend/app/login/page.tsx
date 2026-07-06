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
    router.push("/");
  };

  return (
    <main className="min-h-screen bg-gray-950 text-white flex flex-col items-center justify-center px-4">
      <h1 className="text-lg font-semibold text-gray-300 mb-8 tracking-widest uppercase">
        Stock Market · Live
      </h1>

      <div className="w-full max-w-sm">
        {/* 탭 */}
        <div className="flex gap-1 bg-gray-800 rounded-xl p-1 mb-6">
          {(["login", "signup"] as const).map((t) => (
            <button
              key={t}
              onClick={() => { setTab(t); setError(""); }}
              className={`flex-1 py-2 text-sm font-semibold rounded-lg transition-colors ${
                tab === t ? "bg-blue-600 text-white" : "text-gray-400 hover:text-white"
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
            className="w-full bg-gray-800 text-white rounded-xl px-4 py-3 text-sm outline-none placeholder-gray-600"
          />
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSubmit()}
            placeholder="비밀번호"
            className="w-full bg-gray-800 text-white rounded-xl px-4 py-3 text-sm outline-none placeholder-gray-600"
          />

          {error && (
            <p className="text-red-400 text-xs px-1">{error}</p>
          )}

          <button
            onClick={handleSubmit}
            className="w-full bg-blue-600 hover:bg-blue-500 text-white rounded-xl py-3 text-sm font-semibold"
          >
            {tab === "login" ? "로그인" : "회원가입"}
          </button>
        </div>
      </div>
    </main>
  );
}
