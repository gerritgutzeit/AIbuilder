package cn.kaoyu666.aibuilder.client.gui;

import cn.kaoyu666.aibuilder.client.ai.AIClient;
import cn.kaoyu666.aibuilder.client.config.AIModelConfig;
import cn.kaoyu666.aibuilder.client.config.ModelConfig;
import cn.kaoyu666.aibuilder.client.config.ProviderType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * 添加 AI 模型提供商界面
 * 步骤: 选择提供商 → 输入API Key → 获取模型列表 → 选择模型 → 保存
 */
public class AddProviderScreen extends Screen {
    private final Screen parent;

    // 状态
    private ProviderType selectedProvider = null;
    private List<String> fetchedModels = new ArrayList<>();
    private String selectedModel = null;
    private boolean fetching = false;
    private String statusMessage = "";
    private Formatting statusFormat = Formatting.WHITE;

    // 输入框
    private TextFieldWidget nameField;
    private TextFieldWidget customUrlField;
    private TextFieldWidget apiKeyField;

    // 输入内容状态（init() 重建控件时保留用户输入）
    private String nameText = "";
    private String customUrlText = "";
    private String apiKeyText = "";

    // 按钮引用
    private ButtonWidget fetchModelsBtn;
    private ButtonWidget saveBtn;
    private ButtonWidget modelSelectBtn;

    public AddProviderScreen(Screen parent) {
        super(Text.literal("添加AI模型"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();

        int centerX = this.width / 2;
        int y = 35;
        int fullWidth = 260;
        int halfWidth = 120;
        int spacing = 24;
        int labelHeight = 10;

        // ---- 第一步: 选择提供商 ----
        // 提供商选择按钮（每行3个）
        ProviderType[] providers = ProviderType.values();
        int cols = 3;
        int btnW = (fullWidth * 2 + 20) / cols - 4;
        int startX = centerX - (fullWidth + 10);

        for (int i = 0; i < providers.length; i++) {
            ProviderType p = providers[i];
            int col = i % cols;
            int row = i / cols;
            int bx = startX + col * (btnW + 4);
            int by = y + row * 22;

            boolean isSelected = (p == selectedProvider);
            Text btnText = Text.literal(p.getDisplayName())
                    .formatted(isSelected ? Formatting.GREEN : Formatting.WHITE);

            addDrawableChild(ButtonWidget.builder(btnText, btn -> {
                        selectedProvider = p;
                        fetchedModels.clear();
                        selectedModel = null;
                        nameText = "";
                        init();
                    })
                    .dimensions(bx, by, btnW, 20)
                    .build());
        }

        y += ((providers.length + cols - 1) / cols) * 22 + 15;

        // 自定义URL输入（仅CUSTOM时可见）
        if (selectedProvider == ProviderType.CUSTOM) {
            customUrlField = new TextFieldWidget(this.textRenderer, centerX - fullWidth / 2, y, fullWidth, 20, Text.literal("API Base URL"));
            customUrlField.setMaxLength(512);
            customUrlField.setText(this.customUrlText);
            customUrlField.setPlaceholder(Text.literal("输入API基础地址，如 https://your-api.com/v1"));
            customUrlField.setChangedListener(s -> this.customUrlText = s);
            addDrawableChild(customUrlField);
            y += spacing + 5;
        }

        // API Key 输入
        apiKeyField = new TextFieldWidget(this.textRenderer, centerX - fullWidth / 2, y, fullWidth, 20, Text.literal("API Key"));
        apiKeyField.setMaxLength(512);
        apiKeyField.setText(this.apiKeyText);
        apiKeyField.setPlaceholder(Text.literal("输入你的API Key"));
        apiKeyField.setChangedListener(s -> this.apiKeyText = s);
        addDrawableChild(apiKeyField);
        y += spacing;

        // 配置名称
        nameField = new TextFieldWidget(this.textRenderer, centerX - fullWidth / 2, y, fullWidth, 20, Text.literal("名称"));
        nameField.setMaxLength(64);
        nameField.setPlaceholder(Text.literal("给此配置起个名字（可选）"));
        if (!nameText.isEmpty()) {
            nameField.setText(nameText);
        } else if (selectedProvider != null) {
            nameField.setText(selectedProvider.getDisplayName());
        }
        nameField.setChangedListener(s -> this.nameText = s);
        addDrawableChild(nameField);
        y += spacing + 5;

        // 获取模型列表按钮
        fetchModelsBtn = ButtonWidget.builder(
                        Text.literal(fetching ? "获取中..." : "获取模型列表"),
                        btn -> fetchModels())
                .dimensions(centerX - fullWidth / 2, y, fullWidth, 20)
                .build();
        fetchModelsBtn.active = selectedProvider != null && !fetching;
        addDrawableChild(fetchModelsBtn);
        y += spacing + 5;

        // 模型选择按钮
        if (!fetchedModels.isEmpty()) {
            String modelLabel = selectedModel != null ? selectedModel : "点击选择模型";
            Formatting modelFormat = selectedModel != null ? Formatting.GREEN : Formatting.YELLOW;

            modelSelectBtn = ButtonWidget.builder(
                            Text.literal("模型: " + truncate(modelLabel, 30)).formatted(modelFormat),
                            btn -> {
                                if (client != null) {
                                    client.setScreen(new ModelSelectScreen(this, fetchedModels, model -> {
                                        selectedModel = model;
                                        client.setScreen(this);
                                    }));
                                }
                            })
                    .dimensions(centerX - fullWidth / 2, y, fullWidth, 20)
                    .build();
            addDrawableChild(modelSelectBtn);
            y += spacing;
        }

        // 保存按钮
        saveBtn = ButtonWidget.builder(
                        Text.literal("保存配置").formatted(Formatting.GREEN),
                        btn -> saveModel())
                .dimensions(centerX - halfWidth / 2, y, halfWidth, 20)
                .build();
        saveBtn.active = selectedProvider != null && selectedModel != null;
        addDrawableChild(saveBtn);

        // 返回按钮
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("← 返回"),
                        btn -> close())
                .dimensions(10, 10, 60, 20)
                .build());
    }

    private void fetchModels() {
        if (selectedProvider == null) return;

        String apiKey = this.apiKeyText;
        String baseUrl = selectedProvider == ProviderType.CUSTOM ? this.customUrlText : "";

        fetching = true;
        statusMessage = "正在获取模型列表...";
        statusFormat = Formatting.YELLOW;
        init();

        AIClient.fetchModels(selectedProvider, baseUrl, apiKey)
                .thenAccept(models -> {
                    fetching = false;
                    if (models.isEmpty()) {
                        statusMessage = "未获取到模型，请检查API Key和URL";
                        statusFormat = Formatting.RED;
                    } else {
                        fetchedModels = new ArrayList<>(models);
                        statusMessage = "获取到 " + models.size() + " 个模型";
                        statusFormat = Formatting.GREEN;
                        if (!models.isEmpty()) {
                            selectedModel = models.get(0);
                        }
                    }
                    if (client != null) {
                        client.execute(() -> init());
                    }
                })
                .exceptionally(e -> {
                    fetching = false;
                    statusMessage = "获取失败: " + e.getMessage();
                    statusFormat = Formatting.RED;
                    if (client != null) {
                        client.execute(() -> init());
                    }
                    return null;
                });
    }

    private void saveModel() {
        if (selectedProvider == null || selectedModel == null) return;

        String name = !nameText.isEmpty() ? nameText : selectedProvider.getDisplayName();
        String apiKey = this.apiKeyText;
        String baseUrl = selectedProvider == ProviderType.CUSTOM ? this.customUrlText : "";

        ModelConfig config = new ModelConfig(name, selectedProvider, baseUrl, apiKey, selectedModel);
        AIModelConfig.getInstance().addModel(config);

        statusMessage = "已保存: " + name;
        statusFormat = Formatting.GREEN;

        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // 标题
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("添加AI模型").formatted(Formatting.GOLD, Formatting.BOLD),
                centerX, 12, 0xFFFFFF);

        // 步骤提示
        if (selectedProvider == null) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("① 请选择AI模型提供商").formatted(Formatting.AQUA),
                    centerX, 25, 0xFFFFFF);
        }

        // 状态消息
        if (!statusMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(statusMessage).formatted(statusFormat),
                    centerX, this.height - 20, 0xFFFFFF);
        }
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
