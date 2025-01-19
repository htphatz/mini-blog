package com.htphatz.post_service.mapper;

import com.htphatz.post_service.document.Post;
import com.htphatz.post_service.dto.request.PostRequest;
import com.htphatz.post_service.dto.response.PostResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {
    Post toPost(PostRequest request);
    PostResponse toPostResponse(Post entity);
}
