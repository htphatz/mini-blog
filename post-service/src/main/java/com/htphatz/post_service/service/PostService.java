package com.htphatz.post_service.service;

import com.htphatz.post_service.document.Post;
import com.htphatz.post_service.dto.request.PostRequest;
import com.htphatz.post_service.dto.response.PageDto;
import com.htphatz.post_service.dto.response.PostResponse;
import com.htphatz.post_service.exception.AppException;
import com.htphatz.post_service.exception.ErrorCode;
import com.htphatz.post_service.mapper.PostMapper;
import com.htphatz.post_service.repository.CommentRepository;
import com.htphatz.post_service.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostMapper postMapper;

    public PostResponse createPost(PostRequest request) {
        Post post = postMapper.toPost(request);
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        log.info(post.toString());
        return postMapper.toPostResponse(postRepository.save(post));
    }

    public PageDto<PostResponse> getAllPosts(Integer pageNumber, Integer pageSize) {
        pageNumber--;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> posts = postRepository.findAll(pageable);
        return PageDto.of(posts.map(postMapper::toPostResponse));
    }

    public PostResponse updatePost(String id, PostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        post.setDescription(request.getDescription());
        post.setUpdatedAt(Instant.now());

        return postMapper.toPostResponse(postRepository.save(post));
    }

    public PostResponse getById(String id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        return postMapper.toPostResponse(post);
    }

    public void deletePost(String id) {
        postRepository.deleteById(id);
        commentRepository.findByPostId(id).forEach(comment -> commentRepository.deleteById(comment.getId()));
    }
}
