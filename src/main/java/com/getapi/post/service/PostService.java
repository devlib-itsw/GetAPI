package com.getapi.post.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.admin.domain.AdminCensoredResponse;
import com.getapi.post.domain.Post;
import com.getapi.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
	private final PostRepository postRepository;
	
	public Page<AdminCensoredResponse> getPostsByIsCensoredPage(int page){
		Pageable pageable=PageRequest.of(page, 10, Sort.by("postId").descending());
		
		Page<Post> list=this.postRepository.findByIsCensoredTrue(pageable);
		
		Page<AdminCensoredResponse> dtolist=list.map(post->new AdminCensoredResponse(
				post.getPostId(),
				post.getTitle(),
				post.getContent(),
				post.getPostUuid().toString(),
				post.getUpdatedAt(),
				post.getUser()
		));
		
		return dtolist;
	}
	
	@Transactional
	public void ignore(UUID uuid) {
		Post post=this.postRepository.findByPostUuid(uuid);
		if(post!=null) {
			post.setCensored(false);
		}
	}
	
	@Transactional
	public void delete(UUID uuid) {
		this.postRepository.deleteByPostUuidAndIsCensoredTrue(uuid);
	}
}
