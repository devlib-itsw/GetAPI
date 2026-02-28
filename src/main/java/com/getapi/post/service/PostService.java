package com.getapi.post.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.aspectj.weaver.patterns.TypePatternQuestions.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.getapi.post.domain.Post;
import com.getapi.post.domain.PostTagMapping;
import com.getapi.post.repository.PostRepository;
import com.getapi.post.repository.PostTagMappingRepository;
import com.getapi.tag.domain.Tag;
import com.getapi.tag.repository.TagRepository;
import com.getapi.user.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
	private final PostRepository postRepository;
	private final TagRepository tagRepository;
	private final PostTagMappingRepository postTagMappingRepository;
	
	public Page<Post> getList(int page) {
		List<Sort.Order> sorts = new ArrayList<>();
		sorts.add(Sort.Order.desc("createdAt"));
		Pageable pageable = PageRequest.of(page, 5, Sort.by(sorts));
		System.out.println(sorts);
		/* return this.questionRepository.findAll(spec, pageable); */
		return this.postRepository.findAll(pageable);
	}
	
	public void create(String title, String content, List<String> tags, Users user) {
		Post p = new Post();
		p.setCensored(false);
		p.setCreatedAt(LocalDateTime.now());
		p.setPostId(null);
		p.setUpdatedAt(LocalDateTime.now());
		p.setUser(user);
		p.setViewCount(0L);
		p.setPostUuid(java.util.UUID.randomUUID());
		p.setContent(content);
		p.setTitle(title);
		this.postRepository.save(p);
		
		// 2. 태그 저장 로직 (예시)
	    if (tags != null) {
	        for (String tagName : tags) {
	            Tag tag = new Tag();
	            tag.setCensored(false);
	            tag.setTagId(null);
	            tag.setTagUuid(java.util.UUID.randomUUID());
	            tag.setTag(tagName);
	            this.tagRepository.save(tag);
	            
	            PostTagMapping mapping = new PostTagMapping();
	            mapping.setPost(p);
	            mapping.setTag(tag);

	            this.postTagMappingRepository.save(mapping);
	        }
	    }
		
		
	}
}