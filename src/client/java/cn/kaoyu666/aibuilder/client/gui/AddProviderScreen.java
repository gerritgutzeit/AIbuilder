package cn.kaoyu666.aibuilder.client.gui;

import cn.kaoyu666.aibuilder.client.ai.AIClient;
import cn.kaoyu666.aibuilder.client.config.AIModelConfig;
import cn.kaoyu666.aibuilder.client.config.ModelConfig;
import cn.kaoyu666.aibuilder.client.config.ProviderType;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
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
    private Text statusMessage = new LiteralText("");
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
        super(new TranslatableText("aibuilder.add.title"));
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
            Text btnText = new LiteralText(p.getDisplayName())
                    .formatted(isSelected ? Formatting.GREEN : Formatting.WHITE);

            addDrawableChild(new ButtonWidget(bx, by, btnW, 20, btnText,
                    btn -> {
                        selectedProvider = p;
                        fetchedModels.clear();
                        selectedModel = null;
                        nameText = "";
                        init();
                    }));
        }

        y += ((providers.length + cols - 1) / cols) * 22 + 15;

        // 自定义URL输入（无默认地址的提供商可见）
        if (selectedProvider != null && selectedProvider.needsCustomUrl()) {
            customUrlField = new TextFieldWidget(this.textRenderer, centerX - fullWidth / 2, y, fullWidth, 20, new LiteralText("API Base URL"));
            customUrlField.setMaxLength(512);
            customUrlField.setText(this.customUrlText);
            customUrlField.setChangedListener(s -> this.customUrlText = s);
            addDrawableChild(customUrlField);
            y += spacing + 5;
        }

        // API Key 输入
        apiKeyField = new TextFieldWidget(this.textRenderer, centerX - fullWidth / 2, y, fullWidth, 20, new LiteralText("API Key"));
        apiKeyField.setMaxLength(512);
        apiKeyField.setText(this.apiKeyText);
        apiKeyField.setChangedListener(s -> this.apiKeyText = s);
        addDrawableChild(apiKeyField);
        y += spacing;

        // 配置名称
        nameField = new TextFieldWidget(this.textRenderer, centerX - fullWidth / 2, y, fullWidth, 20, new TranslatableText("aibuilder.add.name_field"));
        nameField.setMaxLength(64);
        if (!nameText.isEmpty()) {
            nameField.setText(nameText);
        } else if (selectedProvider != null) {
            nameField.setText(selectedProvider.getDisplayName());
        }
        nameField.setChangedListener(s -> this.nameText = s);
        addDrawableChild(nameField);
        y += spacing + 5;

        // 获取模型列表按钮
        fetchModelsBtn = new ButtonWidget(centerX - fullWidth / 2, y, fullWidth, 20,
                new TranslatableText(fetching ? "aibuilder.add.fetching" : "aibuilder.add.fetch_models"),
                btn -> fetchModels());
        fetchModelsBtn.active = selectedProvider != null && !fetching;
        addDrawableChild(fetchModelsBtn);
        y += spacing + 5;

        // 模型选择按钮
        if (!fetchedModels.isEmpty()) {
            String modelLabel = selectedModel != null ? truncate(selectedModel, 30) : null;
            Formatting modelFormat = selectedModel != null ? Formatting.GREEN : Formatting.YELLOW;
            Text modelBtnText = selectedModel != null
                    ? new TranslatableText("aibuilder.add.model", modelLabel).formatted(modelFormat)
                    : new TranslatableText("aibuilder.add.model_none").formatted(modelFormat);

            modelSelectBtn = new ButtonWidget(centerX - fullWidth / 2, y, fullWidth, 20,
                    modelBtnText,
                    btn -> {
                        if (client != null) {
                            client.setScreen(new ModelSelectScreen(this, fetchedModels, model -> {
                                selectedModel = model;
                                client.setScreen(this);
                            }));
                        }
                    });
            addDrawableChild(modelSelectBtn);
            y += spacing;
        }

        // 保存按钮
        saveBtn = new ButtonWidget(centerX - halfWidth / 2, y, halfWidth, 20,
                new TranslatableText("aibuilder.add.save").formatted(Formatting.GREEN),
                btn -> saveModel());
        saveBtn.active = selectedProvider != null && selectedModel != null;
        addDrawableChild(saveBtn);

        // 返回按钮
        addDrawableChild(new ButtonWidget(10, 10, 60, 20,
                new TranslatableText("aibuilder.ui.back"),
                btn -> close()));
    }

    private void fetchModels() {
        if (selectedProvider == null) return;

        String apiKey = this.apiKeyText;
        String baseUrl = selectedProvider.needsCustomUrl() ? this.customUrlText : "";

        fetching = true;
        statusMessage = new TranslatableText("aibuilder.add.fetching_status");
        statusFormat = Formatting.YELLOW;
        init();

        AIClient.fetchModels(selectedProvider, baseUrl, apiKey)
                .thenAccept(models -> {
                    // UI 状态的修改和界面刷新必须在客户端主线程执行，避免与渲染线程竞争
                    if (client != null) {
                        client.execute(() -> {
                            fetching = false;
                            if (models.isEmpty()) {
                                statusMessage = new TranslatableText("aibuilder.add.fetch_empty");
                                statusFormat = Formatting.RED;
                            } else {
                                fetchedModels = new ArrayList<>(models);
                                statusMessage = new TranslatableText("aibuilder.add.fetch_success", models.size());
                                statusFormat = Formatting.GREEN;
                                selectedModel = models.get(0);
                            }
                            init();
                        });
                    }
                })
                .exceptionally(e -> {
                    if (client != null) {
                        client.execute(() -> {
                            fetching = false;
                            statusMessage = new TranslatableText("aibuilder.add.fetch_failed", e.getMessage());
                            statusFormat = Formatting.RED;
                            init();
                        });
                    }
                    return null;
                });
    }

    private void saveModel() {
        if (selectedProvider == null || selectedModel == null) return;

        String name = !nameText.isEmpty() ? nameText : selectedProvider.getDisplayName();
        String apiKey = this.apiKeyText;
        String baseUrl = selectedProvider.needsCustomUrl() ? this.customUrlText : "";

        ModelConfig config = new ModelConfig(name, selectedProvider, baseUrl, apiKey, selectedModel);
        AIModelConfig.getInstance().addModel(config);

        statusMessage = new TranslatableText("aibuilder.add.saved", name);
        statusFormat = Formatting.GREEN;

        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        int centerX = this.width / 2;

        // 标题
        Text title = new TranslatableText("aibuilder.add.title").formatted(Formatting.GOLD, Formatting.BOLD);
        this.textRenderer.drawWithShadow(matrices, title,
                centerX - this.textRenderer.getWidth(title) / 2, 12, 0xFFFFFF);

        // 步骤提示
        if (selectedProvider == null) {
            Text step = new TranslatableText("aibuilder.add.step1").formatted(Formatting.AQUA);
            this.textRenderer.drawWithShadow(matrices, step,
                    centerX - this.textRenderer.getWidth(step) / 2, 25, 0xFFFFFF);
        }

        // 状态消息
        if (!statusMessage.getString().isEmpty()) {
            Text status = statusMessage.copy().formatted(statusFormat);
            this.textRenderer.drawWithShadow(matrices, status,
                    centerX - this.textRenderer.getWidth(status) / 2, this.height - 20, 0xFFFFFF);
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
