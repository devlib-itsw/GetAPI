package com.getapi.post.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.getapi.admin.domain.AdminCensoredResponse;
import com.getapi.ai.service.AiCensorAsyncService;
import com.getapi.errors.DataNotFoundException;
import com.getapi.post.domain.Post;
import com.getapi.post.domain.PostTagMapping;
import com.getapi.comment.repository.PostCommentRepository;
import com.getapi.post.repository.LikeRepository;
import com.getapi.post.repository.PostRepository;
import com.getapi.post.repository.PostTagMappingRepository;
import com.getapi.tag.domain.Tag;
import com.getapi.tag.repository.TagRepository;
import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;

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
	private final LikeRepository likeRepository;
	private final PostCommentRepository postCommentRepository;
	private final UserProfileRepository userProfileRepository;
	private final AiCensorAsyncService aiCensorAsyncService;

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

	    Specification<Post> spec = (p, query, cb) -> {
	        query.distinct(true);

	        List<Predicate> orConditions = new ArrayList<>();

	        Join<Post, UserProfile> up = p.join("userProfile", JoinType.LEFT);

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
	            orConditions.add(cb.like(up.get("name"), "%" + kw + "%"));
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

	        Predicate notCensored = cb.isFalse(p.get("isCensored"));
	        if (orConditions.isEmpty()) return notCensored;
	        return cb.and(notCensored, cb.or(orConditions.toArray(new Predicate[0])));
	    };

	    return this.postRepository.findAll(spec, pageable);
	}

	@Transactional
	public Post getPost(UUID postUuid) {

	    // 1. 조회수 증가 (UUID 기준)
	    this.postRepository.increaseViewCountByUuid(postUuid);

	    // 2. 게시글 조회
	    Post post = this.postRepository.findByPostUuid(postUuid)
	            .orElseThrow(() -> new DataNotFoundException("post not found"));
	    if (post.isCensored()) throw new DataNotFoundException("post not found");
	    return post;
	}

	public void create(String title, String content, List<String> tags, Users user) {
		Post p = new Post();
		p.setCensored(false);
		p.setCreatedAt(LocalDateTime.now());
		p.setPostId(null);
		p.setUpdatedAt(LocalDateTime.now());
		UserProfile profile = userProfileRepository.findByUser(user)
				.orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다."));
		p.setUserProfile(profile);
		p.setViewCount(0L);
		p.setPostUuid(java.util.UUID.randomUUID());
		p.setContent(content);
		p.setTitle(title);
		this.postRepository.save(p);
		aiCensorAsyncService.checkPost(p.getPostId(), title + " " + content);

		if (tags != null) {
		    for (String tagName : tags) {
		        Tag tag = new Tag();
		        tag.setCensored(false);
		        tag.setTagId(null);
		        tag.setTagUuid(java.util.UUID.randomUUID());
		        tag.setTag(tagName);
		        this.tagRepository.save(tag);
		        aiCensorAsyncService.checkTag(tag.getTagId(), tagName);

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
	    Post post = this.postRepository.findByPostUuid(postUuid)
	            .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. uuid=" + postUuid));
	    if (post.isCensored()) throw new DataNotFoundException("post not found");
	    return post;
	}

	public Post findByUuidForOwner(UUID postUuid) {
	    return this.postRepository.findByPostUuid(postUuid)
	            .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. uuid=" + postUuid));
	}

	private Specification<Post> buildSpecification(String kw, List<String> filters) {
	    return (p, query, cb) -> {

	        query.distinct(true);
	        List<Predicate> predicates = new ArrayList<>();

	        Join<Post, UserProfile> userJoin = null;

	        if (filters == null || filters.isEmpty() || filters.contains("all")) {
	            userJoin = p.join("userProfile", JoinType.LEFT);
	            Root<PostTagMapping> tagMappingRoot = query.from(PostTagMapping.class);
	            Join<PostTagMapping, Tag> tagJoin = tagMappingRoot.join("tag", JoinType.LEFT);

	            return cb.or(
	                cb.like(p.get("title"), "%" + kw + "%"),
	                cb.like(p.get("content"), "%" + kw + "%"),
	                cb.like(userJoin.get("name"), "%" + kw + "%"),
	                cb.and(cb.equal(tagMappingRoot.get("post"), p), cb.like(tagJoin.get("tag"), "%" + kw + "%"))
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
	                        userJoin = p.join("userProfile", JoinType.LEFT);
	                    }
	                    predicates.add(cb.like(userJoin.get("name"), "%" + kw + "%"));
	                    break;

	                case "hashtag":
	                    Root<PostTagMapping> tagMappingRoot = query.from(PostTagMapping.class);
	                    Join<PostTagMapping, Tag> tagJoin = tagMappingRoot.join("tag", JoinType.LEFT);
	                    predicates.add(cb.and(
	                        cb.equal(tagMappingRoot.get("post"), p),
	                        cb.like(tagJoin.get("tag"), "%" + kw + "%")
	                    ));
	                    break;
	            }
	        }

	        return cb.or(predicates.toArray(new Predicate[0]));
	    };
	}

	public Page<AdminCensoredResponse> getPostsByIsCensoredPage(int page){
		Pageable pageable=PageRequest.of(page, 10, Sort.by("postId").descending());

		Page<Post> list=this.postRepository.findByIsCensoredTrue(pageable);

		Page<AdminCensoredResponse> dtolist=list.map(post->new AdminCensoredResponse(
				post.getPostId(),
				post.getTitle(),
				post.getContent(),
				post.getPostUuid().toString(),
				post.getUpdatedAt(),
				post.getUserProfile().getUser()
		));

		return dtolist;
	}

	@Transactional
	public void update(Post post, String title, String content, Users user) {
		if (!post.getUserProfile().getUser().getUserId().equals(user.getUserId()))
			throw new SecurityException("수정 권한이 없습니다.");
		post.setTitle(title);
		post.setContent(content);
		post.setUpdatedAt(LocalDateTime.now());
		postRepository.save(post);
		Long postId = post.getPostId();
		String checkText = title + " " + content;
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				aiCensorAsyncService.checkPost(postId, checkText);
			}
		});
	}

	@Transactional
	public void deleteByOwner(UUID postUuid, Users user) {
		Post post = postRepository.findByPostUuid(postUuid)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
		if (!post.getUserProfile().getUser().getUserId().equals(user.getUserId()))
			throw new SecurityException("삭제 권한이 없습니다.");
		likeRepository.deleteByPost(post);
		postCommentRepository.deleteByPost(post);
		postTagMappingRepository.deleteAll(postTagMappingRepository.findByPost(post));
		postRepository.delete(post);
	}

	@Transactional
	public void ignore(UUID uuid) {
		Optional<Post> optionalPost=this.postRepository.findByPostUuid(uuid);
		if (optionalPost.isPresent()) {
		    Post post = optionalPost.get();
		    post.setCensored(false);
		}
	}

	@Transactional
	public void delete(UUID uuid) {
		this.postRepository.findByPostUuid(uuid).ifPresent(post -> {
			likeRepository.deleteByPost(post);
			postCommentRepository.deleteByPost(post);
			postTagMappingRepository.deleteAll(postTagMappingRepository.findByPost(post));
			this.postRepository.delete(post);
		});
	}
}
