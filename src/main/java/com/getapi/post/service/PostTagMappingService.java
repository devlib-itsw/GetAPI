package com.getapi.post.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.getapi.post.domain.Post;
import com.getapi.post.domain.PostTagMapping;
import com.getapi.post.repository.PostRepository;
import com.getapi.post.repository.PostTagMappingRepository;
import com.getapi.tag.repository.TagRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostTagMappingService {
	private final PostTagMappingRepository postTagMappingRepository;
	public List<PostTagMapping> getMappings(Post post){
	    return this.postTagMappingRepository.findByPost(post);
	}
}
