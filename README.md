# RedisManager

JavaFX + Lettuce 构建的 Redis 桌面客户端。

## 本地开发

要求：

- JDK 17

启动测试：

```bash
./gradlew test
```

当前平台打包：

```bash
./scripts/package-current-platform.sh 1.2.0
```

macOS 发布流程：

```bash
cp packaging/common/release.env.example packaging/common/release.env
./scripts/release-macos.sh 1.2.0
```

## CI / Release

- `ci.yml`：在 macOS / Windows / Linux 上执行构建和测试
- `release.yml`：当推送 `v*` tag 时构建安装包并发布到 GitHub Releases

详细打包说明见 [packaging/README.md](packaging/README.md)。
