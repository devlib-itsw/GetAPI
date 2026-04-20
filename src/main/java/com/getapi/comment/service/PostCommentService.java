package com.getapi.comment.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.admin.domain.AdminCensoredResponse;
import com.getapi.comment.domain.PostComment;
import com.getapi.comment.repository.PostCommentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostCommentService {
	private final PostCommentRepository postCommentRepository;
	
	public Page<AdminCensoredResponse> getPostCommentsByIsCensoredPage(int page){
		Pageable pageable=PageRequest.of(page, 10, Sort.by("commentId").descending());
		
		Page<PostComment> list=this.postCommentRepository.findByIsCensoredTrue(pageable);
		
		Page<AdminCensoredResponse> dtolist=list.map(comment->new AdminCensoredResponse(
			comment.getCommentId(),
			comment.getContent(),
			comment.getPost().getTitle(),
			comment.getCommentUuid().toString(),
			comment.getUpdatedAt(),
			comment.getUser()
		));
		
		return dtolist;
	}
	
	@Transactional
	public void ignore(UUID uuid) {
		PostComment postComment=this.postCommentRepository.findByCommentUuid(uuid);
		if(postComment!=null) {
			postComment.setCensored(false);
		}
	}
	
	@Transactional
	public void delete(UUID uuid) {
		this.postCommentRepository.deleteByCommentUuidAndIsCensoredTrue(uuid);
	}
}
