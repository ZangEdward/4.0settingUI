# ICS Settings · 安卓 4.0 设置界面复刻（原生 APK）

一个**忠实还原 Android 4.0 (Ice Cream Sandwich, ICS) 系统设置**的安卓原生应用。

它不是用网页或自绘控件“模仿”出来的界面，而是**直接使用 AOSP 官方 Settings 源码**
（`android-4.0.4_r2.1`，`packages/apps/Settings`）中的资源
（图标、字符串、顶栏分类结构、Holo 主题）编译而成的**真实可安装 APK**，
在任意安卓手机上都能呈现原汁原味的 4.0 设置外观。

> 包名：`com.icssettings.app`
> 启动 Activity：`com.icssettings.app.IcsSettingsActivity`

---

## 特性

- ✅ **源码级还原**：全部图标（`ic_settings_*`）、顶栏分类与条目标题、字符串均取自 AOSP 4.0.4 源码，**不是手绘近似**。
- ✅ **原生渲染**：使用框架自带的 `Theme.Holo.Light.DarkActionBar` 与 `Preference`/`PreferenceFragment` 体系，
  因此能在**所有仍内置 Holo 主题的安卓版本（API 14+，即市面上几乎所有在用的设备）**上原样呈现 4.0 视觉与交互。
- ✅ **完整结构**：复刻了 ICS 设置的 4 大分类（无线和网络 / 设备 / 个人 / 系统）与 19 个二级面板
  （Wi-Fi、蓝牙、流量、声音、显示、存储、电池、应用、账户、定位、安全、语言输入、备份重置、基座、日期时间、无障碍、开发者选项、关于手机等）。
- ✅ **可安装 APK**：已用自签名密钥签名（v1/v2/v3），可直接 `adb install` 安装到手机。

## 与原版源码的关系

| 项目 | 来源 |
| --- | --- |
| 顶层分类与条目结构 | `res/xml/settings_headers.xml`（AOSP 4.0.4 同名文件，顺序/图标/标题一致） |
| 图标 | `res/drawable-{mdpi,hdpi,xhdpi}/ic_settings_*.png`（AOSP 官方 PNG，三档密度） |
| 字符串 | `res/values/strings.xml`、`res/values-zh-rCN/strings.xml`（标题取自源码，示例值本地化） |
| 主题 | `Theme.Holo.Light.DarkActionBar`（系统框架主题，跨版本一致） |
| 框架 | `PreferenceActivity` + `PreferenceFragment` + `preference-headers`（ICS 设置架构） |

源码归档：`https://android.googlesource.com/platform/packages/apps/Settings/+archive/android-4.0.4_r2.1.tar.gz`

## 项目结构

```
4.0settingUI/
├── ICS_Settings_4.0.apk        # 已签名的成品 APK（可直接安装）
├── build_apk.sh                # 手动构建脚本（aapt2 → javac → d8 → zipalign → apksigner）
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/icssettings/app/
│   │   ├── IcsSettingsActivity.java   # 顶层 PreferenceActivity，加载 settings_headers
│   │   └── PanelFragment.java         # 每个条目的面板，按 "panel" extra 选择布局
│   ├── res/
│   │   ├── values/strings.xml         # 英文（源码默认）
│   │   ├── values-zh-rCN/strings.xml  # 中文
│   │   ├── xml/settings_headers.xml    # 顶层仪表盘（源自源码）
│   │   ├── xml/panel_*.xml             # 19 个二级面板
│   │   └── drawable-{mdpi,hdpi,xhdpi}/ # AOSP 官方 ic_settings_* 图标
```

## 构建

无需 Gradle。需要 JDK 17 + Android SDK（platform android-33、build-tools 33.0.2）。
构建脚本 `build_apk.sh` 会依次执行：

1. `aapt2 compile` 编译全部资源
2. `aapt2 link` 链接生成未签名 APK 与 `R.java`
3. `javac` 编译 Java（release 11，UTF-8）
4. `d8` 将 class 转为 `classes.dex`
5. 将 `classes.dex` 放入 APK（不压缩）并 `zipalign`
6. `keytool` 生成密钥库 + `apksigner` 签名

```bash
bash build_apk.sh
# 产物：ICS_Settings_4.0.apk
```

> 注：`build_apk.sh` 中的签名密钥与口令为本演示自动生成，仅供本地签名使用；
> 正式发布请替换为自己的密钥库。

## 安装

```bash
adb install ICS_Settings_4.0.apk
```

安装后在启动器找到「Settings」即可打开。

## 许可

本仓库中的设置界面代码与图标资源改编自 AOSP `packages/apps/Settings`（Apache License 2.0）。
仅供学习、怀旧与演示用途。
