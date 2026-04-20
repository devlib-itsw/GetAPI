package com.getapi.post.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.getapi.post.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
	/* @Query("SELECT p FROM Post p WHERE p.isCensored") */
	Page<Post> findByIsCensoredTrue(Pageable page);
	
	Post findByPostUuid(UUID uuid);
	void deleteByPostUuidAndIsCensoredTrue(UUID uuid);
}
