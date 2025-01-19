package com.htphatz.post_service.controller;

import com.htphatz.post_service.dto.request.PostRequest;
import com.htphatz.post_service.dto.response.APIResponse;
import com.htphatz.post_service.dto.response.PageDto;
import com.htphatz.post_service.dto.response.PostResponse;
import com.htphatz.post_service.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping("create")
    public APIResponse<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
        PostResponse result = postService.createPost(request);
        return APIResponse.<PostResponse>builder().result(result).build();
    }

    @GetMapping
    public APIResponse<PageDto<PostResponse>> getAllPosts(
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        PageDto<PostResponse> result = postService.getAllPosts(pageNumber, pageSize);
        return APIResponse.<PageDto<PostResponse>>builder().result(result).build();
    }

    @GetMapping("{id}")
    public APIResponse<PostResponse> getById(@Valid @PathVariable("id") String id) {
        PostResponse result = postService.getById(id);
        return APIResponse.<PostResponse>builder().result(result).build();
    }

    @DeleteMapping("{id}")
    public APIResponse<Void> deletePost(@Valid @PathVariable("id") String id) {
        postService.deletePost(id);
        return APIResponse.<Void>builder().build();
    }

    @PutMapping("{id}")
    public APIResponse<PostResponse> updatePost(@Valid @PathVariable("id") String id, @Valid @RequestBody PostRequest request) {
        PostResponse result = postService.updatePost(id, request);
        return APIResponse.<PostResponse>builder().result(result).build();
    }
}
