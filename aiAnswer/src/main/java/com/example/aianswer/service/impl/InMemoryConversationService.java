package com.example.aianswer.service.impl;

import com.example.aianswer.dto.UserDTO;
import com.example.aianswer.entity.Conversation;
import com.example.aianswer.entity.Message;
import com.example.aianswer.entity.User;
import com.example.aianswer.service.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开发阶段的内存版会话/消息管理实现，不依赖数据库。
 * 便于在尚未配置 MySQL 时先跑通 AI 问诊链路。
 */
// 开发阶段的内存实现目前不再使用；如需无数据库模式可恢复 @Service/@Primary。
// @Service
// @Primary
public class InMemoryConversationService implements ConversationService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryConversationService.class);

    private final Map<String, User> userStore = new ConcurrentHashMap<>();
    private final Map<String, Conversation> conversationStore = new ConcurrentHashMap<>();

    @Override
    public User ensureUser(UserDTO userDTO) {
        String openid = userDTO != null ? userDTO.getOpenid() : null;
        if (openid == null || openid.trim().isEmpty()) {
            openid = "anonymous-" + UUID.randomUUID();
        }
        final String key = openid;
        User user = userStore.computeIfAbsent(key, k -> {
            User u = new User();
            u.setOpenid(k);
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });
        if (userDTO != null) {
            user.setName(userDTO.getName());
            user.setPhone(userDTO.getPhone());
        }
        user.setUpdatedAt(LocalDateTime.now());
        log.debug("InMemory ensureUser, openid={}, name={}, phone={}", user.getOpenid(), user.getName(), user.getPhone());
        return user;
    }

    @Override
    public Conversation ensureConversation(String conversationId, User user) {
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            Conversation exists = conversationStore.get(conversationId);
            if (exists != null) {
                exists.setUser(user);
                exists.setUpdatedAt(LocalDateTime.now());
                return exists;
            }
        }

        Conversation conversation = new Conversation();
        String id = UUID.randomUUID().toString();
        conversation.setConversationId(id);
        conversation.setUser(user);
        conversation.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setLastMessageAt(now);

        conversationStore.put(id, conversation);
        log.debug("InMemory create conversation, id={}, userOpenid={}", id, user != null ? user.getOpenid() : null);
        return conversation;
    }

    @Override
    public Message saveMessage(Conversation conversation, String role, String content) {
        // 内存模式下，我们只简单更新会话的时间信息并打日志，不真正持久化消息。
        if (conversation != null) {
            conversation.setLastMessageAt(LocalDateTime.now());
            conversation.setUpdatedAt(LocalDateTime.now());
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        log.debug("InMemory saveMessage, convId={}, role={}, content={}",
                conversation != null ? conversation.getConversationId() : null,
                role, content);
        return message;
    }
}

