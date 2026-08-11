# AIbuilder

AI 驱动的 Minecraft 建筑生成模组。在聊天框中描述你想要的建筑,AIbuilder 会调用 AI 模型生成它,并直接以 [Litematica](https://www.curseforge.com/minecraft/mc-mods/litematica) 投影的形式加载到你的世界中。

[English](README.md)

## 功能特性

- **聊天驱动建造** — 输入 `/aibuilder` 进入建筑模式,在聊天框描述任意建筑,AI 即可为你生成
- **Litematic 集成** — AI 输出会被转换为合法的 `.litematic` 蓝图,并自动作为 Litematica 投影加载在你前方
- **多提供商支持** — 支持多种 AI 提供商,以及任何 OpenAI 兼容或 Anthropic 兼容的端点(自建服务、中转站等)
- **游戏内配置界面** — 通过 Mod Menu 添加、删除和切换模型配置,支持从提供商获取模型列表
- **思考过程展示** — 建筑生成前,在聊天中展示模型的推理输出(如模型支持)
- **32 种语言** — 跟随 Minecraft 语言设置(默认英语),包含 `zh_cn`、`zh_tw`、`ja_jp`、`ko_kr`、`ru_ru`、`de_de`、`fr_fr`、`es_es`、`lzh` 等

## 依赖要求

每个 Minecraft 版本对应一个独立分支,依赖版本如下:

| Minecraft | 分支 | Fabric Loader | Java | [Litematica](https://www.curseforge.com/minecraft/mc-mods/litematica) | [MaLiLib](https://www.curseforge.com/minecraft/mc-mods/malilib) | [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) |
|---|---|---|---|---|---|---|
| 1.16.5 | `1.16.5` | >= 0.14.25 | 8 | >= 0.0.0-dev.20210917.192300 | >= 0.10.0-dev.21 | >= 1.16.23 |
| 1.18.2 | `1.18.2` | >= 0.19.3 | 17 | >= 0.11.7 | >= 0.12.1 | >= 3.2.5 |
| 1.19.4 | `1.19.4` | >= 0.19.3 | 17 | >= 0.14.7 | >= 0.15.4 | >= 6.3.1 |
| 1.20.2 | `1.20.2` | >= 0.19.3 | 17 | >= 0.16.1 | >= 0.17.0 | >= 8.0.1 |
| 1.20.4 | `1.20.4` | >= 0.19.3 | 17 | >= 0.17.4 | >= 0.18.3 | >= 9.2.0 |
| 1.20.6 | `1.20.6` / `master` | >= 0.19.3 | 21 | >= 0.18.0 | >= 0.19.0 | >= 10.0.0 |
| 1.21.4 | `1.21.4` | >= 0.19.3 | 21 | >= 0.21.7 | >= 0.23.5 | >= 13.0.4 |
| 1.21.11 | `1.21.11` | >= 0.19.3 | 21 | >= 0.26.12 | >= 0.27.16 | >= 17.0.0 |
| 26.1.2 | `26.1.2` | >= 0.19.3 | 25 | >= 0.27.10 | >= 0.28.9 | >= 18.0.0 |
| 26.2 | `26.2` | >= 0.19.3 | 25 | >= 0.28.4 | >= 0.29.3 | >= 20.0.1 |

所有版本均依赖 **Fabric API**(任意版本,构建时按对应版本解析)。

> 注: 26.x 系列使用官方 Mojang 映射,不再需要 Yarn 映射;1.16.5 官方仅有 dev 版 Litematica/MaLiLib。

## 安装方法

1. 根据你的 Minecraft 版本安装对应版本的 [Fabric Loader](https://fabricmc.net/use/)(Java 版本见上表)
2. 从 [Releases](https://github.com/kaoyu555666777/AIbuilder/releases) 下载与你的 Minecraft 版本匹配的 `aibuilder-0.1.1-<版本号>.jar`(或自行构建,见下文)
3. 将该 jar 与 Fabric API、对应版本的 Litematica、MaLiLib、Mod Menu 一起放入 `.minecraft/mods` 目录
4. 启动游戏

## 使用方法

1. 打开 **Mod Menu → AIbuilder**,添加一个 AI 模型(选择提供商、输入 API Key、获取模型列表、选择模型并保存)
2. 在游戏中输入 `/aibuilder` 进入建筑生成模式
3. 在聊天框输入建筑描述,例如*"一栋带烟囱的温馨小木屋"*,按回车发送
4. 等待 AI 响应——生成的建筑会以 Litematica 投影形式出现在你前方
5. 使用 Litematica 快捷键调整投影位置,再次输入 `/aibuilder` 即可退出建筑模式

## 支持的提供商

| 提供商 | 协议 |
|---|---|
| OpenAI (ChatGPT) | OpenAI Chat Completions |
| Anthropic (Claude) | Anthropic Messages |
| 阿里云百炼 (DashScope) | OpenAI 兼容 |
| DeepSeek | OpenAI 兼容 |
| 智谱AI (GLM) | OpenAI 兼容 |
| 小米 MiMo | OpenAI 兼容 |
| 自定义 (OpenAI兼容) | OpenAI Chat Completions — 任意端点,自行填写 Base URL |
| 自定义 (Anthropic兼容) | Anthropic Messages — 任意端点,自行填写 Base URL |

模型配置保存在 `config/aibuilder_models.json`。

> **注意:** API Key 以明文形式存储在该文件中,请勿分享或提交到代码仓库。

## 工作原理

1. 建筑模式下发送的聊天消息会被拦截,连同定义严格 JSON 建筑格式的系统提示词一起发送给已配置的 AI 模型
2. AI 返回设计思路说明和 JSON 数据(`size` 尺寸与 `blocks` 方块列表,含坐标和方块 ID)
3. AIbuilder 解析 JSON,构建 Litematica 兼容的 NBT 结构(调色板 + 位打包方块状态),并保存到 `schematics/` 目录
4. 通过 Litematica API 加载蓝图,放置在玩家前方 5 格处

## 从源码构建

```bash
git clone https://github.com/kaoyu555666777/AIbuilder.git
cd AIbuilder
git checkout 1.20.6   # 切换到目标版本分支(master 即 1.20.6,例如 1.16.5 / 1.18.2 / 1.19.4 / 1.21.4 / 26.2 ...)
./gradlew build
```

产物位于 `build/libs/aibuilder-0.1.1-<版本号>.jar`。所有版本构建完成的 jar 也统一收集在 `dist/` 目录(该目录已被 git 忽略)。如需启动带该模组的开发客户端,使用 `./gradlew runClient`。

## 项目结构

```
src/
├── main/                          # 通用代码与资源
│   ├── java/.../Aibuilder.java    # 模组入口
│   └── resources/
│       ├── assets/aibuilder/lang/ # 32 个语言文件
│       └── fabric.mod.json
└── client/                        # 仅客户端代码
    └── java/.../client/
        ├── ai/AIClient.java       # HTTP 客户端,请求/响应处理
        ├── config/                # 模型配置与提供商类型
        ├── mode/BuilderMode.java  # 建筑模式与聊天拦截逻辑
        ├── schematic/             # AI JSON → Litematic 转换
        ├── gui/                   # Mod Menu 配置界面
        └── mixin/                 # 聊天发送拦截
```

## 许可证

LGPL-3.0
