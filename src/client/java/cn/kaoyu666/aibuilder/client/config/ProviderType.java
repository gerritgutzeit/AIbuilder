package cn.kaoyu666.aibuilder.client.config;

import net.minecraft.client.resource.language.I18n;

import java.util.Locale;

/**
 * AI模型提供商类型枚举
 */
public enum ProviderType {
    OPENAI("https://api.openai.com/v1", "https://api.openai.com/v1/models"),
    CLAUDE("https://api.anthropic.com/v1", "https://api.anthropic.com/v1/models"),
    DASHSCOPE("https://dashscope.aliyuncs.com/compatible-mode/v1", "https://dashscope.aliyuncs.com/compatible-mode/v1/models"),
    DEEPSEEK("https://api.deepseek.com/v1", "https://api.deepseek.com/v1/models"),
    ZHIPU("https://open.bigmodel.cn/api/paas/v4", "https://open.bigmodel.cn/api/paas/v4/models"),
    XIAOMI_MIMO("https://api.xiaoai.mi.com/v1", "https://api.xiaoai.mi.com/v1/models"),
    CUSTOM("", ""),
    CLAUDE_COMPATIBLE("", "");

    private final String defaultBaseUrl;
    private final String modelsUrl;

    ProviderType(String defaultBaseUrl, String modelsUrl) {
        this.defaultBaseUrl = defaultBaseUrl;
        this.modelsUrl = modelsUrl;
    }

    public String getDisplayName() {
        return I18n.translate("aibuilder.provider." + name().toLowerCase(Locale.ROOT));
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String getModelsUrl() {
        return modelsUrl;
    }

    public String getChatUrl(String baseUrl) {
        String url = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : defaultBaseUrl;
        if (isAnthropicFormat()) {
            return url + "/messages";
        }
        return url + "/chat/completions";
    }

    public String getModelsEndpointUrl(String baseUrl) {
        String url = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : defaultBaseUrl;
        if (isAnthropicFormat()) {
            // 仅去掉末尾的 /v1，replace 会替换 URL 中所有 "/v1"，可能破坏自定义代理地址
            String base = url.endsWith("/v1") ? url.substring(0, url.length() - 3) : url;
            return base + "/v1/models";
        }
        return url + "/models";
    }

    /**
     * 是否使用 Anthropic Messages 协议（请求体、认证头、响应格式均与 OpenAI 不同）
     */
    public boolean isAnthropicFormat() {
        return this == CLAUDE || this == CLAUDE_COMPATIBLE;
    }

    public boolean isOpenAICompatible() {
        return !isAnthropicFormat();
    }

    /**
     * 无默认地址，需要用户在界面上填写 Base URL
     */
    public boolean needsCustomUrl() {
        return defaultBaseUrl.isEmpty();
    }
}
