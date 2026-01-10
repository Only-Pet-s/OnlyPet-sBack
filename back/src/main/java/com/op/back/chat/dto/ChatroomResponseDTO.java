package com.op.back.chat.dto;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatroomResponseDTO {
    private String roomId;

    private String otherUid;
    private String otherNickname;
    private String otherProfileImageUrl;

    private String lastMessage;
    private Timestamp lastMessageAt;
    private int unreadCount;
}
