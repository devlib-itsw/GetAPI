package com.getapi.post.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.getapi.post.domain.Like;
import com.getapi.post.domain.Post;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
	List<Like> findByPost_PostId(Long postId);
	boolean existsByPost_PostIdAndUser_UserId(Long postId, Long userId);
	void deleteByPost_PostIdAndUser_UserId(Long postId, Long userId);
	@Query("""
	    SELECT l.post.postId, COUNT(l)
	    FROM Like l
	    WHERE l.post.postId IN :postIds
	    GROUP BY l.post.postId
	""")
	List<Object[]> countLikesByPostIds(@Param("postIds") List<Long> postIds);
	List<Like> findByPost(Post post);
}
