package com.getapi.api.domain;

import com.getapi.user.domain.UserProfile;

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
	private Long starId;

	@ManyToOne
	@JoinColumn(name="api_id", nullable=false)
	private Api api;

	@ManyToOne
	@JoinColumn(name="user_profile_id", nullable=false)
	private UserProfile userProfile;
}
