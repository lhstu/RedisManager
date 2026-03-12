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

## macOS 未签名安装包

如果当前发布版本没有签名，macOS 首次启动可能会拦截应用。这种情况下应用通常仍然可用，只是需要手动放行：

1. 将应用拖入 `Applications`
2. 右键 `RedisManager.app`
3. 选择“打开”
4. 如果仍被拦截，到“系统设置 -> 隐私与安全性”里点击“仍要打开”

详细打包说明见 [packaging/README.md](packaging/README.md)。
