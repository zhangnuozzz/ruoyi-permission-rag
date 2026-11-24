"""Local CPU embedding endpoint; run with scripts/start-local-embedding.sh."""
import os
from pathlib import Path
from threading import Lock

os.environ.setdefault("HF_HUB_OFFLINE", "1")
os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")
os.environ.setdefault("HF_HUB_DISABLE_TELEMETRY", "1")

import torch
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import AutoModel, AutoTokenizer

WORKSPACE = Path(__file__).resolve().parents[4]
MODEL_PATH = Path(os.environ.get("EMBEDDING_MODEL_PATH", WORKSPACE / "models/bge-small-zh-v1.5"))
MODEL_NAME = "bge-small-zh-v1.5"
torch.set_num_threads(int(os.environ.get("EMBEDDING_CPU_THREADS", "4")))
tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH, local_files_only=True)
model = AutoModel.from_pretrained(MODEL_PATH, local_files_only=True).cpu().eval()
lock = Lock()
app = FastAPI(title="Local CPU embeddings", docs_url=None, redoc_url=None)


class EmbeddingRequest(BaseModel):
    input: str | list[str]
    model: str = MODEL_NAME
    encoding_format: str = "float"


class ChunkRequest(BaseModel):
    text: str
    max_tokens: int = 384
    overlap: int = 48


@app.post("/v1/chunks")
def chunks(request: ChunkRequest):
    if not 1 <= request.max_tokens <= 480 or not 0 <= request.overlap < request.max_tokens:
        raise HTTPException(400, "Invalid chunk token limits")
    offsets = tokenizer(request.text, add_special_tokens=False, return_offsets_mapping=True,
                        truncation=False)["offset_mapping"]
    result = []
    step = request.max_tokens - request.overlap
    for start in range(0, len(offsets), step):
        end = min(start + request.max_tokens, len(offsets))
        text = request.text[offsets[start][0]:offsets[end - 1][1]]
        if text.strip():
            result.append(text)
        if end == len(offsets):
            break
    return {"chunks": result}


@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_NAME, "device": "cpu", "dimensions": 512, "max_tokens": 512}


@app.post("/v1/embeddings")
def embed(request: EmbeddingRequest):
    if request.model != MODEL_NAME:
        raise HTTPException(400, "Unknown embedding model")
    if request.encoding_format != "float":
        raise HTTPException(400, "Only float encoding is supported")
    texts = [request.input] if isinstance(request.input, str) else request.input
    if not 1 <= len(texts) <= 32 or any(not text.strip() or len(text) > 16384 for text in texts):
        raise HTTPException(400, "Supply 1-32 nonempty texts, each at most 16384 characters")
    with lock, torch.inference_mode():
        tokens = tokenizer(texts, padding=True, truncation=False, return_tensors="pt")
        if tokens["input_ids"].shape[1] > 512:
            raise HTTPException(400, "Input exceeds 512 tokens; split document chunks before embedding")
        vectors = torch.nn.functional.normalize(model(**tokens).last_hidden_state[:, 0], p=2, dim=1)
        count = int(tokens["attention_mask"].sum())
        return {"object": "list", "model": MODEL_NAME,
                "data": [{"object": "embedding", "index": i, "embedding": v} for i, v in enumerate(vectors.tolist())],
                "usage": {"prompt_tokens": count, "total_tokens": count}}
