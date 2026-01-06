package com.op.back.chat.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.op.back.chat.dto.ChatRequestDTO;
import com.op.back.chat.dto.ChatResponseDTO;
import com.op.back.chat.dto.ChatroomResponseDTO;
import com.op.back.chat.util.ChatroomUtil;
import com.op.back.fcm.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final Firestore firestore;
    private final FcmService fcmService;

    // 유저 프로필 조회
    private Map<String, String> getUserProfile(String uid) {
        try {
            DocumentSnapshot snap =
                    firestore.collection("users").document(uid).get().get();

            if (!snap.exists()) {
                return Map.of(
                        "nickname", "알 수 없음",
                        "profileImageUrl", ""
                );
            }

            return Map.of(
                    "nickname", snap.getString("nickname"),
                    "profileImageUrl", snap.getString("profileImageUrl")
            );
        } catch (Exception e) {
            throw new RuntimeException("유저 프로필 조회 실패");
        }
    }

    // 채팅방 생성 or 조회
    public String getOrCreateRoom(String uidA, String uidB) {
        String roomId = ChatroomUtil.createRoomId(uidA, uidB);

        firestore.runTransaction(tx -> {
            DocumentReference ref =
                    firestore.collection("chatRooms").document(roomId);

            DocumentSnapshot snap = tx.get(ref).get();
            if (!snap.exists()) {
                tx.set(ref, Map.of(
                        "participants", List.of(uidA, uidB),
                        "createdAt", Timestamp.now(),
                        "lastMessage", "",
                        "lastMessageAt", Timestamp.now(),
                        "lastSenderUid", "",
                        "unreadCount", Map.of(uidA, 0, uidB, 0)
                ));
            }
            return null;
        });

        return roomId;
    }

    // 채팅 보내기
    public void sendMessage(String uid, ChatRequestDTO dto) {

        String roomId =
                ChatroomUtil.createRoomId(uid, dto.getReceiverUid());

        // Firestore 트랜잭션
        firestore.runTransaction(tx -> {

            DocumentReference roomRef =
                    firestore.collection("chatRooms").document(roomId);

            DocumentSnapshot roomSnap = tx.get(roomRef).get();

            // 채팅방 없으면 생성
            if (!roomSnap.exists()) {
                tx.set(roomRef, Map.of(
                        "participants", List.of(uid, dto.getReceiverUid()),
                        "createdAt", Timestamp.now(),
                        "lastMessage", "",
                        "lastMessageAt", Timestamp.now(),
                        "lastSenderUid", "",
                        "unreadCount", Map.of(
                                uid, 0,
                                dto.getReceiverUid(), 0
                        )
                ));
            }

            // 메시지 저장
            DocumentReference msgRef =
                    roomRef.collection("messages").document();

            tx.set(msgRef, Map.of(
                    "senderUid", uid,
                    "content", dto.getContent(),
                    "type", "TEXT",
                    "createdAt", Timestamp.now(),
                    "read", false,
                    "deleted", false
            ));

            // 채팅방 요약 업데이트
            tx.update(roomRef, Map.of(
                    "lastMessage", dto.getContent(),
                    "lastMessageAt", Timestamp.now(),
                    "lastSenderUid", uid,
                    "unreadCount." + dto.getReceiverUid(),
                    FieldValue.increment(1)
            ));

            return null;
        });

        // 트랜잭션 성공 후 FCM 전송
        try {
            fcmService.sendFcmChat(
                    uid,
                    dto.getReceiverUid(),
                    dto.getContent(),
                    roomId
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    //메시지 조회
    public List<ChatResponseDTO> getMessages(String roomId) throws Exception {

        List<QueryDocumentSnapshot> docs =
                firestore.collection("chatRooms")
                        .document(roomId)
                        .collection("messages")
                        .orderBy("createdAt", Query.Direction.ASCENDING)
                        .get().get().getDocuments();

        return docs.stream().map(doc -> {

            String senderUid = doc.getString("senderUid");
            Map<String, String> profile = getUserProfile(senderUid);

            return new ChatResponseDTO(
                    doc.getId(),
                    senderUid,
                    profile.get("nickname"),
                    profile.get("profileImageUrl"),
                    doc.getString("content"),
                    (Timestamp) doc.get("createdAt")
            );
        }).toList();
    }

    //채팅방 목록
    public List<ChatroomResponseDTO> getMyChatRooms(String uid) throws Exception {

        return firestore.collection("chatRooms")
                .whereArrayContains("participants", uid)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .get().get().getDocuments()
                .stream()
                .map(doc -> {

                    List<String> participants =
                            (List<String>) doc.get("participants");

                    String otherUid = participants.stream()
                            .filter(p -> !p.equals(uid))
                            .findFirst().orElse("");

                    Map<String, String> profile =
                            getUserProfile(otherUid);

                    Map<String, Long> unread =
                            (Map<String, Long>) doc.get("unreadCount");

                    return new ChatroomResponseDTO(
                            doc.getId(),
                            otherUid,
                            profile.get("nickname"),
                            profile.get("profileImageUrl"),
                            doc.getString("lastMessage"),
                            (Timestamp) doc.get("lastMessageAt"),
                            unread.get(uid).intValue()
                    );
                })
                .toList();
    }

    // 채팅방 입장 (읽음 처리)
    public void enterRoom(String roomId, String uid) throws Exception{
        DocumentReference roomRef = firestore.collection("chatRooms")
                .document(roomId);

        roomRef.update("unreadCount." + uid, 0);

        QuerySnapshot snapshot = roomRef.collection("messages")
                .whereEqualTo("read", false)
                .get().get();

        for(QueryDocumentSnapshot doc : snapshot){
            doc.getReference().update("read", true);
        }
    }
}
