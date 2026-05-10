package com.kid.A0.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    public enum MessageType {
        TEXT,
        IMAGE,
        VIDEO
    }

    private String to;
    private String from;
    private MessageType type;
    private String content;
    private String mediaUrl;
    private Long timestamp;

}

