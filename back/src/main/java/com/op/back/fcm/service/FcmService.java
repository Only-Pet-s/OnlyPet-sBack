package com.op.back.fcm.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.op.back.fcm.dto.FcmTokenRequestDTO;
import com.op.back.fcm.type.FcmType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
    public void sendChat(
            String senderUid,
            String receiverUid,
            String content,
            String roomId
    ) {
        String title = getNickname(senderUid, "새 메시지");

        send(
                receiverUid,
                title,
                content,
                Map.of(
                        "type", FcmType.CHAT.name(),
                        "roomId", roomId
                )
        );
    }

    public void sendPaymentCompleted(
            String buyerUid,
            String petsitterUid,
            String price,
            String paymentId
    ) {
        String buyerName = getNickname(buyerUid, "사용자");
        String petsitterName = getNickname(petsitterUid, "펫시터");

        // 펫시터
        send(
                petsitterUid,
                "결제 알림",
                buyerName + "님이 " + price + "원을 결제했습니다.",
                Map.of(
                        "type", FcmType.PAYMENT_RECEIVED.name(),
                        "paymentId", paymentId
                )
        );

        // 결제자
        send(
                buyerUid,
                "결제 완료",
                petsitterName + "에게 " + price + "원이 결제되었습니다.",
                Map.of(
                        "type", FcmType.PAYMENT_COMPLETED.name(),
                        "paymentId", paymentId
                )
        );
    }

    public void sendRefund(
            String buyerUid,
            String petsitterUid,
            String price,
            String reservationId
    ) {
        send(
                buyerUid,
                "환불 완료",
                price + "원이 환불되었습니다.",
                Map.of(
                        "type", FcmType.PAYMENT_REFUNDED.name(),
                        "reservationId", reservationId
                )
        );

        send(
                petsitterUid,
                "예약 환불 알림",
                price + "원 결제가 환불 처리되었습니다.",
                Map.of(
                        "type", FcmType.PAYMENT_REFUNDED_NOTICE.name(),
                        "reservationId", reservationId
                )
        );
    }

    private void send(
            String targetUid,
            String title,
            String body,
            Map<String, String> data
    ) {
        if (targetUid == null || targetUid.isBlank()) return;

        try {
            DocumentSnapshot user =
                    firestore.collection("users")
                            .document(targetUid)
                            .get().get();

            @SuppressWarnings("unchecked")
            List<String> tokens =
                    (List<String>) user.get("fcmTokens");

            if (tokens == null || tokens.isEmpty()) return;

            for (String token : tokens) {
                try {
                    Message.Builder builder = Message.builder()
                            .setToken(token)
                            .setNotification(
                                    Notification.builder()
                                            .setTitle(title)
                                            .setBody(body)
                                            .build()
                            );

                    if (data != null) {
                        data.forEach(builder::putData);
                    }

                    FirebaseMessaging.getInstance().send(builder.build());

                } catch (FirebaseMessagingException e) {
                    if ("UNREGISTERED".equals(
                            e.getMessagingErrorCode().name())) {

                        firestore.collection("users")
                                .document(targetUid)
                                .update(
                                        "fcmTokens",
                                        FieldValue.arrayRemove(token)
                                );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getNickname(String uid, String defaultName) {
        try {
            DocumentSnapshot user =
                    firestore.collection("users")
                            .document(uid)
                            .get().get();

            return user.getString("nickname") != null
                    ? user.getString("nickname")
                    : defaultName;
        } catch (Exception e) {
            return defaultName;
        }
    }
}
