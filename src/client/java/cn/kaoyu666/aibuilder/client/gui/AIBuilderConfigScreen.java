package cn.kaoyu666.aibuilder.client.gui;

import cn.kaoyu666.aibuilder.client.config.AIModelConfig;
import cn.kaoyu666.aibuilder.client.config.ModelConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
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
        super(new TranslatableText("aibuilder.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.children.clear(); this.buttons.clear();

        int centerX = this.width / 2;
        int startY = 40;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;

        // 添加模型按钮
        addButton(new ButtonWidget(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight,
                new TranslatableText("aibuilder.config.add_model"),
                btn -> {
                    if (client != null) {
                        client.openScreen(new AddProviderScreen(this));
                    }
                }));

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
            Text btnText = new LiteralText((isActive ? "▶ " : "  ") + label)
                    .formatted(isActive ? Formatting.GREEN : Formatting.WHITE);

            // 选择按钮
            addButton(new ButtonWidget(centerX - buttonWidth / 2 - 30, y, buttonWidth - 20, buttonHeight,
                    btnText,
                    btn -> {
                        AIModelConfig.getInstance().setActiveModelIndex(index);
                        init(); // 刷新界面
                    }));

            // 删除按钮
            addButton(new ButtonWidget(centerX + buttonWidth / 2 - 20, y, 30, buttonHeight,
                    new LiteralText("✕").formatted(Formatting.RED),
                    btn -> {
                        AIModelConfig.getInstance().removeModel(index);
                        init(); // 刷新界面
                    }));
        }

        // 翻页按钮（如果模型多）
        if (models.size() > maxVisible) {
            int navY = listStartY + maxVisible * spacing + 5;
            addButton(new ButtonWidget(centerX - buttonWidth - 5, navY, buttonWidth, buttonHeight,
                    new TranslatableText("aibuilder.ui.prev_page"),
                    btn -> {
                        scrollOffset = Math.max(0, scrollOffset - maxVisible);
                        init();
                    }));
            addButton(new ButtonWidget(centerX + 5, navY, buttonWidth, buttonHeight,
                    new TranslatableText("aibuilder.ui.next_page"),
                    btn -> {
                        scrollOffset = Math.min(models.size() - maxVisible, scrollOffset + maxVisible);
                        init();
                    }));
        }

        // 关闭按钮
        addButton(new ButtonWidget(centerX - 100, this.height - 30, 200, buttonHeight,
                new TranslatableText("aibuilder.ui.close"),
                btn -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // 标题
        Text header = new TranslatableText("aibuilder.config.header").formatted(Formatting.GOLD, Formatting.BOLD);
        this.textRenderer.drawWithShadow(matrices, header,
                centerX - this.textRenderer.getWidth(header) / 2, 15, 0xFFFFFF);

        // 当前激活模型提示
        ModelConfig active = AIModelConfig.getInstance().getActiveModel();
        if (active != null) {
            Text current = new TranslatableText("aibuilder.config.current", active.getName()).formatted(Formatting.AQUA);
            this.textRenderer.drawWithShadow(matrices, current,
                    centerX - this.textRenderer.getWidth(current) / 2, this.height - 50, 0xFFFFFF);
        }

        // 无模型提示
        if (AIModelConfig.getInstance().getModels().isEmpty()) {
            Text empty = new TranslatableText("aibuilder.config.empty").formatted(Formatting.GRAY);
            this.textRenderer.drawWithShadow(matrices, empty,
                    centerX - this.textRenderer.getWidth(empty) / 2, this.height / 2 - 20, 0xFFFFFF);
            Text emptyHint = new TranslatableText("aibuilder.config.empty_hint").formatted(Formatting.DARK_GRAY);
            this.textRenderer.drawWithShadow(matrices, emptyHint,
                    centerX - this.textRenderer.getWidth(emptyHint) / 2, this.height / 2, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }
}
