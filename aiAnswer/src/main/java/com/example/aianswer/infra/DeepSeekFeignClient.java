package com.example.aianswer.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用 OpenFeign 调用 DeepSeek Chat API。
 * 便于后续对比 RestTemplate 与 Feign 的性能和可维护性。
 */
@Component
public class DeepSeekFeignClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekFeignClient.class);

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private DeepSeekFeignApi createClient() {
        return Feign.builder()
                .encoder(new JacksonEncoder(new ObjectMapper()))
                .decoder(new JacksonDecoder(new ObjectMapper()))
                .logger(new Slf4jLogger(DeepSeekFeignApi.class))
                .logLevel(feign.Logger.Level.BASIC)
                .target(DeepSeekFeignApi.class, baseUrl);
    }

    /**
     * 调用 DeepSeek 获取回复（使用 Feign）
     */
    public String chat(String userContent, Map<String, Object> extraContext) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("DeepSeek API Key 未配置，返回本地提示文案（Feign）");
            return "后端 DeepSeek API Key 未配置，请先在后端配置后再重试。（Feign）";
        }

        DeepSeekFeignApi client = createClient();

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一名专业的中文问诊助手，请用简洁友好的中文回答用户问题。");

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userContent);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", Arrays.asList(systemMessage, userMessage));
        if (extraContext != null && !extraContext.isEmpty()) {
            body.put("metadata", extraContext);
        }

        try {
            Map<String, Object> resp = client.chat(apiKey, body);
            log.debug("DeepSeek (Feign) 原始响应: {}", resp);

            if (resp == null) {
                return "DeepSeek 返回为空。（Feign）";
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
                                return content.toString();
                            }
                        }
                    }
                }
            }
            return "未能从 DeepSeek 响应中解析到内容。（Feign）";
        } catch (Exception e) {
            log.error("使用 Feign 调用 DeepSeek 接口失败", e);
            return "使用 Feign 调用 DeepSeek 接口失败：" + e.getMessage();
        }
    }

    /**
     * Feign API 声明接口
     */
    interface DeepSeekFeignApi {

        @RequestLine("POST /v1/chat/completions")
        @Headers({
                "Content-Type: application/json",
                "Authorization: Bearer {apiKey}"
        })
        Map<String, Object> chat(String apiKey, Map<String, Object> body);
    }
}

