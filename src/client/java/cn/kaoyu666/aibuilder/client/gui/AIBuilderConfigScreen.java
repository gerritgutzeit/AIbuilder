package cn.kaoyu666.aibuilder.client.gui;

import cn.kaoyu666.aibuilder.client.config.AIModelConfig;
import cn.kaoyu666.aibuilder.client.config.ModelConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * AI Builder 主配置界面
 * 显示已添加的模型列表，提供添加/删除/选择模型和退出功能
 */
public class AIBuilderConfigScreen extends Screen {
    private final Screen parent;
    private int scrollOffset = 0;

    public AIBuilderConfigScreen(Screen parent) {
        super(Component.translatable("aibuilder.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        int centerX = this.width / 2;
        int startY = 40;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;

        // 添加模型按钮
        addRenderableWidget(Button.builder(
                        Component.translatable("aibuilder.config.add_model"),
                        btn -> {
                            if (minecraft != null) {
                                minecraft.setScreen(new AddProviderScreen(this));
                            }
                        })
                .bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight)
                .build());

        // 模型列表
        List<ModelConfig> models = AIModelConfig.getInstance().getModels();
        int activeIndex = AIModelConfig.getInstance().getActiveModelIndex();
        int listStartY = startY + spacing + 10;
        int maxVisible = 8;

        for (int i = scrollOffset; i < Math.min(models.size(), scrollOffset + maxVisible); i++) {
            ModelConfig model = models.get(i);
            int y = listStartY + (i - scrollOffset) * spacing;
            boolean isActive = (i == activeIndex);

            String label = model.getName() + " [" + model.getProviderType().getDisplayName() + "]";
            if (label.length() > 40) label = label.substring(0, 37) + "...";

            final int index = i;
            Component btnText = Component.literal((isActive ? "▶ " : "  ") + label)
                    .withStyle(isActive ? ChatFormatting.GREEN : ChatFormatting.WHITE);

            // 选择按钮
            addRenderableWidget(Button.builder(btnText, btn -> {
                        AIModelConfig.getInstance().setActiveModelIndex(index);
                        init(); // 刷新界面
                    })
                    .bounds(centerX - buttonWidth / 2 - 30, y, buttonWidth - 20, buttonHeight)
                    .build());

            // 删除按钮
            addRenderableWidget(Button.builder(
                            Component.literal("✕").withStyle(ChatFormatting.RED),
                            btn -> {
                                AIModelConfig.getInstance().removeModel(index);
                                init(); // 刷新界面
                            })
                    .bounds(centerX + buttonWidth / 2 - 20, y, 30, buttonHeight)
                    .build());
        }

        // 翻页按钮（如果模型多）
        if (models.size() > maxVisible) {
            int navY = listStartY + maxVisible * spacing + 5;
            addRenderableWidget(Button.builder(Component.translatable("aibuilder.ui.prev_page"), btn -> {
                        scrollOffset = Math.max(0, scrollOffset - maxVisible);
                        init();
                    })
                    .bounds(centerX - buttonWidth - 5, navY, buttonWidth, buttonHeight)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("aibuilder.ui.next_page"), btn -> {
                        scrollOffset = Math.min(models.size() - maxVisible, scrollOffset + maxVisible);
                        init();
                    })
                    .bounds(centerX + 5, navY, buttonWidth, buttonHeight)
                    .build());
        }

        // 关闭按钮
        addRenderableWidget(Button.builder(
                        Component.translatable("aibuilder.ui.close"),
                        btn -> onClose())
                .bounds(centerX - 100, this.height - 30, 200, buttonHeight)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        extractBackground(context, mouseX, mouseY, delta);
        super.extractRenderState(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // 标题
        context.centeredText(this.font,
                Component.translatable("aibuilder.config.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                centerX, 15, 0xFFFFFF);

        // 当前激活模型提示
        ModelConfig active = AIModelConfig.getInstance().getActiveModel();
        if (active != null) {
            context.centeredText(this.font,
                    Component.translatable("aibuilder.config.current", active.getName()).withStyle(ChatFormatting.AQUA),
                    centerX, this.height - 50, 0xFFFFFF);
        }

        // 无模型提示
        if (AIModelConfig.getInstance().getModels().isEmpty()) {
            context.centeredText(this.font,
                    Component.translatable("aibuilder.config.empty").withStyle(ChatFormatting.GRAY),
                    centerX, this.height / 2 - 20, 0xFFFFFF);
            context.centeredText(this.font,
                    Component.translatable("aibuilder.config.empty_hint").withStyle(ChatFormatting.DARK_GRAY),
                    centerX, this.height / 2, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
