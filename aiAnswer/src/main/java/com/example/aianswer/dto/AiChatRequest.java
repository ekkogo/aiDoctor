package com.example.aianswer.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AiChatRequest {

    private String conversationId;

    private String message;

    private UserDTO user;

    private Map<String, Object> context;
}

