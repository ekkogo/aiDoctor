package com.example.aianswer.service.impl;

import com.example.aianswer.dto.AiChatRequest;
import com.example.aianswer.dto.AiChatResponse;
import com.example.aianswer.entity.Conversation;
import com.example.aianswer.entity.User;
import com.example.aianswer.infra.DeepSeekClient;
import com.example.aianswer.service.AiChatService;
import com.example.aianswer.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private final ConversationService conversationService;
    private final DeepSeekClient deepSeekClient;

    public AiChatServiceImpl(ConversationService conversationService,
                             DeepSeekClient deepSeekClient) {
        this.conversationService = conversationService;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("message 不能为空");
        }

        // 调试：记录前端请求入参（谨慎打印隐私字段）
        log.info("收到前端 AI 问诊请求, conversationId={}, userOpenid={}, message={}",
                request.getConversationId(),
                request.getUser() != null ? request.getUser().getOpenid() : null,
                request.getMessage());

        // 1. 用户 & 会话
        User user = conversationService.ensureUser(request.getUser());
        Conversation conversation = conversationService.ensureConversation(request.getConversationId(), user);

        // 2. 记录用户问题
        conversationService.saveMessage(conversation, "user", request.getMessage());

        // 3. 调用 DeepSeek 作为 AI 引擎
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("conversationId", conversation.getConversationId());
        ctx.put("requestContext", request.getContext());
        String replyText = deepSeekClient.chat(request.getMessage(), ctx);

        // 4. 记录 AI 回复
        conversationService.saveMessage(conversation, "ai", replyText);

        // 5. 返回给前端
        AiChatResponse resp = new AiChatResponse();
        resp.setConversationId(conversation.getConversationId());
        resp.setReply(replyText);

        Map<String, Object> raw = new HashMap<>();
        raw.put("notice", "当前回复由 DeepSeek 模型生成，仅供参考，不构成医疗建议。");
        raw.put("echo", request.getMessage());
        raw.put("conversationId", conversation.getConversationId());
        raw.put("requestContext", request.getContext());
        resp.setRaw(raw);

        return resp;
    }
}

