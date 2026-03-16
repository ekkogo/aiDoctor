package com.example.aianswer.service;

import com.example.aianswer.dto.AiChatRequest;
import com.example.aianswer.dto.AiChatResponse;

public interface AiChatService {

    AiChatResponse chat(AiChatRequest request);
}

