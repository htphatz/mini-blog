package com.htphatz.post_service.service;

import com.htphatz.post_service.document.Comment;
import com.htphatz.post_service.document.Post;
import com.htphatz.post_service.dto.request.CommentRequest;
import com.htphatz.post_service.dto.response.CommentResponse;
import com.htphatz.post_service.exception.AppException;
import com.htphatz.post_service.exception.ErrorCode;
import com.htphatz.post_service.mapper.CommentMapper;
import com.htphatz.post_service.repository.CommentRepository;
import com.htphatz.post_service.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentMapper commentMapper;

    public CommentResponse createComment(CommentRequest request) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        Comment comment = commentMapper.toComment(request);
        comment.setCreateAt(Instant.now());
        comment.setUpdatedAt(Instant.now());
        return commentMapper.toCommentResponse(commentRepository.save(comment));
    }

    public List<CommentResponse> getAllComments() {
        List<Comment> comments = commentRepository.findAll();
        return comments.stream().map(commentMapper::toCommentResponse).collect(Collectors.toList());
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

    public List<CommentResponse> getByPostId(String postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        return comments.stream().map(commentMapper::toCommentResponse).collect(Collectors.toList());
    }

    public void deleteComment(String id) {
        commentRepository.deleteById(id);
    }
}
