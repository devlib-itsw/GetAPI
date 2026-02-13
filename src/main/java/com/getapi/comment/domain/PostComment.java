package com.getapi.comment.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.getapi.post.domain.Post;
import com.getapi.user.domain.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class PostComment {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long commentId;

	@Column(unique=true)
	private UUID commentUuid;

	@ManyToOne
	@JoinColumn(name="post_id", nullable=false)
	private Post post;

	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private Users user;

	@Column(columnDefinition="TEXT")
	private String content;

	private boolean isCensored;

	@Column(nullable=false)
	private LocalDateTime createdAt;

	@Column(nullable=false)
	private LocalDateTime updatedAt;
}
