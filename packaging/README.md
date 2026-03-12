## Packaging

This directory stores packaging resources consumed by `jpackage`.

- `common/`: shared resources for all platforms.
- `macos/`: macOS-only resources such as `.icns` icons or additional package metadata.
- `windows/`: Windows-only resources such as `.ico` icons.
- `linux/`: Linux-only resources such as `.png` icons or desktop-entry fragments.

Current repository automation:

- `scripts/generate-icons.sh` renders `common/app-icon.svg` into `macos/RedisManager.icns` and `linux/RedisManager.png`.
- `scripts/package-current-platform.sh` runs tests, builds the installer for the host platform, and writes `SHA256SUMS.txt`.
- `scripts/release-macos.sh` runs the macOS release flow: icons, packaging, optional signature verification, optional notarization.
- `scripts/verify-macos-release.sh` validates the `.app` bundle signature and optionally validates a stapled `.dmg`.

### Common commands

Build an installer for the current platform:

```bash
./scripts/package-current-platform.sh 1.2.0
```

Override the installer type:

```bash
./scripts/package-current-platform.sh 1.2.0 pkg
```

Run the macOS release flow:

```bash
cp packaging/common/release.env.example packaging/common/release.env
./scripts/release-macos.sh 1.2.0
```

Generate icons before packaging:

```bash
./scripts/generate-icons.sh
```

Print resolved packaging settings:

```bash
./gradlew printPackagingInfo
```

Write checksums after packaging:

```bash
./gradlew -PreleaseVersion=1.2.0 writeReleaseChecksums
```

### macOS signing

Export these environment variables before packaging:

```bash
export REDISMANAGER_MAC_SIGN_IDENTITY="Developer ID Application: Your Name (TEAMID)"
export REDISMANAGER_MAC_SIGN_KEYCHAIN="/Users/you/Library/Keychains/login.keychain-db"
```

Then build:

```bash
./scripts/release-macos.sh 1.2.0
```

Optional notarization:

```bash
export APPLE_ID="you@example.com"
export TEAM_ID="TEAMID"
export APP_PASSWORD="app-specific-password"
export NOTARIZE=true
./scripts/release-macos.sh 1.2.0
```

You can use `APPLE_KEYCHAIN_PROFILE` instead of `APPLE_ID` / `TEAM_ID` / `APP_PASSWORD` if `xcrun notarytool store-credentials` has already been configured.
