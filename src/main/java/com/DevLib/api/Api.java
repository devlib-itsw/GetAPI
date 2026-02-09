package com.DevLib.api;

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
public class Api {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long api_id;
	
	@Column(unique=true)
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID api_uuid;
	
	@ManyToOne
	@JoinColumn(name="user_id", nullable=false)
	private Users user;
	
	@Column(nullable=false)
	private String name;
	
	@Column(columnDefinition="TEXT", nullable=false)
	private String original_url; // AES 암호화
	
	@Column(nullable=false, unique=true)
	private String proxy_url;
	
	@Column(columnDefinition="TEXT")
	private String description;
	
	@Column(nullable=false)
	private String doc_url;
	
	@Column(nullable=false)
	private Long price;
	
	private Long view_count=0L;
	
	private Long star_count=0L;
	
	private String status; // pending -challenge-> active / blocked
	
	private boolean is_censored;
	
	@Column(nullable=false)
	private LocalDateTime created_at;
	
	@Column(nullable=false)
	private LocalDateTime updated_at;
}
