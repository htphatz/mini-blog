package com.htphatz.notification_service.service;

import com.htphatz.notification_service.document.FCMToken;
import com.htphatz.notification_service.dto.request.FCMTokenReq;
import com.htphatz.notification_service.repository.FCMTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FCMTokenService {
    private final FCMTokenRepository fcmTokenRepository;

    public void createFCMToken(FCMTokenReq request) {
        Optional<FCMToken> existingToken = fcmTokenRepository.findByUserId(request.getUserId());
        if (existingToken.isPresent()) {
            existingToken.get().setFcmToken(request.getFcmToken());
            fcmTokenRepository.save(existingToken.get());
        } else {
            FCMToken fcmToken = FCMToken.builder()
                    .userId(request.getUserId())
                    .fcmToken(request.getFcmToken())
                    .build();
            fcmTokenRepository.save(fcmToken);
        }
    }
}
