package cn.kaoyu666.aibuilder.client.ai;

import cn.kaoyu666.aibuilder.Aibuilder;
import cn.kaoyu666.aibuilder.client.config.ModelConfig;
import cn.kaoyu666.aibuilder.client.config.ProviderType;
import com.google.gson.*;
import net.minecraft.client.resource.language.I18n;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AI API 客户端 - 处理与各AI提供商的HTTP通信
 */
public class AIClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * 异步获取提供商支持的模型列表
     */
    public static CompletableFuture<List<String>> fetchModels(ProviderType provider, String baseUrl, String apiKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = provider.getModelsEndpointUrl(baseUrl);
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .GET();
                addAuthHeaders(reqBuilder, provider, apiKey);

                HttpResponse<String> response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    Aibuilder.LOGGER.error("Failed to fetch models: HTTP {}", response.statusCode());
                    return List.of();
                }

                return parseModelsList(response.body(), provider);
            } catch (Exception e) {
                Aibuilder.LOGGER.error("Error fetching models", e);
                return List.of();
            }
        });
    }

    /**
     * 异步发送聊天请求，支持流式输出
     */
    public static CompletableFuture<AIResponse> sendChatRequest(ModelConfig model, String systemPrompt, String userMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = model.getProviderType().getChatUrl(model.getBaseUrl());
                String requestBody = buildChatRequestBody(model, systemPrompt, userMessage);

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody));
                addAuthHeaders(reqBuilder, model.getProviderType(), model.getApiKey());

                HttpResponse<String> response = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    return new AIResponse("", I18n.translate("aibuilder.error.http", response.statusCode(), response.body()), false);
                }

                return parseChatResponse(response.body(), model.getProviderType());
            } catch (Exception e) {
                Aibuilder.LOGGER.error("Error sending chat request", e);
                return new AIResponse("", I18n.translate("aibuilder.error.request_exception", e.getMessage()), false);
            }
        });
    }

    private static void addAuthHeaders(HttpRequest.Builder builder, ProviderType provider, String apiKey) {
        if (provider.isAnthropicFormat()) {
            builder.header("x-api-key", apiKey);
            builder.header("anthropic-version", "2023-06-01");
        } else {
            builder.header("Authorization", "Bearer " + apiKey);
        }
    }

    private static String buildChatRequestBody(ModelConfig model, String systemPrompt, String userMessage) {
        ProviderType provider = model.getProviderType();
        JsonObject root = new JsonObject();

        if (provider.isAnthropicFormat()) {
            // Anthropic Messages API 格式
            root.addProperty("model", model.getModelName());
            root.addProperty("max_tokens", 8192);
            root.addProperty("system", systemPrompt);
            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", userMessage);
            messages.add(msg);
            root.add("messages", messages);
        } else {
            // OpenAI 兼容格式
            root.addProperty("model", model.getModelName());
            root.addProperty("max_tokens", 8192);
            root.addProperty("temperature", 0.3);
            JsonArray messages = new JsonArray();

            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);

            root.add("messages", messages);
        }

        return GSON.toJson(root);
    }

    private static List<String> parseModelsList(String responseBody, ProviderType provider) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            if (provider.isAnthropicFormat()) {
                // Anthropic 模型列表格式
                if (json.has("data")) {
                    for (JsonElement el : json.getAsJsonArray("data")) {
                        JsonObject obj = el.getAsJsonObject();
                        if (obj.has("id")) {
                            models.add(obj.get("id").getAsString());
                        }
                    }
                }
            } else {
                // OpenAI 兼容格式
                if (json.has("data")) {
                    for (JsonElement el : json.getAsJsonArray("data")) {
                        JsonObject obj = el.getAsJsonObject();
                        if (obj.has("id")) {
                            String id = obj.get("id").getAsString();
                            // 过滤出聊天模型
                            if (!id.contains("whisper") && !id.contains("tts") && !id.contains("dall-e")
                                    && !id.contains("embedding") && !id.contains("moderation")) {
                                models.add(id);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Aibuilder.LOGGER.error("Error parsing models list", e);
        }
        return models;
    }

    private static AIResponse parseChatResponse(String responseBody, ProviderType provider) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            String thinking = "";
            String content = "";

            if (provider.isAnthropicFormat()) {
                // Anthropic 响应格式
                if (json.has("content")) {
                    JsonArray contentArr = json.getAsJsonArray("content");
                    StringBuilder sb = new StringBuilder();
                    StringBuilder thinkingSb = new StringBuilder();
                    for (JsonElement el : contentArr) {
                        JsonObject block = el.getAsJsonObject();
                        String type = block.get("type").getAsString();
                        if ("text".equals(type)) {
                            sb.append(block.get("text").getAsString());
                        } else if ("thinking".equals(type) && block.has("thinking")) {
                            thinkingSb.append(block.get("thinking").getAsString());
                        }
                    }
                    content = sb.toString();
                    thinking = thinkingSb.toString();
                }
            } else {
                // OpenAI 兼容格式
                JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
                JsonObject message = choice.getAsJsonObject("message");

                // 提取 reasoning/thinking 内容（部分模型支持）
                if (message.has("reasoning_content")) {
                    thinking = message.get("reasoning_content").getAsString();
                } else if (message.has("reasoning")) {
                    thinking = message.get("reasoning").getAsString();
                }

                if (message.has("content")) {
                    content = message.get("content").getAsString();
                }
            }

            // 从内容中分离思考部分和建筑数据
            AIResponse parsed = extractThinkingAndData(content);
            if (thinking.isEmpty() && !parsed.thinking.isEmpty()) {
                return parsed;
            } else if (!thinking.isEmpty()) {
                return new AIResponse(thinking, parsed.content, parsed.success);
            }
            return parsed;
        } catch (Exception e) {
            Aibuilder.LOGGER.error("Error parsing chat response", e);
            return new AIResponse("", I18n.translate("aibuilder.error.parse_response", e.getMessage()), false);
        }
    }

    private static AIResponse extractThinkingAndData(String fullContent) {
        String thinking = "";
        String data = fullContent;
        boolean success = true;

        // 尝试从内容中提取 JSON 建筑数据块
        int jsonStart = fullContent.indexOf("```json");
        int jsonEnd = fullContent.indexOf("```", jsonStart + 7);

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            thinking = fullContent.substring(0, jsonStart).trim();
            data = fullContent.substring(jsonStart + 7, jsonEnd).trim();
        } else {
            // 尝试直接查找 JSON 对象
            int braceStart = fullContent.indexOf('{');
            int braceEnd = fullContent.lastIndexOf('}');
            if (braceStart >= 0 && braceEnd > braceStart) {
                thinking = fullContent.substring(0, braceStart).trim();
                data = fullContent.substring(braceStart, braceEnd + 1).trim();
            } else {
                success = false;
            }
        }

        return new AIResponse(thinking, data, success);
    }

    public record AIResponse(String thinking, String content, boolean success) {}
}
