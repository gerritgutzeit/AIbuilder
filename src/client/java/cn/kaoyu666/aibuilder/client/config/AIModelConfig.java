package cn.kaoyu666.aibuilder.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置管理器 - 负责加载和保存模型配置
 */
public class AIModelConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "aibuilder_models.json";

    private static AIModelConfig instance;
    private final Path configPath;
    private List<ModelConfig> models = new ArrayList<>();
    private int activeModelIndex = -1;

    private AIModelConfig(Path configDir) {
        this.configPath = configDir.resolve(CONFIG_FILE_NAME);
        load();
    }

    public static AIModelConfig getInstance() {
        if (instance == null) {
            Path configDir = Path.of("config");
            instance = new AIModelConfig(configDir);
        }
        return instance;
    }

    public static void init(Path configDir) {
        instance = new AIModelConfig(configDir);
    }

    public List<ModelConfig> getModels() {
        return models;
    }

    public ModelConfig getActiveModel() {
        if (activeModelIndex >= 0 && activeModelIndex < models.size()) {
            return models.get(activeModelIndex);
        }
        return null;
    }

    public int getActiveModelIndex() {
        return activeModelIndex;
    }

    public void setActiveModelIndex(int index) {
        this.activeModelIndex = index;
        save();
    }

    public void addModel(ModelConfig model) {
        models.add(model);
        if (models.size() == 1) {
            activeModelIndex = 0;
        }
        save();
    }

    public void removeModel(int index) {
        if (index >= 0 && index < models.size()) {
            models.remove(index);
            if (index < activeModelIndex) {
                // 删除的是激活模型之前的条目，激活索引需要左移，否则会错误地指向后一个模型
                activeModelIndex--;
            } else if (activeModelIndex >= models.size()) {
                activeModelIndex = models.size() - 1;
            }
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            ConfigData data = new ConfigData();
            data.models = models;
            data.activeModelIndex = activeModelIndex;
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(configPath), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            cn.kaoyu666.aibuilder.Aibuilder.LOGGER.error("Failed to save AIbuilder config", e);
        }
    }

    private void load() {
        if (!Files.exists(configPath)) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(configPath), StandardCharsets.UTF_8)) {
            Type type = TypeToken.get(ConfigData.class).getType();
            ConfigData data = GSON.fromJson(reader, type);
            if (data != null) {
                this.models = data.models != null ? data.models : new ArrayList<>();
                this.activeModelIndex = data.activeModelIndex;
            }
        } catch (Exception e) {
            cn.kaoyu666.aibuilder.Aibuilder.LOGGER.error("Failed to load AIbuilder config", e);
        }
    }

    private static class ConfigData {
        List<ModelConfig> models;
        int activeModelIndex;
    }
}
