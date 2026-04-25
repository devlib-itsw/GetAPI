package com.getapi.user.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Users {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	@Column(unique = true, nullable = false)
	private UUID userUuid;

	@Column(unique = true, nullable = false)
	private String providerId;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(unique = true, nullable = false)
	private String phone;

	@Column(nullable = false)
	private String role;

	@Column(nullable = false)
	private LocalDateTime profileCreatedAt;

	private LocalDateTime profileDeletedAt;

	private Long point = 0L;

	@JsonIgnore
	@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
	private UserProfile userProfile;

	public Users(Long userId, UUID userUuid, String providerId, String email,
				 String phone, String role, LocalDateTime profileCreatedAt,
				 LocalDateTime profileDeletedAt, Long point) {
		this.userId = userId;
		this.userUuid = userUuid;
		this.providerId = providerId;
		this.email = email;
		this.phone = phone;
		this.role = role;
		this.profileCreatedAt = profileCreatedAt;
		this.profileDeletedAt = profileDeletedAt;
		this.point = point;
	}
}
