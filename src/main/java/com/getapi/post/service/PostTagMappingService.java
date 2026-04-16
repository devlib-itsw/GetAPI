package com.getapi.post.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.getapi.post.domain.Post;
import com.getapi.post.domain.PostTagMapping;
import com.getapi.post.repository.PostTagMappingRepository;
import com.getapi.tag.domain.Tag;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostTagMappingService {
	private final PostTagMappingRepository postTagMappingRepository;
	public List<PostTagMapping> getMappings(Post post){
	    return this.postTagMappingRepository.findByPost(post);
	}
	
	public List<PostTagMapping> getAllMappings() {
		return this.postTagMappingRepository.findAll();
	}
	
	public Map<Long, List<Tag>> getTagMap(List<Post> posts) {

	    List<Long> postIds = posts.stream()
	            .map(Post::getPostId)
	            .toList();

	    List<PostTagMapping> mappings = postTagMappingRepository.findByPostIds(postIds);

	    Map<Long, List<Tag>> result = new HashMap<>();

	    for (PostTagMapping mapping : mappings) {
	        Long postId = mapping.getPost().getPostId();

	        result.computeIfAbsent(postId, k -> new ArrayList<>())
	              .add(mapping.getTag());
	    }

	    return result;
	}
}
