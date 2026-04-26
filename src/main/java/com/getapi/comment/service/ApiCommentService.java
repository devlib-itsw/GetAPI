package com.getapi.comment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.getapi.admin.domain.AdminCensoredResponse;
import com.getapi.ai.service.AiCensorAsyncService;
import com.getapi.api.domain.Api;
import com.getapi.comment.domain.ApiComment;
import com.getapi.comment.repository.ApiCommentRepository;
import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiCommentService {

    private final ApiCommentRepository apiCommentRepository;
    private final UserProfileRepository userProfileRepository;
    private final AiCensorAsyncService aiCensorAsyncService;

    public ApiComment create(String content, Users user, Api api) {
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("프로필이 없습니다."));
        ApiComment c = new ApiComment();
        c.setCommentUuid(UUID.randomUUID());
        c.setContent(content);
        c.setApi(api);
        c.setUserProfile(profile);
        c.setCensored(false);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        apiCommentRepository.save(c);
        aiCensorAsyncService.checkApiComment(c.getCommentId(), content);
        return c;
    }

    public List<ApiComment> findByApi(Api api) {
        return apiCommentRepository.findByApiAndIsCensoredFalseOrderByCreatedAtDesc(api);
    }

    public long countByApi(Api api) {
        return apiCommentRepository.countByApiAndIsCensoredFalse(api);
    }

    public ApiComment findByCommentUuid(UUID uuid) {
        return apiCommentRepository.findByCommentUuid(uuid);
    }

    @Transactional
    public void update(UUID uuid, String content, Users user) {
        ApiComment c = apiCommentRepository.findByCommentUuid(uuid);
        if (c == null) return;
        UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
        if (profile == null || !c.getUserProfile().getProfileId().equals(profile.getProfileId()))
            throw new SecurityException("수정 권한이 없습니다.");
        c.setContent(content);
        c.setUpdatedAt(LocalDateTime.now());
        apiCommentRepository.save(c);
        Long commentId = c.getCommentId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                aiCensorAsyncService.checkApiComment(commentId, content);
            }
        });
    }

    @Transactional
    public void deleteByUuid(UUID uuid, Users user) {
        ApiComment c = apiCommentRepository.findByCommentUuid(uuid);
        if (c == null) return;
        UserProfile profile = userProfileRepository.findByUser(user).orElse(null);
        if (profile == null || !c.getUserProfile().getProfileId().equals(profile.getProfileId()))
            throw new SecurityException("삭제 권한이 없습니다.");
        apiCommentRepository.delete(c);
    }

    public Page<AdminCensoredResponse> getApiCommentsByIsCensoredPage(int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("commentId").descending());
        return apiCommentRepository.findByIsCensoredTrue(pageable)
                .map(comment -> new AdminCensoredResponse(
                        comment.getCommentId(),
                        comment.getContent(),
                        comment.getApi().getName(),
                        comment.getCommentUuid().toString(),
                        comment.getUpdatedAt(),
                        comment.getUserProfile().getUser()
                ));
    }

    @Transactional
    public void ignore(UUID uuid) {
        ApiComment c = apiCommentRepository.findByCommentUuid(uuid);
        if (c != null) c.setCensored(false);
    }

    @Transactional
    public void delete(UUID uuid) {
        apiCommentRepository.deleteByCommentUuidAndIsCensoredTrue(uuid);
    }
}
