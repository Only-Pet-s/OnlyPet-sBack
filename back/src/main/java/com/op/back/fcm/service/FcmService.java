package com.op.back.fcm.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.messaging.FirebaseMessaging;
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
    public void sendFcmChat(String senderUid, String receiverUid, String content, String roomId) throws Exception{
        DocumentSnapshot sender = firestore.collection("users").document(senderUid).get().get();
        DocumentSnapshot receiver = firestore.collection("users").document(receiverUid).get().get();

        List<String> tokens = (List<String>)receiver.get("fcmTokens");

        if(tokens==null){return;}

        for(String token : tokens){
            Message msg = Message.builder().setToken(token).setNotification(
                    Notification.builder()
                            .setTitle(sender.getString("nickname"))
                            .setBody(content)
                            .build()
            ).putData("roomId", roomId).build();

            FirebaseMessaging.getInstance().send(msg);
        }
    }
}
