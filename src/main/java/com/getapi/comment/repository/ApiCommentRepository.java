package com.getapi.comment.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.getapi.comment.domain.ApiComment;

public interface ApiCommentRepository extends JpaRepository<ApiComment, Long> {
	Page<ApiComment> findByIsCensoredTrue(Pageable page);
	
	ApiComment findByCommentUuid(UUID uuid);
	void deleteByCommentUuidAndIsCensoredTrue(UUID uuid);
}
