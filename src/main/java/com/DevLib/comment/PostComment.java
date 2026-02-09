package com.DevLib.comment;

import java.time.LocalDateTime;
import java.util.UUID;

import com.DevLib.post.Post;
import com.DevLib.user.Users;

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
	private Long comment_id;
	
	@Column(unique=true)
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID comment_uuid;
	
	@ManyToOne
	@JoinColumn(name="post_id", nullable=false)
	private Post post;
	
	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private Users user;
	
	@Column(columnDefinition="TEXT")
	private String content;
	
	private boolean is_censored;
	
	@Column(nullable=false)
	private LocalDateTime created_at;
	
	@Column(nullable=false)
	private LocalDateTime updated_at;
}
