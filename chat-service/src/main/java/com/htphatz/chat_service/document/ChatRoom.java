package com.htphatz.chat_service.document;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "chat_rooms")
public class ChatRoom {
    @MongoId
    private String id;

    @Field(name = "chat_id")
    private String chatId;

    @Field(name = "sender_id")
    private String senderId;

    @Field(name = "recipient_id")
    private String recipientId;
}
