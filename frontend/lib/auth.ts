const TOKEN_KEY = "auth_token";

export const getToken = (): string | null => {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
};

export const setToken = (token: string): void =>
  localStorage.setItem(TOKEN_KEY, token);

export const removeToken = (): void =>
  localStorage.removeItem(TOKEN_KEY);

// 모든 인증 API 요청에 공통으로 사용할 헤더
export const authHeaders = (): Record<string, string> => {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
};
