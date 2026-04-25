package com.getapi.comment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.getapi.comment.domain.PostComment;
import com.getapi.post.domain.Post;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
	@Query("""
        SELECT pc
        FROM PostComment pc
        JOIN FETCH pc.userProfile
        WHERE pc.post.postId = :postId
          AND pc.isCensored = false
        ORDER BY pc.createdAt DESC
    """)
    List<PostComment> getPostComments(@Param("postId") Long postId);

	List<PostComment> findByPostAndIsCensoredFalse(Post post);
	Optional<PostComment> findByCommentUuid(UUID commentUuid);

	Page<PostComment> findByIsCensoredTrue(Pageable page);

	void deleteByCommentUuidAndIsCensoredTrue(UUID uuid);
	void deleteByUserProfile(com.getapi.user.domain.UserProfile userProfile);
	void deleteByPost(com.getapi.post.domain.Post post);
}
