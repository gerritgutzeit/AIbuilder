# AIbuilder

AI-driven Minecraft building generator mod. Describe a building in chat, and AIbuilder asks an AI model to generate it, then loads the result directly into your world as a [Litematica](https://www.curseforge.com/minecraft/mc-mods/litematica) projection.

[中文文档](README_zh.md)

## Features

- **Chat-driven building** — Enter build mode with `/aibuilder`, describe any building in chat, and the AI generates it for you
- **Litematic integration** — AI output is converted into a valid `.litematic` schematic and automatically loaded as a Litematica placement in front of you
- **Multi-provider support** — Works with many AI providers, plus any OpenAI-compatible or Anthropic-compatible endpoint (self-hosted, relays, etc.)
- **In-game configuration UI** — Add, remove, and switch model configurations through Mod Menu, including fetching the model list from a provider
- **Thinking process display** — Shows the model's reasoning output (when supported) in chat before the building is placed
- **32 languages** — Follows your Minecraft language setting (English by default), including `zh_cn`, `zh_tw`, `ja_jp`, `ko_kr`, `ru_ru`, `de_de`, `fr_fr`, `es_es`, `lzh`, and more

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.20.6 |
| Fabric Loader | >= 0.19.3 |
| Fabric API | * |
| [Litematica](https://www.curseforge.com/minecraft/mc-mods/litematica) | >= 0.18.0 |
| [MaLiLib](https://www.curseforge.com/minecraft/mc-mods/malilib) | >= 0.19.0 |
| [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) | >= 10.0.0 |

Java 21 is required.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.20.6
2. Download `aibuilder-*.jar` from [Releases](https://gitlab.com/kaoyu666/AIbuilder/-/releases) (or build it yourself, see below)
3. Put the jar together with Fabric API, Litematica, MaLiLib, and Mod Menu into your `.minecraft/mods` folder
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
git clone https://gitlab.com/kaoyu666/AIbuilder.git
cd AIbuilder
./gradlew build
```

The output jar is in `build/libs/`. To launch a development client with the mod, use `./gradlew runClient`.

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
