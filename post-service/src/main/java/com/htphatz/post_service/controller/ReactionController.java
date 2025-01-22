package com.htphatz.post_service.controller;

import com.htphatz.post_service.dto.request.ReactionRequest;
import com.htphatz.post_service.dto.response.APIResponse;
import com.htphatz.post_service.dto.response.PageDto;
import com.htphatz.post_service.dto.response.ReactionResponse;
import com.htphatz.post_service.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("reactions")
@RequiredArgsConstructor
public class ReactionController {
    private final ReactionService reactionService;

    @PostMapping("create")
    public APIResponse<ReactionResponse> createReaction(@Valid @RequestBody ReactionRequest request) {
        ReactionResponse result = reactionService.createReaction(request);
        return APIResponse.<ReactionResponse>builder().result(result).build();
    }

    @GetMapping
    public APIResponse<PageDto<ReactionResponse>> getAllReaction(
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize
    ) {
        PageDto<ReactionResponse> result = reactionService.getAllReactions(pageNumber, pageSize);
        return APIResponse.<PageDto<ReactionResponse>>builder().result(result).build();
    }

    @GetMapping("{id}")
    public APIResponse<ReactionResponse> getById(@Valid @PathVariable("id") String id) {
        ReactionResponse result = reactionService.getById(id);
        return APIResponse.<ReactionResponse>builder().result(result).build();
    }

    @GetMapping("get-reaction/{postId}")
    public APIResponse<PageDto<ReactionResponse>> getByPostId(
            @Valid @PathVariable("postId") String postId,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        PageDto<ReactionResponse> result = reactionService.getByPostId(postId, pageNumber, pageSize);
        return APIResponse.<PageDto<ReactionResponse>>builder().result(result).build();
    }

    @DeleteMapping("{id}")
    public APIResponse<Void> deleteReaction(@Valid @PathVariable("id") String id) {
        reactionService.deleteReaction(id);
        return APIResponse.<Void>builder().build();
    }
}
