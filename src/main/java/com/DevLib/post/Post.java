package com.DevLib.post;

import java.time.LocalDateTime;
import java.util.UUID;

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
public class Post {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long post_id;
	
	@Column(unique=true)
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID post_uuid;
	
	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private Users user;
	
	@Column(nullable=false)
	private String title;
	
	@Column(columnDefinition="TEXT", nullable=false)
	private String content;
	
	private Long view_count=0L;
	
	private Long like_count=0L;
	
	private boolean is_censored;

	@Column(nullable=false)
	private LocalDateTime created_at;
	
	@Column(nullable=false)
	private LocalDateTime updated_at;
}
