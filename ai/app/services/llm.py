"""로컬 LLM(Ollama) 스트리밍 호출."""

import json
import os
from collections.abc import AsyncIterator

import httpx

OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://host.docker.internal:11434").rstrip("/")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "gemma3:12b")

# 모델을 처음 GPU 에 올릴 때 첫 조각까지 수십 초 걸릴 수 있어 read 를 넉넉히
TIMEOUT = httpx.Timeout(connect=5.0, read=180.0, write=10.0, pool=5.0)


class LlmError(Exception):
    """LLM 호출 실패. 메시지는 내부 로그용 (사용자에게 그대로 보내지 않음)."""


async def ollama_chat(messages: list[dict], client: httpx.AsyncClient, json_mode: bool = False) -> AsyncIterator[str]:
    """Ollama /api/chat 의 NDJSON 스트림을 글자 조각(delta)으로 바꿔 내보낸다."""
    payload = {"model": OLLAMA_MODEL, "messages": messages, "stream": True}
    if json_mode:
        payload["format"] = "json"
    async with client.stream("POST", f"{OLLAMA_BASE_URL}/api/chat", json=payload) as res:
        if res.status_code != 200:
            raise LlmError(f"ollama status {res.status_code}")
        async for line in res.aiter_lines():
            if not line.strip():
                continue
            try:
                data = json.loads(line)
            except json.JSONDecodeError as e:
                raise LlmError("ollama sent invalid json") from e
            if data.get("error"):
                raise LlmError("ollama error")
            delta = (data.get("message") or {}).get("content") or ""
            if delta:
                yield delta
            if data.get("done"):
                return
