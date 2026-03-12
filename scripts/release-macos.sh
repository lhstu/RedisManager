#!/usr/bin/env bash

set -euo pipefail

if [[ "${OSTYPE:-}" != darwin* ]]; then
  echo "This script only supports macOS." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

VERSION="${1:-}"
ENV_FILE="${2:-packaging/common/release.env}"
NOTARIZE="${NOTARIZE:-false}"

if [[ -z "$VERSION" ]]; then
  echo "Usage: $0 <version> [env-file]" >&2
  exit 1
fi

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

required_tools=(codesign spctl xcrun qlmanage sips iconutil)
for tool in "${required_tools[@]}"; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Missing required tool: $tool" >&2
    exit 1
  fi
done

if [[ -n "${REDISMANAGER_MAC_SIGN_IDENTITY:-}" ]]; then
  echo "Signing identity: ${REDISMANAGER_MAC_SIGN_IDENTITY}"
else
  echo "No signing identity configured. Build will be unsigned."
fi

./scripts/generate-icons.sh
./scripts/package-current-platform.sh "$VERSION" dmg

APP_PATH="build/packaging/image/RedisManager.app"
DMG_PATH="build/packaging/installer/RedisManager-${VERSION}.dmg"

if [[ -n "${REDISMANAGER_MAC_SIGN_IDENTITY:-}" ]]; then
  ./scripts/verify-macos-release.sh "$APP_PATH"
fi

if [[ "$NOTARIZE" == "true" ]]; then
  ./scripts/notarize-macos.sh "$DMG_PATH"
  ./scripts/verify-macos-release.sh "$APP_PATH" "$DMG_PATH"
fi

echo "Release artifacts:"
echo "  $APP_PATH"
echo "  $DMG_PATH"
echo "  build/packaging/installer/SHA256SUMS.txt"
