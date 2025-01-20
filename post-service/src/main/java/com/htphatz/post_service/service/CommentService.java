package com.htphatz.post_service.service;

import com.htphatz.post_service.document.Comment;
import com.htphatz.post_service.document.Post;
import com.htphatz.post_service.dto.request.CommentRequest;
import com.htphatz.post_service.dto.response.CommentResponse;
import com.htphatz.post_service.dto.response.PageDto;
import com.htphatz.post_service.exception.AppException;
import com.htphatz.post_service.exception.ErrorCode;
import com.htphatz.post_service.mapper.CommentMapper;
import com.htphatz.post_service.repository.CommentRepository;
import com.htphatz.post_service.repository.PostRepository;
import com.htphatz.post_service.repository.httpclient.ProfileClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ProfileClient profileClient;
    private final CommentMapper commentMapper;

    public CommentResponse createComment(CommentRequest request) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        Comment comment = commentMapper.toComment(request);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        comment.setUserId(userId);
        var profileResponse = profileClient.getByUserId(userId);
        String displayName = profileResponse.getFirstName() + " " + profileResponse.getLastName();
        comment.setDisplayName(displayName);
        return commentMapper.toCommentResponse(commentRepository.save(comment));
    }

    public PageDto<CommentResponse> getAllComments(Integer pageNumber, Integer pageSize) {
        pageNumber--;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepository.findAll(pageable);
        return PageDto.of(comments.map(commentMapper::toCommentResponse));
    }

    public CommentResponse updateComment(String id, CommentRequest request) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        comment.setContent(request.getContent());
        comment.setPostId(post.getId());
        comment.setUpdatedAt(Instant.now());
        return commentMapper.toCommentResponse(commentRepository.save(comment));
    }

    public CommentResponse getById(String id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        return commentMapper.toCommentResponse(comment);
    }

    public PageDto<CommentResponse> getByPostId(String postId, Integer pageNumber, Integer pageSize) {
        pageNumber--;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments = commentRepository.findByPostIdPageable(postId, pageable);
        return PageDto.of(comments.map(commentMapper::toCommentResponse));
    }

    public void deleteComment(String id) {
        commentRepository.deleteById(id);
    }
}
