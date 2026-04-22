package com.getapi.post.domain;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;


import com.getapi.user.domain.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Post {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long postId;

	@Column(unique=true)
	private UUID postUuid;

	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
// <<<<<<< siwoo
	private Users user;
	
	
	
// =======
 	private Users user;

// >>>>>>> develop
	@Column(nullable=false)
	private String title;

	@Column(columnDefinition="TEXT", nullable=false)
	private String content;
	
	private Long viewCount=0L;

//	private Long likeCount=0L;

	private boolean isCensored;

	@Column(nullable=false)
	private LocalDateTime createdAt;

	@Column(nullable=false)
	private LocalDateTime updatedAt;
	
}
