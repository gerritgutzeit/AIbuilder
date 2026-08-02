package cn.kaoyu666.aibuilder.client.gui;

import net.minecraft.client.gui.DrawContext;
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
        super(Text.literal("选择模型"));
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

        // 返回按钮
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("← 返回"),
                        btn -> close())
                .dimensions(10, 10, 60, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("选择AI模型").formatted(Formatting.GOLD, Formatting.BOLD),
                centerX, 15, 0xFFFFFF);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("共 " + models.size() + " 个可用模型").formatted(Formatting.GRAY),
                centerX, 28, 0xFFFFFF);
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
