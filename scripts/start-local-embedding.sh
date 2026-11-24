#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PYTHONPYCACHEPREFIX="$PROJECT_DIR/../../.local-runtime/python-cache"
export HF_HUB_OFFLINE=1 TRANSFORMERS_OFFLINE=1 HF_HUB_DISABLE_TELEMETRY=1
export CUDA_VISIBLE_DEVICES=''
cd "$PROJECT_DIR/scripts/model_services"
exec "$PROJECT_DIR/../.venv/bin/python" -m uvicorn embedding_server:app \
  --host 127.0.0.1 --port "${EMBEDDING_PORT:-8001}" --workers 1 --no-access-log
