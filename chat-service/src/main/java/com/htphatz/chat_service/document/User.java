package com.htphatz.chat_service.document;

import com.htphatz.chat_service.enums.Status;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @MongoId
    private String id;

    @Field(name = "user_id")
    private String userId;

    @Field(name = "username")
    private String username;

    @Field(name = "status", targetType = FieldType.STRING)
    private Status status;
}
