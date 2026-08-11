package cn.kaoyu666.aibuilder.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.Consumer;

/**
 * 模型选择界面 - 从获取到的模型列表中选择一个模型
 */
public class ModelSelectScreen extends Screen {
    private final Screen parent;
    private final List<String> models;
    private final Consumer<String> onSelect;
    private int scrollOffset = 0;

    public ModelSelectScreen(Screen parent, List<String> models, Consumer<String> onSelect) {
        super(new TranslatableText("aibuilder.select.title"));
        this.parent = parent;
        this.models = models;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();
        this.children.clear(); this.buttons.clear();

        int centerX = this.width / 2;
        int buttonWidth = 280;
        int buttonHeight = 20;
        int spacing = 22;
        int startY = 40;
        int maxVisible = 12;

        for (int i = scrollOffset; i < Math.min(models.size(), scrollOffset + maxVisible); i++) {
            String model = models.get(i);
            int y = startY + (i - scrollOffset) * spacing;

            addButton(new ButtonWidget(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight,
                    new LiteralText(truncate(model, 45)),
                    btn -> {
                        onSelect.accept(model);
                    }));
        }

        // 翻页
        if (models.size() > maxVisible) {
            int navY = startY + maxVisible * spacing + 5;
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

        // 返回按钮
        addButton(new ButtonWidget(10, 10, 60, 20,
                new TranslatableText("aibuilder.ui.back"),
                btn -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        Text header = new TranslatableText("aibuilder.select.header").formatted(Formatting.GOLD, Formatting.BOLD);
        this.textRenderer.drawWithShadow(matrices, header,
                centerX - this.textRenderer.getWidth(header) / 2, 15, 0xFFFFFF);

        Text count = new TranslatableText("aibuilder.select.count", models.size()).formatted(Formatting.GRAY);
        this.textRenderer.drawWithShadow(matrices, count,
                centerX - this.textRenderer.getWidth(count) / 2, 28, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (client != null) {
            client.openScreen(parent);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }
}
