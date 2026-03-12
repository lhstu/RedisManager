## RedisManager Release

包含产物：

- macOS `.dmg`
- Windows `.msi`
- Linux `.deb`
- `SHA256SUMS.txt`

## macOS 安装说明

当前发布的 macOS 安装包默认可能是未签名版本。

如果 macOS 阻止打开，请按下面步骤处理：

1. 将应用拖入 `Applications`
2. 在 Finder 中右键 `RedisManager.app`
3. 选择“打开”
4. 在系统弹窗中再次确认“打开”

如果系统仍然拦截：

1. 打开“系统设置”
2. 进入“隐私与安全性”
3. 在页面底部找到被阻止的应用提示
4. 点击“仍要打开”

说明：

- 未签名不代表程序不能运行，只是首次启动需要用户手动放行
- `SHA256SUMS.txt` 可用于校验安装包完整性

## 校验示例

```bash
shasum -a 256 RedisManager-*.dmg
```
