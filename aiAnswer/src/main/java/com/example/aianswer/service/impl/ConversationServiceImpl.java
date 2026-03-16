package com.example.aianswer.service.impl;

import com.example.aianswer.dto.UserDTO;
import com.example.aianswer.entity.Conversation;
import com.example.aianswer.entity.Message;
import com.example.aianswer.entity.User;
import com.example.aianswer.repository.ConversationRepository;
import com.example.aianswer.repository.MessageRepository;
import com.example.aianswer.repository.UserRepository;
import com.example.aianswer.service.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationServiceImpl(UserRepository userRepository,
                                   ConversationRepository conversationRepository,
                                   MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    @Transactional
    public User ensureUser(UserDTO userDTO) {
        String openid = userDTO != null ? userDTO.getOpenid() : null;
        if (openid == null || openid.trim().isEmpty()) {
            // 如果前端暂时不给 openid，就创建一个匿名用户标识
            openid = "anonymous-" + UUID.randomUUID();
        }
        String finalOpenid = openid;
        Optional<User> optional = userRepository.findByOpenid(finalOpenid);
        User user = optional.orElseGet(() -> {
            User u = new User();
            u.setOpenid(finalOpenid);
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });
        if (userDTO != null) {
            user.setName(userDTO.getName());
            user.setPhone(userDTO.getPhone());
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public Conversation ensureConversation(String conversationId, User user) {
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            Optional<Conversation> optional = conversationRepository.findByConversationId(conversationId);
            if (optional.isPresent()) {
                Conversation conversation = optional.get();
                conversation.setUser(user);
                conversation.setUpdatedAt(LocalDateTime.now());
                return conversationRepository.save(conversation);
            }
        }

        Conversation conversation = new Conversation();
        conversation.setConversationId(UUID.randomUUID().toString());
        conversation.setUser(user);
        conversation.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setLastMessageAt(now);
        return conversationRepository.save(conversation);
    }

    @Override
    @Transactional
    public Message saveMessage(Conversation conversation, String role, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(saved.getCreatedAt());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return saved;
    }
}

