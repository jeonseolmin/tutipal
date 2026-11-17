"""자료 RAG 재료: 글자 뽑기(txt · md · pdf) · 임베딩(Ollama bge-m3). DB 저장 · 검색은 Spring 이 한다 (스키마는 Spring Flyway)."""

import base64
import io
import logging
import os

import httpx
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from pypdf import PdfReader
from pypdf.errors import PdfReadError

from app.deps import internal_only
from app.services.llm import OLLAMA_BASE_URL

router = APIRouter(dependencies=[Depends(internal_only)])
log = logging.getLogger(__name__)

EMBEDDING_MODEL = os.environ.get("EMBEDDING_MODEL", "bge-m3")
MAX_PAGES = 300


class ExtractRequest(BaseModel):
    filename: str = Field(max_length=200)
    data: str = Field(max_length=15_000_000)   # base64, 원본 10MB 이하


class Page(BaseModel):
    page: int | None   # pdf 쪽 번호 (txt/md 는 None)
    text: str


class EmbedRequest(BaseModel):
    texts: list[str] = Field(min_length=1, max_length=64)


@router.post("/v1/extract")
def extract(req: ExtractRequest) -> dict:
    try:
        raw = base64.b64decode(req.data, validate=True)
    except ValueError as e:
        raise HTTPException(status_code=400, detail="bad base64") from e
    name = req.filename.lower()
    if name.endswith((".txt", ".md")):
        text = raw.decode("utf-8", errors="replace")
        return {"pages": [Page(page=None, text=text).model_dump()]}
    if name.endswith(".pdf"):
        try:
            reader = PdfReader(io.BytesIO(raw))
            pages = [Page(page=i + 1, text=(p.extract_text() or "")).model_dump()
                     for i, p in enumerate(reader.pages[:MAX_PAGES])]
        except (PdfReadError, ValueError, KeyError) as e:
            log.warning("pdf extract failed: %s", type(e).__name__)
            raise HTTPException(status_code=422, detail="cannot read pdf") from e
        return {"pages": pages}
    raise HTTPException(status_code=415, detail="txt, md, pdf only")


@router.post("/v1/embed")
async def embed(req: EmbedRequest) -> dict:
    texts = [t[:4000] for t in req.texts]
    async with httpx.AsyncClient(timeout=httpx.Timeout(120.0, connect=5.0)) as client:
        try:
            res = await client.post(f"{OLLAMA_BASE_URL}/api/embed", json={"model": EMBEDDING_MODEL, "input": texts})
        except httpx.HTTPError as e:
            log.warning("embed failed: %s", type(e).__name__)
            raise HTTPException(status_code=503, detail="embedding unavailable") from e
    if res.status_code != 200:
        raise HTTPException(status_code=503, detail="embedding unavailable")
    vectors = res.json().get("embeddings") or []
    if len(vectors) != len(texts):
        raise HTTPException(status_code=503, detail="embedding unavailable")
    return {"model": EMBEDDING_MODEL, "vectors": vectors}
