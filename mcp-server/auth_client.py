import os

import httpx
from dotenv import load_dotenv

load_dotenv()

BACKEND_URL = os.environ.get("BACKEND_URL", "http://localhost:8080")
USERNAME = os.environ["MCP_USERNAME"]
PASSWORD = os.environ["MCP_PASSWORD"]

_token: str | None = None


def _login() -> str:
    global _token
    response = httpx.post(
        f"{BACKEND_URL}/api/auth/login",
        json={"username": USERNAME, "password": PASSWORD},
        timeout=10.0,
    )
    response.raise_for_status()
    _token = response.json()["token"]
    return _token


def get_token() -> str:
    """캐시된 JWT를 반환하고, 없으면 새로 로그인한다."""
    return _token or _login()


def refresh_token() -> str:
    """백엔드가 401을 반환했을 때(JWT 만료) 캐시를 버리고 재로그인한다."""
    return _login()


if __name__ == "__main__":
    token = get_token()
    print(f"로그인 성공. JWT: {token[:20]}...")
