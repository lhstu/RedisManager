#!/usr/bin/env bash

set -euo pipefail

if [[ "${OSTYPE:-}" != darwin* ]]; then
  echo "This script only supports macOS." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

APP_PATH="${1:-build/packaging/image/RedisManager.app}"
DMG_PATH="${2:-}"

if [[ ! -d "$APP_PATH" ]]; then
  echo "App bundle not found: $APP_PATH" >&2
  exit 1
fi

codesign --verify --deep --strict --verbose=2 "$APP_PATH"
codesign -dv --verbose=4 "$APP_PATH" 2>&1
spctl -a -vv "$APP_PATH"

if [[ -n "$DMG_PATH" ]]; then
  if [[ ! -f "$DMG_PATH" ]]; then
    echo "Installer not found: $DMG_PATH" >&2
    exit 1
  fi
  xcrun stapler validate "$DMG_PATH"
fi
