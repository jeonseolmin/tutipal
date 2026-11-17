import json

import httpx
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.routers import chat as chat_router
from app.services.llm import LlmError, ollama_chat


def _ollama_transport(lines: list[str], status: int = 200) -> httpx.MockTransport:
    body = "\n".join(lines).encode()
    return httpx.MockTransport(lambda request: httpx.Response(status, content=body))


async def _collect(transport: httpx.MockTransport) -> list[str]:
    async with httpx.AsyncClient(transport=transport) as client:
        return [d async for d in ollama_chat([{"role": "user", "content": "hi"}], client)]


@pytest.mark.anyio
async def test_ollama_stream_yields_deltas_until_done():
    lines = [
        json.dumps({"message": {"content": "안"}, "done": False}),
        "",
        json.dumps({"message": {"content": "녕"}, "done": False}),
        json.dumps({"message": {"content": ""}, "done": True}),
        json.dumps({"message": {"content": "무시"}, "done": False}),
    ]
    assert await _collect(_ollama_transport(lines)) == ["안", "녕"]


@pytest.mark.anyio
async def test_ollama_error_status_raises():
    with pytest.raises(LlmError):
        await _collect(_ollama_transport(["model not found"], status=404))


@pytest.mark.anyio
async def test_ollama_error_line_raises():
    with pytest.raises(LlmError):
        await _collect(_ollama_transport([json.dumps({"error": "boom"})]))


@pytest.fixture
def anyio_backend():
    return "asyncio"


client = TestClient(app)
BODY = {"system": "너는 조교야", "messages": [{"role": "user", "content": "질문"}]}


def test_chat_requires_internal_key():
    assert client.post("/v1/chat", json=BODY).status_code == 401
    assert client.post("/v1/chat", json=BODY, headers={"X-Internal-Key": "wrong"}).status_code == 401


def test_chat_streams_ndjson(monkeypatch):
    async def fake(messages, _client, json_mode=False):
        assert not json_mode
        assert messages[0] == {"role": "system", "content": "너는 조교야"}
        for d in ["좋", "아요"]:
            yield d

    monkeypatch.setattr(chat_router, "ollama_chat", fake)
    res = client.post("/v1/chat", json=BODY, headers={"X-Internal-Key": "test-key"})
    assert res.status_code == 200
    assert [json.loads(line) for line in res.text.splitlines()] == [{"delta": "좋"}, {"delta": "아요"}]


def test_chat_hides_llm_error_details(monkeypatch):
    async def broken(messages, _client, json_mode=False):
        raise LlmError("secret detail")
        yield  # pragma: no cover

    monkeypatch.setattr(chat_router, "ollama_chat", broken)
    res = client.post("/v1/chat", json=BODY, headers={"X-Internal-Key": "test-key"})
    assert res.text.strip() == json.dumps({"error": "llm_unavailable"})
    assert "secret" not in res.text


def test_chat_rejects_bad_input():
    headers = {"X-Internal-Key": "test-key"}
    assert client.post("/v1/chat", json={"system": "s", "messages": []}, headers=headers).status_code == 422
    bad_role = {"system": "s", "messages": [{"role": "system", "content": "x"}]}
    assert client.post("/v1/chat", json=bad_role, headers=headers).status_code == 422


def test_chat_passes_json_mode(monkeypatch):
    seen = {}

    async def fake(messages, _client, json_mode=False):
        seen["json_mode"] = json_mode
        yield "{}"

    monkeypatch.setattr(chat_router, "ollama_chat", fake)
    res = client.post("/v1/chat", json={**BODY, "json_mode": True}, headers={"X-Internal-Key": "test-key"})
    assert res.status_code == 200 and seen["json_mode"] is True


@pytest.mark.anyio
async def test_ollama_json_mode_sends_format():
    sent = {}

    def handler(request: httpx.Request) -> httpx.Response:
        sent.update(json.loads(request.content))
        return httpx.Response(200, content=json.dumps({"message": {"content": "{}"}, "done": True}).encode())

    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as c:
        assert [d async for d in ollama_chat([{"role": "user", "content": "hi"}], c, json_mode=True)] == ["{}"]
    assert sent["format"] == "json"


def test_chat_passes_image_and_limits_size(monkeypatch):
    seen = {}

    async def fake(messages, _client, json_mode=False):
        seen["messages"] = messages
        yield "화면"

    monkeypatch.setattr(chat_router, "ollama_chat", fake)
    headers = {"X-Internal-Key": "test-key"}
    body = {"system": "s", "messages": [{"role": "user", "content": "요약", "images": ["aGVsbG8="]}]}
    assert client.post("/v1/chat", json=body, headers=headers).status_code == 200
    assert seen["messages"][1] == {"role": "user", "content": "요약", "images": ["aGVsbG8="]}

    two = {"system": "s", "messages": [{"role": "user", "content": "x", "images": ["a", "b"]}]}
    assert client.post("/v1/chat", json=two, headers=headers).status_code == 422
    big = {"system": "s", "messages": [{"role": "user", "content": "x", "images": ["a" * 3_000_001]}]}
    assert client.post("/v1/chat", json=big, headers=headers).status_code == 422
