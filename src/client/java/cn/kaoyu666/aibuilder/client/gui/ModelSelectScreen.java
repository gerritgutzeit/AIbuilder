package cn.kaoyu666.aibuilder.client.gui;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
        super(Text.translatable("aibuilder.select.title"));
        this.parent = parent;
        this.models = models;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();

        int centerX = this.width / 2;
        int buttonWidth = 280;
        int buttonHeight = 20;
        int spacing = 22;
        int startY = 40;
        int maxVisible = 12;

        for (int i = scrollOffset; i < Math.min(models.size(), scrollOffset + maxVisible); i++) {
            String model = models.get(i);
            int y = startY + (i - scrollOffset) * spacing;

            addDrawableChild(ButtonWidget.builder(
                            Text.literal(truncate(model, 45)),
                            btn -> {
                                onSelect.accept(model);
                            })
                    .dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight)
                    .build());
        }

        // 翻页
        if (models.size() > maxVisible) {
            int navY = startY + maxVisible * spacing + 5;
            addDrawableChild(ButtonWidget.builder(Text.translatable("aibuilder.ui.prev_page"), btn -> {
                        scrollOffset = Math.max(0, scrollOffset - maxVisible);
                        init();
                    })
                    .dimensions(centerX - buttonWidth - 5, navY, buttonWidth, buttonHeight)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("aibuilder.ui.next_page"), btn -> {
                        scrollOffset = Math.min(models.size() - maxVisible, scrollOffset + maxVisible);
                        init();
                    })
                    .dimensions(centerX + 5, navY, buttonWidth, buttonHeight)
                    .build());
        }

        // 返回按钮
        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("aibuilder.ui.back"),
                        btn -> close())
                .dimensions(10, 10, 60, 20)
                .build());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        Text header = Text.translatable("aibuilder.select.header").formatted(Formatting.GOLD, Formatting.BOLD);
        this.textRenderer.drawWithShadow(matrices, header,
                centerX - this.textRenderer.getWidth(header) / 2, 15, 0xFFFFFF);

        Text count = Text.translatable("aibuilder.select.count", models.size()).formatted(Formatting.GRAY);
        this.textRenderer.drawWithShadow(matrices, count,
                centerX - this.textRenderer.getWidth(count) / 2, 28, 0xFFFFFF);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }
}
