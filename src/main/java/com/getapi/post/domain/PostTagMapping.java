package com.getapi.post.domain;

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
public class PostTagMapping {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long mapId;

	@ManyToOne
	@JoinColumn(name="post_id", nullable=false)
	private Post post;

	@ManyToOne
	@JoinColumn(name="tag_id", nullable=false)
	private Tag tag;
}