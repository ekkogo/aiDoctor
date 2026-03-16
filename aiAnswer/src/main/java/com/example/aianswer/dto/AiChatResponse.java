package com.example.aianswer.dto;

import lombok.Data;

@Data
public class AiChatResponse {

    private String conversationId;

    private String reply;

    /**
     * 预留给前端 raw 字段，可返回更结构化的诊断结果
     */
    private Object raw;
}

