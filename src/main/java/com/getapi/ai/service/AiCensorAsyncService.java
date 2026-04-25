package com.getapi.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.api.repository.ApiRepository;
import com.getapi.comment.repository.ApiCommentRepository;
import com.getapi.comment.repository.PostCommentRepository;
import com.getapi.post.repository.PostRepository;
import com.getapi.tag.repository.TagRepository;
import com.getapi.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiCensorAsyncService {

    private static final Logger log = LoggerFactory.getLogger(AiCensorAsyncService.class);

    private final AiFilterService aiFilterService;
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final ApiCommentRepository apiCommentRepository;
    private final TagRepository tagRepository;
    private final ApiRepository apiRepository;
    private final UserProfileRepository userProfileRepository;

    @Async
    @Transactional
    public void checkPost(Long postId, String text) {
        boolean censored = aiFilterService.check(text).isCensored();
        postRepository.findById(postId).ifPresent(post -> {
            post.setCensored(censored);
            log.info("[AI] Post {} → isCensored={}", postId, censored);
        });
    }

    @Async
    @Transactional
    public void checkPostComment(Long commentId, String text) {
        boolean censored = aiFilterService.check(text).isCensored();
        postCommentRepository.findById(commentId).ifPresent(comment -> {
            comment.setCensored(censored);
            log.info("[AI] PostComment {} → isCensored={}", commentId, censored);
        });
    }

    @Async
    @Transactional
    public void checkApiComment(Long commentId, String text) {
        boolean censored = aiFilterService.check(text).isCensored();
        apiCommentRepository.findById(commentId).ifPresent(comment -> {
            comment.setCensored(censored);
            log.info("[AI] ApiComment {} → isCensored={}", commentId, censored);
        });
    }

    @Async
    @Transactional
    public void checkTag(Long tagId, String text) {
        boolean censored = aiFilterService.check(text).isCensored();
        tagRepository.findById(tagId).ifPresent(tag -> {
            tag.setCensored(censored);
            log.info("[AI] Tag {} → isCensored={}", tagId, censored);
        });
    }

    @Async
    @Transactional
    public void checkApi(Long apiId, String text) {
        boolean censored = aiFilterService.check(text).isCensored();
        apiRepository.findById(apiId).ifPresent(api -> {
            api.setCensored(censored);
            log.info("[AI] Api {} → isCensored={}", apiId, censored);
        });
    }

    @Async
    @Transactional
    public void checkUserProfile(Long profileId, String text) {
        boolean censored = aiFilterService.check(text).isCensored();
        userProfileRepository.findById(profileId).ifPresent(profile -> {
            profile.setCensored(censored);
            if (censored) {
                profile.setNickname(null);
                profile.setIntroduction(null);
                profile.setWebUrl(null);
            }
            log.info("[AI] UserProfile {} → isCensored={}", profileId, censored);
        });
    }
}
