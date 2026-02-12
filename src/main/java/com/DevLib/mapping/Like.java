package com.DevLib.mapping;

import com.DevLib.post.Post;
import com.DevLib.user.Users;

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
public class Like {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long like_id;
	
	@ManyToOne
	@JoinColumn(name="post_id", nullable=false)
	private Post post;
	
	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private Users user;
}
