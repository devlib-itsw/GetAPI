package com.getapi.comment.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.getapi.comment.domain.PostComment;
import com.getapi.comment.repository.PostCommentRepository;
import com.getapi.post.domain.Post;
import com.getapi.post.repository.PostRepository;
import com.getapi.user.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostRepository postRepository;

	private final PostCommentRepository postCommentRepository;
	
	public PostComment create(String content, Users user, Post post) {
		PostComment p = new PostComment();
		p.setCensored(false);
		p.setCreatedAt(LocalDateTime.now());
		p.setCommentId(null);
		p.setPost(post);
		p.setUpdatedAt(LocalDateTime.now());
		p.setUser(user);
		p.setCommentUuid(java.util.UUID.randomUUID());
		p.setContent(content);
		this.postCommentRepository.save(p);
		return p;
	}
	
	// public List<PostComment> getPostComments(Long postId) {
        // return postCommentRepository.getPostComments(postId);
    // }
	
	public List<PostComment> getPostCommentsByPost(Post post) {
        return postCommentRepository.findByPost(post);
    }
	
	public PostComment findById(Long commentId) {
        return postCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. id=" + commentId));
    }
	
	public void modify(PostComment postComment, String content) {
		postComment.setContent(content);
		postComment.setUpdatedAt(LocalDateTime.now());
		this.postCommentRepository.save(postComment);
	}
	
	public void delete(Long id) {
		this.postCommentRepository.deleteById(id);
	}
	
	public PostComment findByCommentUuid(UUID commentUuid) {
	    return postCommentRepository.findByCommentUuid(commentUuid)
	            .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. uuid=" + commentUuid));
	}
}
