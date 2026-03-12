#!/usr/bin/env bash

set -euo pipefail

if [[ "${OSTYPE:-}" != darwin* ]]; then
  echo "This script only supports macOS." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

DMG_PATH="${1:-}"
APPLE_ID="${APPLE_ID:-}"
TEAM_ID="${TEAM_ID:-}"
APP_PASSWORD="${APP_PASSWORD:-}"
APPLE_KEYCHAIN_PROFILE="${APPLE_KEYCHAIN_PROFILE:-}"

if [[ -z "$DMG_PATH" ]]; then
  latest_matches=(build/packaging/installer/RedisManager-*.dmg)
  if [[ -e "${latest_matches[0]:-}" ]]; then
    DMG_PATH="$(ls -t build/packaging/installer/RedisManager-*.dmg | head -n 1)"
  fi
fi

if [[ ! -f "$DMG_PATH" ]]; then
  echo "Installer not found: $DMG_PATH" >&2
  exit 1
fi

submit_args=()
if [[ -n "$APPLE_KEYCHAIN_PROFILE" ]]; then
  submit_args+=(--keychain-profile "$APPLE_KEYCHAIN_PROFILE")
else
  if [[ -z "$APPLE_ID" || -z "$TEAM_ID" || -z "$APP_PASSWORD" ]]; then
    echo "Set APPLE_KEYCHAIN_PROFILE or provide APPLE_ID, TEAM_ID and APP_PASSWORD." >&2
    exit 1
  fi
  submit_args+=(
    --apple-id "$APPLE_ID"
    --team-id "$TEAM_ID"
    --password "$APP_PASSWORD"
  )
fi

xcrun notarytool submit "$DMG_PATH" \
  "${submit_args[@]}" \
  --wait

xcrun stapler staple "$DMG_PATH"
xcrun stapler validate "$DMG_PATH"
