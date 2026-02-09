package com.DevLib.tag;

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
public class Tag {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long tag_id;
	
	@Column(unique=true)
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID tag_uuid;
	
	@Column(nullable=false)
	private String tag;
	
	private boolean is_censored;
}
