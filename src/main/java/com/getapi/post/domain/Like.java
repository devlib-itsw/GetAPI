package com.getapi.post.domain;

import com.getapi.user.domain.Users;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="likes")
public class Like {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long likeId;

	@ManyToOne
	@JoinColumn(name="post_id", nullable=false)
	private Post post;

	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private Users user;
}
