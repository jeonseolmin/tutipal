import hmac
import os

from fastapi import Header, HTTPException

INTERNAL_KEY = os.environ.get("AI_INTERNAL_KEY", "")
LLM_PROVIDER = os.environ.get("LLM_PROVIDER", "ollama")


def internal_only(x_internal_key: str = Header(default="")) -> None:
    """Spring 만 호출하도록: 내부 키가 비었거나 다르면 거부."""
    if not INTERNAL_KEY or not hmac.compare_digest(x_internal_key, INTERNAL_KEY):
        raise HTTPException(status_code=401)
