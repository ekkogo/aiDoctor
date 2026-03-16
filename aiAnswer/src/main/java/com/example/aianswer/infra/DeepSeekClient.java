package com.example.aianswer.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * 简单的 DeepSeek Chat 接入（兼容 OpenAI Chat Completions 风格）
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.system-prompt:你是一名专业的中文医生，请用简洁友好的中文回答用户问题，如果用户问题与医学无关或无法提供医学建议：请拒绝，并说明拒绝原因（不超过200字）。}")
    private String systemPrompt;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 调用 DeepSeek 获取回复
     */
    public String chat(String userContent, Map<String, Object> extraContext) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("DeepSeek API Key 未配置，返回本地提示文案");
            return "后端 DeepSeek API Key 未配置，请先在后端配置后再重试。";
        }

        String url = baseUrl + "/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userContent);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", Arrays.asList(systemMessage, userMessage));
        // 将额外 context 挂到自定义字段中，方便后续调试或扩展
        if (extraContext != null && !extraContext.isEmpty()) {
            body.put("metadata", extraContext);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            Map<?, ?> resp = restTemplate.postForObject(url, entity, Map.class);
            log.debug("DeepSeek 原始响应: {}", resp);

            if (resp == null) {
                return "DeepSeek 返回为空。";
            }
            Object choicesObj = resp.get("choices");
            if (choicesObj instanceof List) {
                List<?> list = (List<?>) choicesObj;
                if (!list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof Map) {
                        Map<?, ?> choice = (Map<?, ?>) first;
                        Object messageObj = choice.get("message");
                        if (messageObj instanceof Map) {
                            Map<?, ?> msg = (Map<?, ?>) messageObj;
                            Object content = msg.get("content");
                            if (content != null) {
                                return content.toString().trim();
                            }
                        }
                    }
                }
            }
            return "未能从 DeepSeek 响应中解析到内容。";
        } catch (Exception e) {
            log.error("调用 DeepSeek 接口失败", e);
            return "调用 DeepSeek 接口失败：" + e.getMessage();
        }
    }
}

