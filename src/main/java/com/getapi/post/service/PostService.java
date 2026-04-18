package com.getapi.post.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.errors.DataNotFoundException;
import com.getapi.post.domain.Post;
import com.getapi.post.domain.PostTagMapping;
import com.getapi.post.repository.PostRepository;
import com.getapi.post.repository.PostTagMappingRepository;
import com.getapi.tag.domain.Tag;
import com.getapi.tag.repository.TagRepository;
import com.getapi.user.domain.Users;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
	private final PostRepository postRepository;
	private final TagRepository tagRepository;
	private final PostTagMappingRepository postTagMappingRepository;
	
	public Page<Post> getList(int page, String kw, List<String> filters, String sort) {
	    
	    // 1. 좋아요순 정렬인 경우 (네이티브 쿼리 사용)
	    if ("likes".equals(sort)) {
	        Pageable pageable = PageRequest.of(page, 5); 
	        return postRepository.findAllOrderByLikes(kw, pageable);
	    }

	    // 2. 최신순/조회순인 경우 (Specification 사용)
	    List<Sort.Order> sorts = new ArrayList<>();
	    if ("views".equals(sort)) {
	        sorts.add(Sort.Order.desc("viewCount"));
	    } else {
	        sorts.add(Sort.Order.desc("createdAt"));
	    }
	    
	    Pageable pageable = PageRequest.of(page, 5, Sort.by(sorts));

	    // 🔥 Specification 정의 시작
	    Specification<Post> spec = (p, query, cb) -> {
	        query.distinct(true);

	        // 람다 안에서 리스트를 새로 만들어야 에러가 나지 않습니다.
	        List<Predicate> orConditions = new ArrayList<>();

	        // 기본 join
	        Join<Post, Users> u = p.join("user", JoinType.LEFT);

	        boolean searchTitle = filters.contains("title");
	        boolean searchContent = filters.contains("content");
	        boolean searchUser = filters.contains("user");
	        boolean searchTag = filters.contains("hashtag");
	        boolean isAll = filters.contains("all");

	        if (isAll || searchTitle) {
	            orConditions.add(cb.like(p.get("title"), "%" + kw + "%"));
	        }

	        if (isAll || searchContent) {
	            orConditions.add(cb.like(p.get("content"), "%" + kw + "%"));
	        }

	        if (isAll || searchUser) {
	            orConditions.add(cb.like(u.get("name"), "%" + kw + "%"));
	        }

	        if (isAll || searchTag) {
	            Root<PostTagMapping> ptm = query.from(PostTagMapping.class);
	            Join<PostTagMapping, Tag> tag = ptm.join("tag", JoinType.LEFT);

	            Predicate tagPredicate = cb.and(
	                cb.equal(ptm.get("post"), p),
	                cb.like(tag.get("tag"), "%" + kw + "%")
	            );

	            orConditions.add(tagPredicate);
	        }

	        // 최종적으로 생성된 orConditions를 반환
	        return cb.or(orConditions.toArray(new Predicate[0]));
	    };

	    return postRepository.findAll(spec, pageable);
	}
	
	@Transactional
	public Post getPost(UUID postUuid) {

	    // 1. 조회수 증가 (UUID 기준)
	    this.postRepository.increaseViewCountByUuid(postUuid);

	    // 2. 게시글 조회
	    return this.postRepository.findByPostUuid(postUuid)
	            .orElseThrow(() -> new DataNotFoundException("post not found"));
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
	
	public Post findById(Long postId) {
	    return this.postRepository.findById(postId)
	            .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));
	}
	
	public Post findByUuid(UUID postUuid) {
	    return this.postRepository.findByPostUuid(postUuid)
	            .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. uuid=" + postUuid));
	}
	
	private Specification<Post> buildSpecification(String kw, List<String> filters) {
	    return (p, query, cb) -> {

	        query.distinct(true);
	        List<Predicate> predicates = new ArrayList<>();

	        Join<Post, Users> userJoin = null;

	        if (filters == null || filters.isEmpty() || filters.contains("all")) {
	            userJoin = p.join("user", JoinType.LEFT);
	            Join<Post, PostTagMapping> tagMappingJoin = p.join("postTagMappings", JoinType.LEFT);
	            Join<PostTagMapping, Tag> tagJoin = tagMappingJoin.join("tag", JoinType.LEFT);

	            return cb.or(
	                cb.like(p.get("title"), "%" + kw + "%"),
	                cb.like(p.get("content"), "%" + kw + "%"),
	                cb.like(userJoin.get("name"), "%" + kw + "%"),
	                cb.like(tagJoin.get("tag"), "%" + kw + "%")
	            );
	        }

	        for (String filter : filters) {
	            switch (filter) {

	                case "title":
	                    predicates.add(cb.like(p.get("title"), "%" + kw + "%"));
	                    break;

	                case "content":
	                    predicates.add(cb.like(p.get("content"), "%" + kw + "%"));
	                    break;

	                case "user":
	                    if (userJoin == null) {
	                        userJoin = p.join("user", JoinType.LEFT);
	                    }
	                    predicates.add(cb.like(userJoin.get("name"), "%" + kw + "%"));
	                    break;

	                case "hashtag":
	                    Join<Post, PostTagMapping> tagMappingJoin = p.join("postTagMappings", JoinType.LEFT);
	                    Join<PostTagMapping, Tag> tagJoin = tagMappingJoin.join("tag", JoinType.LEFT);
	                    predicates.add(cb.like(tagJoin.get("tag"), "%" + kw + "%"));
	                    break;
	            }
	        }

	        return cb.or(predicates.toArray(new Predicate[0]));
	    };
	}
}