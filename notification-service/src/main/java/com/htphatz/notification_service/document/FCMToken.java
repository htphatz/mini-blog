package com.htphatz.notification_service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "fcm_tokens")
public class FCMToken {
    @MongoId
    private String id;

    @Field(name = "user_id")
    private String userId;

    @Field(name = "fcm_token")
    private String fcmToken;
}
