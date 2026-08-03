package cn.kaoyu666.aibuilder.client.mode;

import cn.kaoyu666.aibuilder.Aibuilder;
import cn.kaoyu666.aibuilder.client.ai.AIClient;
import cn.kaoyu666.aibuilder.client.config.AIModelConfig;
import cn.kaoyu666.aibuilder.client.config.ModelConfig;
import cn.kaoyu666.aibuilder.client.schematic.SchematicHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI Builder 模式管理器 - 控制建筑生成模式的开关与消息处理
 */
public class BuilderMode {
    private static final BuilderMode INSTANCE = new BuilderMode();
    private boolean active = false;
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final AtomicInteger schematicCounter = new AtomicInteger(1);

    public static BuilderMode getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * 切换 AI Builder 模式
     */
    public void toggle() {
        active = !active;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        if (active) {
            client.player.sendMessage(
                    Text.translatable("aibuilder.prefix").formatted(Formatting.GREEN)
                            .append(Text.translatable("aibuilder.mode.entered").formatted(Formatting.GREEN))
                            .append(Text.literal("\n").append(Text.translatable("aibuilder.mode.entered.hint1").formatted(Formatting.GRAY)))
                            .append(Text.literal("\n").append(Text.translatable("aibuilder.mode.entered.hint2").formatted(Formatting.GRAY))),
                    false
            );

            ModelConfig model = AIModelConfig.getInstance().getActiveModel();
            if (model == null) {
                client.player.sendMessage(
                        Text.translatable("aibuilder.prefix").formatted(Formatting.RED)
                                .append(Text.translatable("aibuilder.mode.no_model").formatted(Formatting.RED)),
                        false
                );
            } else {
                client.player.sendMessage(
                        Text.translatable("aibuilder.prefix").formatted(Formatting.YELLOW)
                                .append(Text.translatable("aibuilder.mode.current_model").formatted(Formatting.YELLOW))
                                .append(Text.literal(model.toString()).formatted(Formatting.AQUA)),
                        false
                );
            }
        } else {
            client.player.sendMessage(
                    Text.translatable("aibuilder.prefix").formatted(Formatting.YELLOW)
                            .append(Text.translatable("aibuilder.mode.exited").formatted(Formatting.YELLOW)),
                    false
            );
        }
    }

    /**
     * 处理聊天消息（在 Builder 模式下拦截）
     * @return true 如果消息已被拦截处理
     */
    public boolean handleChatMessage(String message) {
        if (!active) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return false;

        if (processing.get()) {
            client.player.sendMessage(
                    Text.translatable("aibuilder.prefix").formatted(Formatting.YELLOW)
                            .append(Text.translatable("aibuilder.mode.processing").formatted(Formatting.YELLOW)),
                    false
            );
            return true;
        }

        ModelConfig model = AIModelConfig.getInstance().getActiveModel();
        if (model == null) {
            client.player.sendMessage(
                    Text.translatable("aibuilder.prefix").formatted(Formatting.RED)
                            .append(Text.translatable("aibuilder.mode.configure_first").formatted(Formatting.RED)),
                    false
            );
            return true;
        }

        // 标记正在处理
        processing.set(true);

        client.player.sendMessage(
                Text.translatable("aibuilder.prefix").formatted(Formatting.GREEN)
                        .append(Text.translatable("aibuilder.mode.generating", message).formatted(Formatting.WHITE)),
                false
        );

        // 异步发送请求
        String systemPrompt = SchematicHelper.getSystemPrompt();
        AIClient.sendChatRequest(model, systemPrompt, message)
                .thenAccept(response -> {
                    // 回到主线程处理结果
                    client.execute(() -> {
                        processing.set(false);

                        if (client.player == null) return;

                        // 显示思考过程
                        if (response.thinking() != null && !response.thinking().isEmpty()) {
                            String thinkingText = response.thinking();
                            // 分段显示思考内容（避免过长）
                            String[] lines = thinkingText.split("\n");
                            client.player.sendMessage(
                                    Text.translatable("aibuilder.prefix").formatted(Formatting.GREEN)
                                            .append(Text.translatable("aibuilder.mode.thinking_header").formatted(Formatting.DARK_GRAY)),
                                    false
                            );
                            for (String line : lines) {
                                if (!line.trim().isEmpty()) {
                                    client.player.sendMessage(
                                            Text.literal("  ").append(Text.literal(line).formatted(Formatting.GRAY)),
                                            false
                                    );
                                }
                            }
                        }

                        if (response.success() && response.content() != null && !response.content().isEmpty()) {
                            // 加载投影
                            String schematicName = "ai_build_" + schematicCounter.getAndIncrement();
                            boolean loaded = SchematicHelper.loadAISchematic(response.content(), schematicName);

                            if (loaded) {
                                client.player.sendMessage(
                                        Text.translatable("aibuilder.prefix").formatted(Formatting.GREEN)
                                                .append(Text.translatable("aibuilder.mode.build_done").formatted(Formatting.GREEN))
                                                .append(Text.literal("\n").append(Text.translatable("aibuilder.mode.build_done.hint").formatted(Formatting.GRAY))),
                                        false
                                );
                            } else {
                                client.player.sendMessage(
                                        Text.translatable("aibuilder.prefix").formatted(Formatting.RED)
                                                .append(Text.translatable("aibuilder.mode.load_failed").formatted(Formatting.RED)),
                                        false
                                );
                            }
                        } else {
                            client.player.sendMessage(
                                    Text.translatable("aibuilder.prefix").formatted(Formatting.RED)
                                            .append(Text.translatable("aibuilder.mode.generate_failed").formatted(Formatting.RED))
                                            .append(Text.literal("\n" + (response.content() != null ? response.content() : I18n.translate("aibuilder.mode.unknown_error"))).formatted(Formatting.GRAY)),
                                    false
                            );
                        }
                    });
                })
                .exceptionally(e -> {
                    client.execute(() -> {
                        processing.set(false);
                        if (client.player != null) {
                            client.player.sendMessage(
                                    Text.translatable("aibuilder.prefix").formatted(Formatting.RED)
                                            .append(Text.translatable("aibuilder.mode.request_exception", e.getMessage()).formatted(Formatting.RED)),
                                    false
                            );
                        }
                    });
                    return null;
                });

        return true; // 消息已被拦截
    }
}
