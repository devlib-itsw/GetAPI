package com.DevLib.user;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Users {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long user_id;
	
	@Column(unique=true)
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID user_uuid;
	
	@Column(unique=true, nullable=false)
	private String provider_id;
	
	@Column(unique=true, nullable=false)
	private String email;
	
	@Column(unique=true, nullable=false)
	private String phone;
	
	@Column(nullable=false)
	private String name; // google name
	
	private String nickname; // custom name
	
	@Column(columnDefinition="TEXT")
	private String introduction;
	
	private String web_url;
	
	private Long point=0L;
	
	@Column(unique=true, nullable=false)
	private String api_key;
	
	@Column(nullable=false)
	private String profile_image;
	
	@Column(nullable=false)
	private String role;
	
	@Column(nullable=false)
	private String secret_key;
	
	@Column(nullable=false)
	private String rotate_token;
	
	@Column(nullable=false)
	private LocalDateTime profile_created_at;
	
	@Column(nullable=false)
	private LocalDateTime api_updated_at;
}
