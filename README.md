# AIbuilder

AI-driven Minecraft building generator mod. Describe a building in chat, and AIbuilder asks an AI model to generate it, then loads the result directly into your world as a [Litematica](https://www.curseforge.com/minecraft/mc-mods/litematica) projection.

[中文](README_zh.md)

## Features

- **Chat-driven building** — Enter build mode with `/aibuilder`, describe any building in chat, and the AI generates it for you
- **Litematic integration** — AI output is converted into a valid `.litematic` schematic and automatically loaded as a Litematica placement in front of you
- **Multi-provider support** — Works with many AI providers, plus any OpenAI-compatible or Anthropic-compatible endpoint (self-hosted, relays, etc.)
- **In-game configuration UI** — Add, remove, and switch model configurations through Mod Menu, including fetching the model list from a provider
- **Thinking process display** — Shows the model's reasoning output (when supported) in chat before the building is placed
- **32 languages** — Follows your Minecraft language setting (English by default), including Chinese Simplified, Chinese Traditional, Japanese, Korean, Russian, German, French, Spanish, Classical Chinese, and more

## Requirements

Each Minecraft version has its own branch, with the following dependency versions:

| Minecraft | Branch | Fabric Loader | Java | [Litematica](https://www.curseforge.com/minecraft/mc-mods/litematica) | [MaLiLib](https://www.curseforge.com/minecraft/mc-mods/malilib) | [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) |
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

All versions depend on **Fabric API** (any version; the matching one is resolved at build time).

> Note: The 26.x line uses official Mojang mappings and no longer needs Yarn mappings; for 1.16.5 only dev builds of Litematica/MaLiLib are available.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version (see the table above for the required Java version)
2. Download the `aibuilder-0.1.1-<version>.jar` matching your Minecraft version from [Releases](https://github.com/kaoyu555666777/AIbuilder/releases) (or build it yourself, see below)
3. Put the jar together with Fabric API and the matching Litematica, MaLiLib, and Mod Menu versions into your `.minecraft/mods` folder
4. Launch the game

## Usage

1. Open **Mod Menu → AIbuilder** and add an AI model (choose a provider, enter your API key, fetch the model list, pick a model, and save)
2. In game, run `/aibuilder` to enter build mode
3. Type a building description in chat, e.g. *"a cozy wooden cottage with a chimney"*, and press Enter
4. Wait for the AI to respond — the finished building appears as a Litematica projection in front of you
5. Adjust the placement with Litematica hotkeys, then run `/aibuilder` again to exit build mode

## Supported Providers

| Provider | Protocol |
|---|---|
| OpenAI (ChatGPT) | OpenAI Chat Completions |
| Anthropic (Claude) | Anthropic Messages |
| Alibaba Cloud Bailian (DashScope) | OpenAI-compatible |
| DeepSeek | OpenAI-compatible |
| Zhipu AI (GLM) | OpenAI-compatible |
| Xiaomi MiMo | OpenAI-compatible |
| Custom (OpenAI-compatible) | OpenAI Chat Completions — any endpoint, you provide the base URL |
| Custom (Anthropic-compatible) | Anthropic Messages — any endpoint, you provide the base URL |

Model configurations are stored in `config/aibuilder_models.json`.

> **Note:** API keys are stored in plain text in that file. Do not share it or commit it to a repository.

## How It Works

1. Chat messages sent while build mode is active are intercepted and forwarded to the configured AI model along with a system prompt that defines a strict JSON building format
2. The AI replies with a design explanation plus a JSON payload (`size` and a list of `blocks` with coordinates and block IDs)
3. AIbuilder parses the JSON, builds a Litematica-compatible NBT structure (palette + packed block states), and saves it to `schematics/`
4. The schematic is loaded through Litematica's API and placed 5 blocks in front of the player

## Building from Source

```bash
git clone https://github.com/kaoyu555666777/AIbuilder.git
cd AIbuilder
git checkout 1.20.6   # Switch to the branch of your target version (master is 1.20.6; e.g. 1.16.5 / 1.18.2 / 1.19.4 / 1.21.4 / 26.2 ...)
./gradlew build
```

The output jar is at `build/libs/aibuilder-0.1.1-<version>.jar`. Ready-built jars for every version are also collected in the `dist/` directory (which is git-ignored). To launch a development client with the mod, use `./gradlew runClient`.

## Project Structure

```
src/
├── main/                          # Common code + resources
│   ├── java/.../Aibuilder.java    # Mod entrypoint
│   └── resources/
│       ├── assets/aibuilder/lang/ # 32 language files
│       └── fabric.mod.json
└── client/                        # Client-only code
    └── java/.../client/
        ├── ai/AIClient.java       # HTTP client, request/response handling
        ├── config/                # Model configuration & provider types
        ├── mode/BuilderMode.java  # Build mode & chat interception logic
        ├── schematic/             # AI JSON → Litematic conversion
        ├── gui/                   # Mod Menu config screens
        └── mixin/                 # Chat send interception
```

## License

LGPL-3.0
