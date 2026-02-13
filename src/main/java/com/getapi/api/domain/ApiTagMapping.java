package com.getapi.api.domain;

import com.getapi.tag.domain.Tag;

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
public class ApiTagMapping {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long mapId;

	@ManyToOne
	@JoinColumn(name="api_id", nullable=false)
	private Api api;

	@ManyToOne
	@JoinColumn(name="tag_id", nullable=false)
	private Tag tag;
}
