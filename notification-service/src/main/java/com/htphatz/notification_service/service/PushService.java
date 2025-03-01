package com.htphatz.notification_service.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.htphatz.notification_service.dto.request.PushMessageReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PushService {
    private final FirebaseMessaging firebaseMessaging;

    public String pushNotification(PushMessageReq request) {
        Notification notification = Notification.builder()
                .setTitle(request.getTitle())
                .setImage(request.getImage())
                .setBody(request.getBody())
                .build();

        Message message = Message.builder()
                .setToken(request.getRecipientToken())
                .setNotification(notification)
                .putAllData(request.getData())
                .build();

        try {
            firebaseMessaging.send(message);
            return "Push notification successfully";
        } catch (FirebaseMessagingException ex) {
            return "Push notification fail";
        }
    }
}
