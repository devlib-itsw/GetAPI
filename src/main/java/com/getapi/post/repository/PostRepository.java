package com.getapi.post.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.post.domain.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>{
//	@Query("select distinct p from Posts p left join p.userid u1 left join p.answerList a left join a.author u2 where p.title like concat('%', :kw, '%') or p.content like concat('%', :kw, '%') or u1.username like concat('%', :kw, '%') or a.content like concat('%', :kw, '%') or u2.username like concat('%', :kw, '%')")
	Page<Post> findAll(Pageable pageable);
	Optional<Post> findByPostUuid(UUID postUuid);
	
	@Modifying
    @Transactional
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :id")
    void increaseViewCount(@Param("id") Long id);
	
	@Modifying
	@Query("update Post p set p.viewCount = p.viewCount + 1 where p.postUuid = :postUuid")
	void increaseViewCountByUuid(@Param("postUuid") UUID postUuid);
	
	@Query("""
	SELECT p, COUNT(l)
	FROM Post p
	LEFT JOIN Like l ON l.post = p
	GROUP BY p
	""")
	List<Object[]> findPostsWithLikeCount();
}