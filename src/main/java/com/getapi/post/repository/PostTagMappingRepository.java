package com.getapi.post.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.getapi.post.domain.Post;
import com.getapi.post.domain.PostTagMapping;

@Repository
public interface PostTagMappingRepository extends JpaRepository<PostTagMapping, Long>{
	List<PostTagMapping> findByPost(Post postid);
}
