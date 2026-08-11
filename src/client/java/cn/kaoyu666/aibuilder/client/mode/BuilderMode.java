package cn.kaoyu666.aibuilder.client.mode;

import cn.kaoyu666.aibuilder.Aibuilder;
import cn.kaoyu666.aibuilder.client.ai.AIClient;
import cn.kaoyu666.aibuilder.client.config.AIModelConfig;
import cn.kaoyu666.aibuilder.client.config.ModelConfig;
import cn.kaoyu666.aibuilder.client.schematic.SchematicHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

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
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;

        if (active) {
            client.player.sendSystemMessage(
                    Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.GREEN)
                            .append(Component.translatable("aibuilder.mode.entered").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal("\n").append(Component.translatable("aibuilder.mode.entered.hint1").withStyle(ChatFormatting.GRAY)))
                            .append(Component.literal("\n").append(Component.translatable("aibuilder.mode.entered.hint2").withStyle(ChatFormatting.GRAY)))
            );

            ModelConfig model = AIModelConfig.getInstance().getActiveModel();
            if (model == null) {
                client.player.sendSystemMessage(
                        Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.RED)
                                .append(Component.translatable("aibuilder.mode.no_model").withStyle(ChatFormatting.RED))
                );
            } else {
                client.player.sendSystemMessage(
                        Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.YELLOW)
                                .append(Component.translatable("aibuilder.mode.current_model").withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(model.toString()).withStyle(ChatFormatting.AQUA))
                );
            }
        } else {
            client.player.sendSystemMessage(
                    Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.YELLOW)
                            .append(Component.translatable("aibuilder.mode.exited").withStyle(ChatFormatting.YELLOW))
            );
        }
    }

    /**
     * 处理聊天消息（在 Builder 模式下拦截）
     * @return true 如果消息已被拦截处理
     */
    public boolean handleChatMessage(String message) {
        if (!active) return false;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return false;

        if (processing.get()) {
            client.player.sendSystemMessage(
                    Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.YELLOW)
                            .append(Component.translatable("aibuilder.mode.processing").withStyle(ChatFormatting.YELLOW))
            );
            return true;
        }

        ModelConfig model = AIModelConfig.getInstance().getActiveModel();
        if (model == null) {
            client.player.sendSystemMessage(
                    Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.RED)
                            .append(Component.translatable("aibuilder.mode.configure_first").withStyle(ChatFormatting.RED))
            );
            return true;
        }

        // 标记正在处理
        processing.set(true);

        client.player.sendSystemMessage(
                Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("aibuilder.mode.generating", message).withStyle(ChatFormatting.WHITE))
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
                            client.player.sendSystemMessage(
                                    Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.GREEN)
                                            .append(Component.translatable("aibuilder.mode.thinking_header").withStyle(ChatFormatting.DARK_GRAY))
                            );
                            for (String line : lines) {
                                if (!line.trim().isEmpty()) {
                                    client.player.sendSystemMessage(
                                            Component.literal("  ").append(Component.literal(line).withStyle(ChatFormatting.GRAY))
                                    );
                                }
                            }
                        }

                        if (response.success() && response.content() != null && !response.content().isEmpty()) {
                            // 加载投影
                            String schematicName = "ai_build_" + schematicCounter.getAndIncrement();
                            boolean loaded = SchematicHelper.loadAISchematic(response.content(), schematicName);

                            if (loaded) {
                                client.player.sendSystemMessage(
                                        Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.GREEN)
                                                .append(Component.translatable("aibuilder.mode.build_done").withStyle(ChatFormatting.GREEN))
                                                .append(Component.literal("\n").append(Component.translatable("aibuilder.mode.build_done.hint").withStyle(ChatFormatting.GRAY)))
                                );
                            } else {
                                client.player.sendSystemMessage(
                                        Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.RED)
                                                .append(Component.translatable("aibuilder.mode.load_failed").withStyle(ChatFormatting.RED))
                                );
                            }
                        } else {
                            client.player.sendSystemMessage(
                                    Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.RED)
                                            .append(Component.translatable("aibuilder.mode.generate_failed").withStyle(ChatFormatting.RED))
                                            .append(Component.literal("\n" + (response.content() != null ? response.content() : I18n.get("aibuilder.mode.unknown_error"))).withStyle(ChatFormatting.GRAY))
                            );
                        }
                    });
                })
                .exceptionally(e -> {
                    client.execute(() -> {
                        processing.set(false);
                        if (client.player != null) {
                            client.player.sendSystemMessage(
                                    Component.translatable("aibuilder.prefix").withStyle(ChatFormatting.RED)
                                            .append(Component.translatable("aibuilder.mode.request_exception", e.getMessage()).withStyle(ChatFormatting.RED))
                            );
                        }
                    });
                    return null;
                });

        return true; // 消息已被拦截
    }
}
