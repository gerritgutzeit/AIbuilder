package cn.kaoyu666.aibuilder.client.ai;

import cn.kaoyu666.aibuilder.Aibuilder;
import cn.kaoyu666.aibuilder.client.config.ModelConfig;
import cn.kaoyu666.aibuilder.client.config.ProviderType;
import com.google.gson.*;
import net.minecraft.client.resource.language.I18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AI API 客户端 - 处理与各AI提供商的HTTP通信
 * 1.16.5 目标为 Java 8，使用 HttpURLConnection 而非 java.net.http
 */
public class AIClient {
    private static final Gson GSON = new Gson();

    /**
     * 异步获取提供商支持的模型列表
     */
    public static CompletableFuture<List<String>> fetchModels(ProviderType provider, String baseUrl, String apiKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = provider.getModelsEndpointUrl(baseUrl);
                HttpResult result = httpRequest("GET", url, provider, apiKey, null);
                if (result == null || result.status != 200) {
                    return Collections.emptyList();
                }

                return parseModelsList(result.body, provider);
            } catch (Exception e) {
                Aibuilder.LOGGER.error("Error fetching models", e);
                return Collections.emptyList();
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

                HttpResult result = httpRequest("POST", url, model.getProviderType(), model.getApiKey(), requestBody);
                if (result == null) {
                    return new AIResponse("", I18n.translate("aibuilder.error.request_exception", "network error"), false);
                }
                if (result.status != 200) {
                    return new AIResponse("", I18n.translate("aibuilder.error.http", result.status, result.body), false);
                }

                return parseChatResponse(result.body, model.getProviderType());
            } catch (Exception e) {
                Aibuilder.LOGGER.error("Error sending chat request", e);
                return new AIResponse("", I18n.translate("aibuilder.error.request_exception", e.getMessage()), false);
            }
        });
    }

    /**
     * 发送 HTTP 请求（GET/POST），返回响应体字符串；非 200 或异常返回 null
     */
    private static HttpResult httpRequest(String method, String urlString, ProviderType provider, String apiKey, String postBody)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("Accept", "application/json");
            addAuthHeaders(conn, provider, apiKey);

            if ("POST".equals(method)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                byte[] payload = postBody.getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(payload.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                }
            }

            int status = conn.getResponseCode();
            String body = readBody(conn, status);
            return new HttpResult(status, body);
        } finally {
            conn.disconnect();
        }
    }

    private static void addAuthHeaders(HttpURLConnection conn, ProviderType provider, String apiKey) {
        if (provider.isAnthropicFormat()) {
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
        } else {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        }
    }

    private static String readBody(HttpURLConnection conn, int status) throws IOException {
        InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static class HttpResult {
        private final int status;
        private final String body;

        HttpResult(int status, String body) {
            this.status = status;
            this.body = body;
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
                        String id = getStringOrEmpty(obj, "id");
                        if (!id.isEmpty()) {
                            models.add(id);
                        }
                    }
                }
            } else {
                // OpenAI 兼容格式
                if (json.has("data")) {
                    for (JsonElement el : json.getAsJsonArray("data")) {
                        JsonObject obj = el.getAsJsonObject();
                        String id = getStringOrEmpty(obj, "id");
                        // 过滤出聊天模型
                        if (!id.isEmpty() && !id.contains("whisper") && !id.contains("tts") && !id.contains("dall-e")
                                && !id.contains("embedding") && !id.contains("moderation")) {
                            models.add(id);
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
                        String type = getStringOrEmpty(block, "type");
                        if ("text".equals(type)) {
                            sb.append(getStringOrEmpty(block, "text"));
                        } else if ("thinking".equals(type)) {
                            thinkingSb.append(getStringOrEmpty(block, "thinking"));
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
                // 注意：部分 API 会返回 JSON null，直接 getAsString 会抛异常，需做空值保护
                thinking = getStringOrEmpty(message, "reasoning_content");
                if (thinking.isEmpty()) {
                    thinking = getStringOrEmpty(message, "reasoning");
                }

                content = getStringOrEmpty(message, "content");
            }

            // 从内容中分离思考部分和建筑数据
            AIResponse parsed = extractThinkingAndData(content);
            if (thinking.isEmpty() && !parsed.thinking().isEmpty()) {
                return parsed;
            } else if (!thinking.isEmpty()) {
                return new AIResponse(thinking, parsed.content(), parsed.success());
            }
            return parsed;
        } catch (Exception e) {
            Aibuilder.LOGGER.error("Error parsing chat response", e);
            return new AIResponse("", I18n.translate("aibuilder.error.parse_response", e.getMessage()), false);
        }
    }

    /**
     * 安全读取 JSON 字符串字段：字段不存在或值为 null 时返回空字符串
     */
    private static String getStringOrEmpty(JsonObject obj, String key) {
        if (obj.has(key)) {
            JsonElement el = obj.get(key);
            if (el != null && !el.isJsonNull()) {
                return el.getAsString();
            }
        }
        return "";
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

    /**
     * AI 响应结果（Java 8 兼容，代替 record）
     */
    public static class AIResponse {
        private final String thinking;
        private final String content;
        private final boolean success;

        public AIResponse(String thinking, String content, boolean success) {
            this.thinking = thinking;
            this.content = content;
            this.success = success;
        }

        public String thinking() {
            return thinking;
        }

        public String content() {
            return content;
        }

        public boolean success() {
            return success;
        }
    }
}
