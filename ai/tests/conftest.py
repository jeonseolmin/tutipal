import os
import sys
from pathlib import Path

# app 을 import 하기 전에 환경값을 고정 (deps 가 import 시점에 읽음)
os.environ["AI_INTERNAL_KEY"] = "test-key"
os.environ["LLM_PROVIDER"] = "ollama"
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
