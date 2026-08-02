package cn.kaoyu666.aibuilder.client.config;

/**
 * AI模型提供商类型枚举
 */
public enum ProviderType {
    OPENAI("OpenAI (ChatGPT)", "https://api.openai.com/v1", "https://api.openai.com/v1/models"),
    CLAUDE("Anthropic (Claude)", "https://api.anthropic.com/v1", "https://api.anthropic.com/v1/models"),
    DASHSCOPE("阿里云百炼 (DashScope)", "https://dashscope.aliyuncs.com/compatible-mode/v1", "https://dashscope.aliyuncs.com/compatible-mode/v1/models"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "https://api.deepseek.com/v1/models"),
    ZHIPU("智谱AI (GLM)", "https://open.bigmodel.cn/api/paas/v4", "https://open.bigmodel.cn/api/paas/v4/models"),
    XIAOMI_MIMO("小米 MiMo", "https://api.xiaoai.mi.com/v1", "https://api.xiaoai.mi.com/v1/models"),
    CUSTOM("自定义 (OpenAI兼容)", "", "");

    private final String displayName;
    private final String defaultBaseUrl;
    private final String modelsUrl;

    ProviderType(String displayName, String defaultBaseUrl, String modelsUrl) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.modelsUrl = modelsUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String getModelsUrl() {
        return modelsUrl;
    }

    public String getChatUrl(String baseUrl) {
        String url = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : defaultBaseUrl;
        if (this == CLAUDE) {
            return url + "/messages";
        }
        return url + "/chat/completions";
    }

    public String getModelsEndpointUrl(String baseUrl) {
        String url = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : defaultBaseUrl;
        if (this == CLAUDE) {
            return url.replace("/v1", "") + "/v1/models";
        }
        return url + "/models";
    }

    public boolean isOpenAICompatible() {
        return this != CLAUDE;
    }
}
