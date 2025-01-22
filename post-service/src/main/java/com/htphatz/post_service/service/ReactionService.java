package com.htphatz.post_service.service;

import com.htphatz.post_service.document.Post;
import com.htphatz.post_service.document.Reaction;
import com.htphatz.post_service.dto.request.ReactionRequest;
import com.htphatz.post_service.dto.response.PageDto;
import com.htphatz.post_service.dto.response.ReactionResponse;
import com.htphatz.post_service.exception.AppException;
import com.htphatz.post_service.exception.ErrorCode;
import com.htphatz.post_service.mapper.ReactionMapper;
import com.htphatz.post_service.repository.PostRepository;
import com.htphatz.post_service.repository.ReactionRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final ProfileClient profileClient;
    private final ReactionMapper reactionMapper;

    public ReactionResponse createReaction(ReactionRequest request) {
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();

        Optional<Reaction> existingReaction = reactionRepository.findByPostIdAndUserId(request.getPostId(), userId);
        if (existingReaction.isPresent()) {
            Reaction reaction = existingReaction.get();
            if (reaction.getReactionType() == request.getReactionType()) {
                reactionMapper.toReactionResponse(reactionRepository.save(reaction));
                throw new AppException(ErrorCode.REACTION_CONFLICT);
            } else {
                reaction.setReactionType(request.getReactionType());
                return reactionMapper.toReactionResponse(reactionRepository.save(reaction));
            }
        } else {
            var profileResponse = profileClient.getByUserId(userId);
            String displayName = profileResponse.getFirstName() + " " + profileResponse.getLastName();

            // Cộng một tương tác
            post.setLikedCount(post.getLikedCount() + 1);
            postRepository.save(post);

            Reaction newReaction = reactionMapper.toReact(request);
            newReaction.setUserId(userId);
            newReaction.setCreatedAt(Instant.now());
            newReaction.setDisplayName(displayName);
            return reactionMapper.toReactionResponse(reactionRepository.save(newReaction));
        }
    }

    public PageDto<ReactionResponse> getAllReactions(Integer pageNumber, Integer pageSize) {
        pageNumber--;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Reaction> reactions = reactionRepository.findAll(pageable);
        return PageDto.of(reactions.map(reactionMapper::toReactionResponse));
    }

    public ReactionResponse getById(String id) {
        Reaction reaction = reactionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REACTION_NOT_FOUND));

        return reactionMapper.toReactionResponse(reaction);
    }

    public PageDto<ReactionResponse> getByPostId(String postId, Integer pageNumber, Integer pageSize) {
        pageNumber--;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Reaction> reactions = reactionRepository.findByPostIdPageable(postId, pageable);
        return PageDto.of(reactions.map(reactionMapper::toReactionResponse));
    }

    public void deleteReaction(String id) {
        reactionRepository.findById(id)
                .flatMap(value -> postRepository.findById(value.getPostId()))
                .ifPresent(post -> {
                    post.setLikedCount(post.getLikedCount() - 1);
                    postRepository.save(post);
                });
        reactionRepository.deleteById(id);
    }
}
