package com.example.aianswer.service;

import com.example.aianswer.dto.UserDTO;
import com.example.aianswer.entity.Conversation;
import com.example.aianswer.entity.Message;
import com.example.aianswer.entity.User;

public interface ConversationService {

    User ensureUser(UserDTO userDTO);

    Conversation ensureConversation(String conversationId, User user);

    Message saveMessage(Conversation conversation, String role, String content);
}

