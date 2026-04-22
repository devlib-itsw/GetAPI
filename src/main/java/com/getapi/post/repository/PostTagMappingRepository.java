package com.getapi.post.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.getapi.post.domain.Post;
import com.getapi.post.domain.PostTagMapping;

@Repository
public interface PostTagMappingRepository extends JpaRepository<PostTagMapping, Long>{
	List<PostTagMapping> findByPost(Post postid);
	@Query("SELECT ptm FROM PostTagMapping ptm WHERE ptm.post.postId IN :postIds")
	List<PostTagMapping> findByPostIds(@Param("postIds") List<Long> postIds);
}
