package com.getapi.post.repository;

import org.aspectj.weaver.patterns.TypePatternQuestions.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.getapi.post.domain.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>{
//	@Query("select distinct p from Posts p left join p.userid u1 left join p.answerList a left join a.author u2 where p.title like concat('%', :kw, '%') or p.content like concat('%', :kw, '%') or u1.username like concat('%', :kw, '%') or a.content like concat('%', :kw, '%') or u2.username like concat('%', :kw, '%')")
	Page<Post> findAll(Pageable pageable);
}
