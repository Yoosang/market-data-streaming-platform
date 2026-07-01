const USER_ID_KEY = "marketdata_user_id";

// 브라우저 localStorage에서 사용자 ID를 가져오거나 없으면 새로 생성
// SSR 환경(서버)에서는 window가 없으므로 빈 문자열 반환
export function getUserId(): string {
  if (typeof window === "undefined") return "";

  let userId = localStorage.getItem(USER_ID_KEY);
  if (!userId) {
    userId = crypto.randomUUID();
    localStorage.setItem(USER_ID_KEY, userId);
  }
  return userId;
}
