package com.getapi.post.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.post.domain.Like;
import com.getapi.post.domain.Post;
import com.getapi.post.repository.LikeRepository;
import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LikeService {
	private final LikeRepository likeRepository;
	private final UserProfileRepository userProfileRepository;
	
	public void add(Post post, Users user) {
		Like l = new Like();
		l.setLikeId(null);
		l.setPost(post);
		UserProfile profile = userProfileRepository.findByUser(user)
				.orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다."));
		l.setUserProfile(profile);
		this.likeRepository.save(l);
	}
	
	//public List<Like> findLikesByPostId(Long postId) {
	  //  return this.likeRepository.findByPost_PostId(postId);
	//}
	
	public List<Like> findLikesByPost(Post post) {
        return likeRepository.findByPost(post);
    }
	
	public boolean isLiked(Long postId, Long userId) {
		Long profileId = userProfileRepository.findByUser_UserId(userId)
				.orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다."))
				.getProfileId();
		return this.likeRepository.existsByPost_PostIdAndUserProfile_ProfileId(postId, profileId);
	}

	@Transactional
	public void remove(Long postId, Long userId) {
		Long profileId = userProfileRepository.findByUser_UserId(userId)
				.orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다."))
				.getProfileId();
		this.likeRepository.deleteByPost_PostIdAndUserProfile_ProfileId(postId, profileId);
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
