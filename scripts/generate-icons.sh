#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SOURCE_SVG="${1:-$ROOT_DIR/packaging/common/app-icon.svg}"
MACOS_DIR="$ROOT_DIR/packaging/macos"
LINUX_DIR="$ROOT_DIR/packaging/linux"
ICONSET_DIR="$MACOS_DIR/RedisManager.iconset"
BASE_PNG="$MACOS_DIR/app-icon-1024.png"

if [[ ! -f "$SOURCE_SVG" ]]; then
  echo "SVG source not found: $SOURCE_SVG" >&2
  exit 1
fi

mkdir -p "$MACOS_DIR" "$LINUX_DIR" "$ICONSET_DIR"
rm -f "$MACOS_DIR/RedisManager.icns"
rm -f "$LINUX_DIR/RedisManager.png"
rm -rf "$ICONSET_DIR"
mkdir -p "$ICONSET_DIR"

qlmanage -t -s 1024 -o "$MACOS_DIR" "$SOURCE_SVG" >/dev/null 2>&1
if [[ -f "$MACOS_DIR/app-icon.svg.png" ]]; then
  mv "$MACOS_DIR/app-icon.svg.png" "$BASE_PNG"
elif [[ -f "$MACOS_DIR/$(basename "$SOURCE_SVG").png" ]]; then
  mv "$MACOS_DIR/$(basename "$SOURCE_SVG").png" "$BASE_PNG"
fi

if [[ ! -f "$BASE_PNG" ]]; then
  echo "Failed to render base PNG from SVG." >&2
  exit 1
fi

for size in 16 32 128 256 512; do
  sips -z "$size" "$size" "$BASE_PNG" --out "$ICONSET_DIR/icon_${size}x${size}.png" >/dev/null
  retina=$((size * 2))
  sips -z "$retina" "$retina" "$BASE_PNG" --out "$ICONSET_DIR/icon_${size}x${size}@2x.png" >/dev/null
done

iconutil -c icns "$ICONSET_DIR" -o "$MACOS_DIR/RedisManager.icns"
cp "$BASE_PNG" "$LINUX_DIR/RedisManager.png"

echo "Generated:"
echo "  $MACOS_DIR/RedisManager.icns"
echo "  $LINUX_DIR/RedisManager.png"
