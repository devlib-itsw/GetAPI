package com.getapi.comment.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.getapi.comment.domain.PostComment;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
	Page<PostComment> findByIsCensoredTrue(Pageable page);
	
	PostComment findByCommentUuid(UUID uuid);
	void deleteByCommentUuidAndIsCensoredTrue(UUID uuid);
}
