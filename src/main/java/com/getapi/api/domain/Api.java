package com.getapi.api.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.getapi.user.domain.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Api {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long apiId;

	@Column(unique=true)
	private UUID apiUuid;
	
	
	@OneToMany(mappedBy = "api") //4월14일
	private List<ApiTagMapping> apiTagMappings = new ArrayList<>();
	
	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private Users user;

	@Column(nullable=false)
	private String name;

	@Column(columnDefinition="TEXT", nullable=false)
	private String originalUrl; // AES

	@Column(nullable=false, unique=true)
	private String proxyUrl;

	@Column(columnDefinition="TEXT")
	private String description;

	@Column(nullable=false)
	private String docUrl;

	@Column(nullable=false)
	private Long price;

	private Long viewCount=0L;

//	private Long starCount=0L;

	private String status; // pending -challenge-> active / blocked

	private boolean isCensored;

	@Column(nullable=false)
	private LocalDateTime createdAt;

	@Column(nullable=false)
	private LocalDateTime updatedAt;
}
