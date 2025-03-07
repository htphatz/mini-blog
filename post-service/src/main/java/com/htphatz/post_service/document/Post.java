package com.htphatz.post_service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "posts")
public class Post {

    @MongoId
    private String id;

    @Field(name = "user_id")
    private String userId;

    @Field(name = "display_name")
    private String displayName;

    @Field(name = "description")
    private String description;

    @Field(name = "created_at")
    private Instant createdAt;

    @Field(name = "updated_at")
    private Instant updatedAt;

    @Field(name = "liked_count")
    private Integer likedCount;

    @ReadOnlyProperty
    @DocumentReference
    private List<Comment> comments;

    @ReadOnlyProperty
    @DocumentReference
    private List<Reaction> reactions;
}
