package com.htphatz.post_service.mapper;

import com.htphatz.post_service.document.Comment;
import com.htphatz.post_service.dto.request.CommentRequest;
import com.htphatz.post_service.dto.response.CommentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    Comment toComment(CommentRequest request);
    CommentResponse toCommentResponse(Comment entity);
}
