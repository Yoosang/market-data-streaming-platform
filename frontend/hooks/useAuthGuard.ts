"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getToken, removeToken } from "@/lib/auth";

function getExpiryMs(token: string): number | null {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return typeof payload.exp === "number" ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

// 토큰이 없으면 즉시 로그인 페이지로 이동, 있으면 만료 시점에 자동 로그아웃.
// 인증 확인이 끝나기 전까지는 authed=false를 반환 — 호출부는 이때 아무것도 렌더링하지 않아야 함
export function useAuthGuard(): boolean {
  const router = useRouter();
  const [authed, setAuthed] = useState(false);

  useEffect(() => {
    const token = getToken();
    if (!token) {
      router.replace("/login");
      return;
    }

    const expiryMs = getExpiryMs(token);
    if (expiryMs !== null && expiryMs <= Date.now()) {
      removeToken();
      router.replace("/login");
      return;
    }

    setAuthed(true);
    if (expiryMs === null) return;

    const timer = setTimeout(() => {
      removeToken();
      router.replace("/login");
    }, expiryMs - Date.now());
    return () => clearTimeout(timer);
  }, [router]);

  return authed;
}
