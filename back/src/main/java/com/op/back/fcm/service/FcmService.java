package com.op.back.fcm.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.op.back.fcm.dto.FcmTokenRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final Firestore firestore;

    public void registerToken(String uid, FcmTokenRequestDTO req){
        firestore.collection("users")
                .document(uid)
                .update("fcmTokens", FieldValue.arrayUnion(req.getFcmToken()));
    }

    // 채팅용 푸시 알림 전송 메서드
    public void sendFcmChat(
            String senderUid,
            String receiverUid,
            String content,
            String roomId
    ) {

        try {
            DocumentSnapshot sender =
                    firestore.collection("users")
                            .document(senderUid)
                            .get().get();

            DocumentSnapshot receiver =
                    firestore.collection("users")
                            .document(receiverUid)
                            .get().get();

            if (!receiver.exists()) return;

            @SuppressWarnings("unchecked")
            List<String> tokens =
                    (List<String>) receiver.get("fcmTokens");

            if (tokens == null || tokens.isEmpty()) return;

            String title = sender.getString("nickname");
            if (title == null) title = "새 메시지";

            for (String token : tokens) {
                try {
                    Message msg = Message.builder()
                            .setToken(token)
                            .setNotification(
                                    Notification.builder()
                                            .setTitle(title)
                                            .setBody(content)
                                            .build()
                            )
                            .putData("roomId", roomId)
                            .putData("icon", "noti_icon")
                            .build();

                    FirebaseMessaging.getInstance().send(msg);

                } catch (FirebaseMessagingException e) {

                    // 핵심 처리
                    if ("UNREGISTERED".equals(
                            e.getMessagingErrorCode().name())) {

                        // 유효하지 않은 토큰 즉시 제거
                        firestore.collection("users")
                                .document(receiverUid)
                                .update(
                                        "fcmTokens",
                                        FieldValue.arrayRemove(token)
                                );

                        //System.out.println("유효하지 않은 토큰 제거: " + token);
                    } else {
                        //System.err.println("FCM 실패: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            // 절대 throw 하지 말고 로그만
            e.printStackTrace();
        }
    }

}
