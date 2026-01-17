package com.op.back.chat.dto;


import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatResponseDTO {
    private String messageId;

    // 내부용
    private String senderUid;

    // 화면용
    private String senderNickname;
    private String senderProfileImageUrl;

    private String content;
    private Timestamp createdAt;

    private boolean read;
}
