package com.DevLib.mapping;

import com.DevLib.api.Api;
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
public class Star {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long star_id;
	
	@ManyToOne
	@JoinColumn(name="api_id", nullable=false)
	private Api api;
	
	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private Users user;
}
