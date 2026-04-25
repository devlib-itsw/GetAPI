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
	Page<Post> findByIsCensoredFalse(Pageable pageable);
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
	WHERE p.isCensored = false
	GROUP BY p
	""")
	List<Object[]> findPostsWithLikeCount();

	@Query(value = """
		    SELECT p.* FROM post p
		    LEFT JOIN users u ON p.user_id = u.user_id
		    LEFT JOIN user_profile up ON u.user_id = up.user_id
		    WHERE p.is_censored = false
		      AND (:kw IS NULL OR :kw = ''
		       OR p.title LIKE CONCAT('%', :kw, '%')
		       OR up.name LIKE CONCAT('%', :kw, '%'))
		    """,
		    countQuery = """
		    SELECT COUNT(*) FROM post p
		    LEFT JOIN users u ON p.user_id = u.user_id
		    LEFT JOIN user_profile up ON u.user_id = up.user_id
		    WHERE p.is_censored = false
		      AND (:kw IS NULL OR :kw = ''
		       OR p.title LIKE CONCAT('%', :kw, '%')
		       OR up.name LIKE CONCAT('%', :kw, '%'))
		    """,
		    nativeQuery = true)
	Page<Post> findAllByKeyword(@Param("kw") String kw, Pageable pageable);

	@Query(value = """
        SELECT p.* FROM post p
        LEFT JOIN user_profile u ON p.user_profile_id = u.profile_id
        WHERE p.is_censored = false
          AND (:kw IS NULL OR :kw = ''
           OR p.title LIKE %:kw%
           OR p.content LIKE %:kw%
           OR u.name LIKE %:kw%)
        GROUP BY p.post_id, p.content, p.created_at, p.is_censored, p.post_uuid, p.title, p.updated_at, p.user_profile_id, p.view_count
        ORDER BY (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.post_id) DESC, p.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT p.post_id) FROM post p
        LEFT JOIN user_profile u ON p.user_profile_id = u.profile_id
        WHERE p.is_censored = false
          AND (:kw IS NULL OR :kw = ''
           OR p.title LIKE %:kw%
           OR p.content LIKE %:kw%
           OR u.name LIKE %:kw%)
        """,
        nativeQuery = true)
    Page<Post> findAllOrderByLikes(@Param("kw") String kw, Pageable pageable);

	void deleteByPostUuidAndIsCensoredTrue(UUID uuid);

	List<Post> findByUserProfileAndIsCensoredFalse(com.getapi.user.domain.UserProfile userProfile);

	Page<Post> findByIsCensoredTrue(Pageable page);
}
