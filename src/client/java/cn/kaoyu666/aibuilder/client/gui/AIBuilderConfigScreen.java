package cn.kaoyu666.aibuilder.client.gui;

import cn.kaoyu666.aibuilder.client.config.AIModelConfig;
import cn.kaoyu666.aibuilder.client.config.ModelConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * AI Builder 主配置界面
 * 显示已添加的模型列表，提供添加/删除/选择模型和退出功能
 */
public class AIBuilderConfigScreen extends Screen {
    private final Screen parent;
    private int scrollOffset = 0;

    public AIBuilderConfigScreen(Screen parent) {
        super(Text.literal("AIbuilder 配置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();

        int centerX = this.width / 2;
        int startY = 40;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;

        // 标题
        // (在 render 中绘制)

        // 添加模型按钮
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("+ 添加AI模型"),
                        btn -> {
                            if (client != null) {
                                client.setScreen(new AddProviderScreen(this));
                            }
                        })
                .dimensions(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight)
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
            Text btnText = Text.literal((isActive ? "▶ " : "  ") + label)
                    .formatted(isActive ? Formatting.GREEN : Formatting.WHITE);

            // 选择按钮
            addDrawableChild(ButtonWidget.builder(btnText, btn -> {
                        AIModelConfig.getInstance().setActiveModelIndex(index);
                        init(); // 刷新界面
                    })
                    .dimensions(centerX - buttonWidth / 2 - 30, y, buttonWidth - 20, buttonHeight)
                    .build());

            // 删除按钮
            addDrawableChild(ButtonWidget.builder(
                            Text.literal("✕").formatted(Formatting.RED),
                            btn -> {
                                AIModelConfig.getInstance().removeModel(index);
                                init(); // 刷新界面
                            })
                    .dimensions(centerX + buttonWidth / 2 - 20, y, 30, buttonHeight)
                    .build());
        }

        // 翻页按钮（如果模型多）
        if (models.size() > maxVisible) {
            int navY = listStartY + maxVisible * spacing + 5;
            addDrawableChild(ButtonWidget.builder(Text.literal("▲ 上一页"), btn -> {
                        scrollOffset = Math.max(0, scrollOffset - maxVisible);
                        init();
                    })
                    .dimensions(centerX - buttonWidth - 5, navY, buttonWidth, buttonHeight)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.literal("▼ 下一页"), btn -> {
                        scrollOffset = Math.min(models.size() - maxVisible, scrollOffset + maxVisible);
                        init();
                    })
                    .dimensions(centerX + 5, navY, buttonWidth, buttonHeight)
                    .build());
        }

        // 无模型提示
        if (models.isEmpty()) {
            // 在 render 中显示提示
        }

        // 关闭按钮
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("关闭"),
                        btn -> close())
                .dimensions(centerX - 100, this.height - 30, 200, buttonHeight)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // 标题
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("AIbuilder - AI模型配置").formatted(Formatting.GOLD, Formatting.BOLD),
                centerX, 15, 0xFFFFFF);

        // 当前激活模型提示
        ModelConfig active = AIModelConfig.getInstance().getActiveModel();
        if (active != null) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("当前使用: " + active.getName()).formatted(Formatting.AQUA),
                    centerX, this.height - 50, 0xFFFFFF);
        }

        // 无模型提示
        if (AIModelConfig.getInstance().getModels().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("尚未添加任何AI模型").formatted(Formatting.GRAY),
                    centerX, this.height / 2 - 20, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("点击上方「添加AI模型」按钮开始配置").formatted(Formatting.DARK_GRAY),
                    centerX, this.height / 2, 0xFFFFFF);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
