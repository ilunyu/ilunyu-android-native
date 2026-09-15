#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NATIVE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LUNYU_ROOT="$(cd "${NATIVE_ROOT}/../lunyu" && pwd)"

echo "==> Compiling resources via lunyu/tools/build_flutter_content.py..."
python3 "${LUNYU_ROOT}/tools/build_flutter_content.py"

echo "==> Syncing compiled assets to ilunyu-android-native..."
rsync -av --delete \
  "${LUNYU_ROOT}/flutter_app/assets/content/" \
  "${NATIVE_ROOT}/app/src/main/assets/content/"

echo "==> Resource compilation complete!"
