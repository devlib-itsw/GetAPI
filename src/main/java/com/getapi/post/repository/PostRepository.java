package com.getapi.post.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.post.domain.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post>{
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
	
	@Query("""
	    SELECT DISTINCT p
	    FROM Post p
	    LEFT JOIN p.user u1
	    WHERE p.title LIKE CONCAT('%', :kw, '%')
	       OR u1.name LIKE CONCAT('%', :kw, '%')
	""")
	Page<Post> findAllByKeyword(@Param("kw") String kw, Pageable pageable);
	
	@Query(value = """
        SELECT p.* FROM post p
        LEFT JOIN users u ON p.user_id = u.user_id
        WHERE (:kw IS NULL OR :kw = '' 
           OR p.title LIKE %:kw% 
           OR p.content LIKE %:kw% 
           OR u.name LIKE %:kw%)
        GROUP BY p.post_id, p.content, p.created_at, p.is_censored, p.post_uuid, p.title, p.updated_at, p.user_id, p.view_count
        ORDER BY (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.post_id) DESC, p.created_at DESC
        """, 
        countQuery = """
        SELECT COUNT(DISTINCT p.post_id) FROM post p
        LEFT JOIN users u ON p.user_id = u.user_id
        WHERE (:kw IS NULL OR :kw = '' 
           OR p.title LIKE %:kw% 
           OR p.content LIKE %:kw% 
           OR u.name LIKE %:kw%)
        """, 
        nativeQuery = true)
    Page<Post> findAllOrderByLikes(@Param("kw") String kw, Pageable pageable);
	
}