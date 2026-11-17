import json
import logging

import httpx
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from app.deps import LLM_PROVIDER, internal_only
from app.schemas.chat import ChatRequest
from app.services.llm import TIMEOUT, LlmError, ollama_chat

router = APIRouter(dependencies=[Depends(internal_only)])
log = logging.getLogger(__name__)


def _line(obj: dict) -> str:
    return json.dumps(obj, ensure_ascii=False) + "\n"


@router.post("/v1/chat")
async def chat(req: ChatRequest) -> StreamingResponse:
    if LLM_PROVIDER != "ollama":
        # ponytail: 외부 API(Claude 등) 공급자는 다음 단계, 지금은 ollama 만
        raise HTTPException(status_code=501, detail="LLM_PROVIDER=ollama 만 지원")

    messages = [{"role": "system", "content": req.system}] + [m.model_dump(exclude_none=True) for m in req.messages]

    async def stream():
        async with httpx.AsyncClient(timeout=TIMEOUT) as client:
            try:
                async for delta in ollama_chat(messages, client, json_mode=req.json_mode):
                    yield _line({"delta": delta})
            except (LlmError, httpx.HTTPError) as e:
                # 질문·답변 내용은 남기지 않고 오류 종류만
                log.warning("llm failed: %s", type(e).__name__)
                yield _line({"error": "llm_unavailable"})

    return StreamingResponse(stream(), media_type="application/x-ndjson")
