package com.getapi.comment.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.admin.domain.AdminCensoredResponse;
import com.getapi.comment.domain.ApiComment;
import com.getapi.comment.repository.ApiCommentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiCommentService {
	private final ApiCommentRepository apiCommentRepository;
	
	public Page<AdminCensoredResponse> getApiCommentsByIsCensoredPage(int page){
		Pageable pageable=PageRequest.of(page, 10, Sort.by("commentId").descending());
		
		Page<ApiComment> list=this.apiCommentRepository.findByIsCensoredTrue(pageable);
		
		Page<AdminCensoredResponse> dtolist=list.map(comment->new AdminCensoredResponse(
			comment.getCommentId(),
			comment.getContent(),
			comment.getApi().getName(),
			comment.getCommentUuid().toString(),
			comment.getUpdatedAt(),
			comment.getUser()
		));
		
		return dtolist;
	}
	
	@Transactional
	public void ignore(UUID uuid) {
		ApiComment apiComment=this.apiCommentRepository.findByCommentUuid(uuid);
		if(apiComment!=null) {
			apiComment.setCensored(false);
		}
	}
	
	@Transactional
	public void delete(UUID uuid) {
		this.apiCommentRepository.deleteByCommentUuidAndIsCensoredTrue(uuid);
	}
}
