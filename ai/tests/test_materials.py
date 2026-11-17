import base64
import io

import httpx
import pytest
from fastapi.testclient import TestClient
from pypdf import PdfWriter

from app.main import app
from app.routers import materials

client = TestClient(app)
H = {"X-Internal-Key": "test-key"}


def b64(data: bytes) -> str:
    return base64.b64encode(data).decode()


def test_extract_text_and_md():
    res = client.post("/v1/extract", json={"filename": "노트.md", "data": b64("# 반복문\nfor 문".encode())}, headers=H)
    assert res.status_code == 200
    assert res.json()["pages"] == [{"page": None, "text": "# 반복문\nfor 문"}]


def test_extract_pdf_pages():
    w = PdfWriter()
    w.add_blank_page(width=200, height=200)
    w.add_blank_page(width=200, height=200)
    buf = io.BytesIO()
    w.write(buf)
    res = client.post("/v1/extract", json={"filename": "a.pdf", "data": b64(buf.getvalue())}, headers=H)
    assert res.status_code == 200
    assert [p["page"] for p in res.json()["pages"]] == [1, 2]


def test_extract_rejects_bad_input():
    assert client.post("/v1/extract", json={"filename": "a.exe", "data": b64(b"MZ")}, headers=H).status_code == 415
    assert client.post("/v1/extract", json={"filename": "a.pdf", "data": b64(b"not a pdf")}, headers=H).status_code == 422
    assert client.post("/v1/extract", json={"filename": "a.txt", "data": "!!"}, headers=H).status_code == 400
    assert client.post("/v1/extract", json={"filename": "a.txt", "data": b64(b"x")}).status_code == 401


def test_embed_calls_ollama(monkeypatch):
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"embeddings": [[0.1, 0.2], [0.3, 0.4]]})

    real = httpx.AsyncClient
    monkeypatch.setattr(materials.httpx, "AsyncClient", lambda **kw: real(transport=httpx.MockTransport(handler), **kw))
    res = client.post("/v1/embed", json={"texts": ["a", "b"]}, headers=H)
    assert res.status_code == 200
    assert res.json()["vectors"] == [[0.1, 0.2], [0.3, 0.4]]


def test_embed_count_mismatch_is_503(monkeypatch):
    real = httpx.AsyncClient
    transport = httpx.MockTransport(lambda r: httpx.Response(200, json={"embeddings": [[0.1]]}))
    monkeypatch.setattr(materials.httpx, "AsyncClient", lambda **kw: real(transport=transport, **kw))
    assert client.post("/v1/embed", json={"texts": ["a", "b"]}, headers=H).status_code == 503
