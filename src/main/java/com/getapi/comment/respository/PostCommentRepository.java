package com.getapi.comment.respository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.getapi.comment.domain.PostComment;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long>{

}
