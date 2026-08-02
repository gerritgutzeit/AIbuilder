package cn.kaoyu666.aibuilder.client.config;

/**
 * 单个模型配置
 */
public class ModelConfig {
    private String name;
    private ProviderType providerType;
    private String baseUrl;
    private String apiKey;
    private String modelName;

    public ModelConfig() {}

    public ModelConfig(String name, ProviderType providerType, String baseUrl, String apiKey, String modelName) {
        this.name = name;
        this.providerType = providerType;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProviderType getProviderType() { return providerType; }
    public void setProviderType(ProviderType providerType) { this.providerType = providerType; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    @Override
    public String toString() {
        return name + " [" + providerType.getDisplayName() + " - " + modelName + "]";
    }
}
