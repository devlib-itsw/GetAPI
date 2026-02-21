package com.getapi.user.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Users {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long userId;

	@Column(unique=true, nullable=false)
	private UUID userUuid;

	@Column(unique=true, nullable=false)
	private String providerId;

	@Column(unique=true, nullable=false)
	private String email;

	@Column(unique=true, nullable=false)
	private String phone;

	@Column(nullable=false)
	private String name; // google name

	private String nickname; // custom name

	@Column(columnDefinition="TEXT")
	private String introduction;

	private String webUrl;

	private Long point=0L;

	@Column(unique=true)
	private String apiKey;

	@Column(nullable=false)
	private String profileImage;

	@Column(nullable=false)
	private String role;

	private String secretKey;

	@Column(nullable=false)
	private String rotationToken;

	@Column(nullable=false)
	private LocalDateTime profileCreatedAt;

	private LocalDateTime apiUpdatedAt;

	private LocalDateTime profileDeletedAt;
}
