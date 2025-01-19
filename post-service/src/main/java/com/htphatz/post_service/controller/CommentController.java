package com.htphatz.post_service.controller;

import com.htphatz.post_service.dto.request.CommentRequest;
import com.htphatz.post_service.dto.response.APIResponse;
import com.htphatz.post_service.dto.response.CommentResponse;
import com.htphatz.post_service.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("create")
    public APIResponse<CommentResponse> createComment(@Valid @RequestBody CommentRequest request) {
        CommentResponse result = commentService.createComment(request);
        return APIResponse.<CommentResponse>builder().result(result).build();
    }

    @GetMapping
    public APIResponse<List<CommentResponse>> getAllComments() {
        List<CommentResponse> result = commentService.getAllComments();
        return APIResponse.<List<CommentResponse>>builder().result(result).build();
    }

    @GetMapping("{id}")
    public APIResponse<CommentResponse> getById(@Valid @PathVariable("id") String id) {
        CommentResponse result = commentService.getById(id);
        return APIResponse.<CommentResponse>builder().result(result).build();
    }

    @GetMapping("get-comment/{postId}")
    public APIResponse<List<CommentResponse>> getByPostId(@Valid @PathVariable("postId") String postId) {
        List<CommentResponse> result = commentService.getByPostId(postId);
        return APIResponse.<List<CommentResponse>>builder().result(result).build();
    }

    @DeleteMapping("{id}")
    public APIResponse<Void> deletePost(@Valid @PathVariable("id") String id) {
        commentService.deleteComment(id);
        return APIResponse.<Void>builder().build();
    }

    @PutMapping("{id}")
    public APIResponse<CommentResponse> updateComment(@Valid @PathVariable("id") String id, @Valid @RequestBody CommentRequest request) {
        CommentResponse result = commentService.updateComment(id, request);
        return APIResponse.<CommentResponse>builder().result(result).build();
    }
}
