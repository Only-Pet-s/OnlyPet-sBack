package com.op.back.chat.controller;

import com.op.back.auth.util.JwtUtil;
import com.op.back.chat.dto.ChatRequestDTO;
import com.op.back.chat.dto.ChatResponseDTO;
import com.op.back.chat.dto.ChatroomRequestDTO;
import com.op.back.chat.dto.ChatroomResponseDTO;
import com.op.back.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final JwtUtil jwtUtil;

    @PostMapping("/room")
    public ResponseEntity<?> getOrCreateRoom(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChatroomRequestDTO req
    ) {
        String sender = jwtUtil.getUid(authHeader.substring(7));
        String receiver = req.getReceiver();

//        return chatService.getOrCreateRoom(sender, receiver);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "roomId", chatService.getOrCreateRoom(sender, receiver)
                ));
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendMessage(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChatRequestDTO request
    ) {
        String uid = jwtUtil.getUid(authHeader.substring(7));
        chatService.sendMessage(uid, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/rooms")
    public List<ChatroomResponseDTO> myRooms(
            @RequestHeader("Authorization") String authHeader
    ) throws Exception {
        String uid = jwtUtil.getUid(authHeader.substring(7));

        return chatService.getMyChatRooms(uid);
    }

    @GetMapping("/messages")
    public List<ChatResponseDTO> messages(
            @RequestParam String roomId
    ) throws Exception {
        return chatService.getMessages(roomId);
    }

    @PostMapping("/enter")
    public void enterRoom(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String roomId
    ) {
        String uid = jwtUtil.getUid(authHeader.substring(7));
        try {
            chatService.enterRoom(roomId, uid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
