package com.htphatz.post_service.mapper;

import com.htphatz.post_service.document.Reaction;
import com.htphatz.post_service.dto.request.ReactionRequest;
import com.htphatz.post_service.dto.response.ReactionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReactionMapper {
    Reaction toReact(ReactionRequest request);
    ReactionResponse toReactionResponse(Reaction entity);
}
