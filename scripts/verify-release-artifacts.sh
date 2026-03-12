#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
INSTALLER_DIR="${1:-$ROOT_DIR/build/packaging/installer}"

if [[ ! -d "$INSTALLER_DIR" ]]; then
  echo "Installer directory not found: $INSTALLER_DIR" >&2
  exit 1
fi

shopt -s nullglob
artifacts=("$INSTALLER_DIR"/*)
shopt -u nullglob

if [[ ${#artifacts[@]} -eq 0 ]]; then
  echo "No installer artifacts found in $INSTALLER_DIR" >&2
  exit 1
fi

for artifact in "${artifacts[@]}"; do
  shasum -a 256 "$artifact"
done
