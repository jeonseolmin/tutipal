import os

import psycopg
from fastapi import Depends, FastAPI

from app.deps import LLM_PROVIDER, internal_only
from app.routers import chat, materials

app = FastAPI(title="TUTIPAL AI")
app.include_router(chat.router)
app.include_router(materials.router)

DATABASE_URL = os.environ.get("DATABASE_URL", "")


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "provider": LLM_PROVIDER}


@app.get("/v1/db-check", dependencies=[Depends(internal_only)])
def db_check() -> dict:
    with psycopg.connect(DATABASE_URL, connect_timeout=5) as conn:
        row = conn.execute("SELECT extversion FROM pg_extension WHERE extname = 'vector'").fetchone()
    return {"pgvector": row[0] if row else None}
