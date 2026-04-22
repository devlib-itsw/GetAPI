package com.getapi.post.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.post.domain.Like;
import com.getapi.post.domain.Post;
import com.getapi.post.repository.LikeRepository;
import com.getapi.user.domain.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LikeService {
	private final LikeRepository likeRepository;
	
	public void add(Post post, Users user) {
		Like l = new Like();
		l.setLikeId(null);
		l.setPost(post);
		l.setUser(user);
		this.likeRepository.save(l);
	}
	
	//public List<Like> findLikesByPostId(Long postId) {
	  //  return this.likeRepository.findByPost_PostId(postId);
	//}
	
	public List<Like> findLikesByPost(Post post) {
        return likeRepository.findByPost(post);
    }
	
	public boolean isLiked(Long postId, Long userId) {
	    return this.likeRepository.existsByPost_PostIdAndUser_UserId(postId, userId);
	}
	
	@Transactional
	public void remove(Long postId, Long userId) {
	    this.likeRepository.deleteByPost_PostIdAndUser_UserId(postId, userId);
	}
	
	public Map<Long, Long> getLikeCountMap(List<Post> posts) {
	    List<Long> postIds = posts.stream()
	            .map(Post::getPostId)
	            .toList();

	    List<Object[]> results = likeRepository.countLikesByPostIds(postIds);

	    Map<Long, Long> likeMap = new HashMap<>();
	    for (Object[] row : results) {
	        Long postId = (Long) row[0];
	        Long count = (Long) row[1];
	        likeMap.put(postId, count);
	    }

	    return likeMap;
	}
}
