# Android Release 与防闪退冒烟

## 1. 生成 APK

在项目根目录执行：

```powershell
.\gradlew.bat assembleRelease
```

输出 APK：

- `app/build/outputs/apk/release/app-release.apk`

## 2. 一键真机冒烟

先确认手机已连接并开启 USB 调试，然后执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke_test_android.ps1
```

脚本会自动：

1. 构建 `release` APK
2. 安装到手机
3. 启动应用
4. 观察并抓取日志
5. 输出 `PASS/FAIL`

日志输出目录：

- `D:\temp\MiniTask\logs\smoke_yyyyMMdd_HHmmss.log`

## 3. 常用参数

```powershell
# 只验证安装和启动，不重新构建
powershell -ExecutionPolicy Bypass -File .\scripts\smoke_test_android.ps1 -SkipBuild

# 只抓日志和启动（APK 已安装）
powershell -ExecutionPolicy Bypass -File .\scripts\smoke_test_android.ps1 -SkipBuild -SkipInstall

# 观察时长 40 秒
powershell -ExecutionPolicy Bypass -File .\scripts\smoke_test_android.ps1 -ObserveSeconds 40

# 仅检查命令流程，不要求 adb 设备
powershell -ExecutionPolicy Bypass -File .\scripts\smoke_test_android.ps1 -DryRun

# 自定义临时目录和日志目录
powershell -ExecutionPolicy Bypass -File .\scripts\smoke_test_android.ps1 -TempRoot D:\temp\MiniTask -LogDir D:\temp\MiniTask\logs
```

## 4. 发布签名（可选）

如果要使用正式签名，在 `gradle.properties` 中添加：

```properties
RELEASE_STORE_FILE=your_keystore.jks
RELEASE_STORE_PASSWORD=***
RELEASE_KEY_ALIAS=***
RELEASE_KEY_PASSWORD=***
```

当前构建脚本在没有上述配置时会回退到 debug 签名，便于本地安装测试。
