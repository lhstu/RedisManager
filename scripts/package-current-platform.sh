#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

VERSION="${1:-}"
INSTALLER_TYPE="${2:-}"

GRADLE_ARGS=()
if [[ -n "$VERSION" ]]; then
  GRADLE_ARGS+=("-PreleaseVersion=$VERSION")
fi
if [[ -n "$INSTALLER_TYPE" ]]; then
  GRADLE_ARGS+=("-PinstallerType=$INSTALLER_TYPE")
fi

./gradlew clean test packageInstallerCurrentPlatform writeReleaseChecksums "${GRADLE_ARGS[@]}"
