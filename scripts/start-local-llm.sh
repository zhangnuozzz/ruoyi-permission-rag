#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKSPACE_DIR="$(cd "$PROJECT_DIR/../.." && pwd)"
export HF_HUB_OFFLINE=1 TRANSFORMERS_OFFLINE=1 HF_HUB_DISABLE_TELEMETRY=1
export VLLM_NO_USAGE_STATS=1 DO_NOT_TRACK=1
export VLLM_CACHE_ROOT="$WORKSPACE_DIR/.local-runtime/vllm-cache"
export TRITON_CACHE_DIR="$WORKSPACE_DIR/.local-runtime/triton-cache"
exec "$PROJECT_DIR/../.venv/bin/vllm" serve \
  "${MODEL_PATH:-$WORKSPACE_DIR/models/Qwen3-4B-Instruct-2507}" \
  --served-model-name rag-local --host 127.0.0.1 --port "${LLM_PORT:-8000}" \
  --max-model-len "${MAX_MODEL_LEN:-8192}" \
  --max-num-seqs "${MAX_NUM_SEQS:-4}" \
  --gpu-memory-utilization "${GPU_MEMORY_UTILIZATION:-0.80}" \
  --language-model-only \
  --disable-log-requests \
  --enforce-eager
