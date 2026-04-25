package com.getapi.comment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import com.getapi.comment.domain.PostComment;
import com.getapi.comment.repository.PostCommentRepository;
import com.getapi.post.domain.Post;
import com.getapi.post.repository.PostRepository;
import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostRepository postRepository;

	private final PostCommentRepository postCommentRepository;

	private final UserProfileRepository userProfileRepository;

	private final AiCensorAsyncService aiCensorAsyncService;

	public PostComment create(String content, Users user, Post post) {
		PostComment p = new PostComment();
		p.setCensored(false);
		p.setCreatedAt(LocalDateTime.now());
		p.setCommentId(null);
		p.setPost(post);
		p.setUpdatedAt(LocalDateTime.now());
		UserProfile profile = userProfileRepository.findByUser(user)
				.orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다."));
		p.setUserProfile(profile);
		p.setCommentUuid(java.util.UUID.randomUUID());
		p.setContent(content);
		this.postCommentRepository.save(p);
		aiCensorAsyncService.checkPostComment(p.getCommentId(), content);
		return p;
	}
	
	// public List<PostComment> getPostComments(Long postId) {
        // return postCommentRepository.getPostComments(postId);
    // }
	
	public List<PostComment> getPostCommentsByPost(Post post) {
        return postCommentRepository.findByPostAndIsCensoredFalse(post);
    }
	
	public PostComment findById(Long commentId) {
        return postCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. id=" + commentId));
    }
	
	public void modify(PostComment postComment, String content) {
		postComment.setContent(content);
		postComment.setUpdatedAt(LocalDateTime.now());
		this.postCommentRepository.save(postComment);
		aiCensorAsyncService.checkPostComment(postComment.getCommentId(), content);
	}

	@Transactional
	public void update(UUID commentUuid, String content, Users user) {
		PostComment comment = postCommentRepository.findByCommentUuid(commentUuid)
				.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
		if (!comment.getUserProfile().getUser().getUserId().equals(user.getUserId()))
			throw new SecurityException("수정 권한이 없습니다.");
		comment.setContent(content);
		comment.setUpdatedAt(LocalDateTime.now());
		postCommentRepository.save(comment);
		Long commentId = comment.getCommentId();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				aiCensorAsyncService.checkPostComment(commentId, content);
			}
		});
	}

	@Transactional
	public void deleteByOwner(UUID commentUuid, Users user) {
		PostComment comment = postCommentRepository.findByCommentUuid(commentUuid)
				.orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
		if (!comment.getUserProfile().getUser().getUserId().equals(user.getUserId()))
			throw new SecurityException("삭제 권한이 없습니다.");
		postCommentRepository.delete(comment);
	}

	public void delete(Long id) {
		this.postCommentRepository.deleteById(id);
	}
	
	public PostComment findByCommentUuid(UUID commentUuid) {
	    return postCommentRepository.findByCommentUuid(commentUuid)
	            .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. uuid=" + commentUuid));
	}
	
	public Page<AdminCensoredResponse> getPostCommentsByIsCensoredPage(int page){
		Pageable pageable=PageRequest.of(page, 10, Sort.by("commentId").descending());
		
		Page<PostComment> list=this.postCommentRepository.findByIsCensoredTrue(pageable);
		
		Page<AdminCensoredResponse> dtolist=list.map(comment->new AdminCensoredResponse(
			comment.getCommentId(),
			comment.getContent(),
			comment.getPost().getTitle(),
			comment.getCommentUuid().toString(),
			comment.getUpdatedAt(),
			comment.getUserProfile().getUser()
		));
		
		return dtolist;
	}
	
	@Transactional
	public void ignore(UUID uuid) {
		Optional<PostComment> optionalPostComment=this.postCommentRepository.findByCommentUuid(uuid);
		
		if(optionalPostComment.isPresent()) {
			PostComment postComment = optionalPostComment.get();
			postComment.setCensored(false);
		}
		
	}
	
	@Transactional
	public void delete(UUID uuid) {
		this.postCommentRepository.deleteByCommentUuidAndIsCensoredTrue(uuid);
	}
}
