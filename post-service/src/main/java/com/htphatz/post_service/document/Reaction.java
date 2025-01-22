package com.htphatz.post_service.document;

import com.htphatz.post_service.enums.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "reactions")
public class Reaction {

    @MongoId
    private String id;

    @Field(name = "post_id")
    private String postId;

    @Field(name = "user_id")
    private String userId;

    @Field(name = "display_name")
    private String displayName;

    @Field(name = "reaction_type", targetType = FieldType.STRING)
    private ReactionType reactionType;

    @Field(name = "created_at")
    private Instant createdAt;
}
