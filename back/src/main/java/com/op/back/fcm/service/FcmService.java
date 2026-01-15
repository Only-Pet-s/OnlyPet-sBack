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

    public void unregisterToken(String uid, FcmTokenRequestDTO req){
        firestore.collection("users")
                .document(uid)
                .update("fcmTokens", FieldValue.arrayRemove(req.getFcmToken()));
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

    // 결제 알림
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
                "[펫시터] " + petsitterName + "에게 " + price + "원이 결제되었습니다.",
                Map.of(
                        "type", FcmType.PAYMENT_COMPLETED.name(),
                        "paymentId", paymentId
                )
        );
    }

    // 환불 알림
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

    // 예약 관련 알림 필요: 생성, 도착, 완료, 취소, 수락, 거절

    // 예약 생성 시에는 예약한 유저에게만 결제 요청 알림
    public void sendReservationCreated(
            String buyerUid,
            String price,
            String reservationId
    ){
        send(
                buyerUid,
                "예약 생성 알림",
                price + "원 결제해주세요.",
                Map.of(
                        "type", FcmType.RESERVATION_CREATED.name(),
                        "reservationId", reservationId
                )
        );
    }

    // 사용자가 결제를 완료하고 펫시터에게 알림이 도착했을 때
    public void sendReservationReceived(
            String buyerUid,
            String petsitterUid,
            String reservationId
    ){
        String buyerName = getNickname(buyerUid, "사용자");
        String psName = getNickname(petsitterUid, "펫시터");
        send(
                buyerUid,
                "예약 요청 성공 알림",
                psName + "님에게 예약 요청이 성공하였습니다.",
                Map.of(
                        "type", FcmType.RESERVATION_RECEIVED.name(),
                        "reservationId", reservationId
                )
        );

        // 예약 도착
        send(
                petsitterUid,
                "예약 도착 알림",
                buyerName + "님으로 부터 예약 요청이 도착했습니다. 예약 수락 또는 거절을 해주세요.",
                Map.of(
                        "type", FcmType.RESERVATION_RECEIVED.name(),
                        "reservationId", reservationId
                )
        );
    }

    // 예약 수락 알림
    public void sendReservationAccepted(
            String buyerUid,
            String petsitterUid,
            String reservationId
    ){
        String petsitterName = getNickname(petsitterUid, "펫시터");

        send(
                buyerUid,
                "예약 확정 알림", "[펫시터] " + petsitterName + "님이 예약을 수락하셨습니다.",
                Map.of(
                        "type", FcmType.RESERVATION_ACCEPTED.name(),
                        "reservationId", reservationId
                )
        );
    }

    // 예약 거절 알림
    public void sendReservationRejected(
            String buyerUid,
            String petsitterUid,
            String reservationId
    ){
        String petsitterName = getNickname(petsitterUid, "펫시터");

        send(
                buyerUid,
                "예약 거절 알림", "[펫시터] " + petsitterName + "님이 예약을 거절하셨습니다.",
                Map.of(
                        "type", FcmType.RESERVATION_CANCELED.name(),
                        "reservationId", reservationId
                )
        );
    }

    // 예약 취소 알림
    public void sendReservationCancelled(
            String buyerUid,
            String petsitterUid,
            String reservationId
    ){
        String buyerName = getNickname(buyerUid, "사용자");
        String psName = getNickname(petsitterUid, "사용자");
        send(
                buyerUid,
                "예약 취소 알림",
                psName + "님에 대한 예약 취소가 완료되었습니다.",
                Map.of(
                        "type", FcmType.RESERVATION_CANCELED.name(),
                        "reservationId", reservationId
                )
        );

        // 예약 도착
        send(
                petsitterUid,
                "예약 취소 알림",
                buyerName + "님의 예약이 취소되었습니다.",
                Map.of(
                        "type", FcmType.RESERVATION_CANCELED.name(),
                        "reservationId", reservationId
                )
        );
    }

    // 예약 취소에 대한 환불 알림
    public void sendReservationCancelledRefund(
            String buyerUid,
            String petsitterUid,
            String refundAmount,
            String reservationId
    ){
        String buyerName = getNickname(buyerUid, "사용자");
        send(
                buyerUid,
                "예약 취소에 대한 환불 안내",
                refundAmount + "원이 환불되었습니다.",
                Map.of(
                        "type", FcmType.RESERVATION_CANCELED.name(),
                        "reservationId", reservationId
                )
        );

        // 예약 도착
        send(
                petsitterUid,
                buyerName + "님의 예약 취소에 대한 환수 안내",
                refundAmount + "원이 환수되었습니다.",
                Map.of(
                        "type", FcmType.RESERVATION_CANCELED.name(),
                        "reservationId", reservationId
                )
        );
    }

    // 강의 구매 알림
    public void sendPurchaseLecture(
            String buyerUid,
            String instructorUid,
            int price,
            String purchaseId
    ){
        String buyerName = getNickname(buyerUid, "사용자");
        send(
                buyerUid,
                "강의 구매 알림",
                price + "원이 결제되었습니다.",
                Map.of(
                        "type", FcmType.LECTURE_PURCHASED.name(),
                        "purchaseId", purchaseId
                )
        );

        // 예약 도착
        send(
                instructorUid,
                buyerName + "님이 강의를 구매하셨습니다.",
                price + "원이 입금되었습니다.",
                Map.of(
                        "type", FcmType.LECTURE_PURCHASED.name(),
                        "purchaseId", purchaseId
                )
        );
    }

    // fcm 푸시 알림 공통 메소드
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
