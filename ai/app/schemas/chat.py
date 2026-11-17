from typing import Literal

from pydantic import BaseModel, Field, field_validator


class Message(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=8000)
    # 화면 요약 · 화면 질문: base64 JPEG/PNG 한 장 (저장하지 않고 Ollama 에 그대로 넘김)
    images: list[str] | None = Field(default=None, max_length=1)

    @field_validator("images")
    @classmethod
    def _image_size(cls, v: list[str] | None) -> list[str] | None:
        if v and any(len(img) > 3_000_000 for img in v):
            raise ValueError("image too large")
        return v


class ChatRequest(BaseModel):
    system: str = Field(max_length=12000)   # 주제 · 선생님 설정 + 자료 조각(최대 3개)
    messages: list[Message] = Field(min_length=1, max_length=40)
    # True 면 Ollama 가 JSON 하나만 내도록 강제 (이해도 질문 채점 · 문제 만들기)
    json_mode: bool = False
