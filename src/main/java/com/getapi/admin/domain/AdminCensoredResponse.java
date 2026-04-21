package com.getapi.admin.domain;

import java.time.LocalDateTime;

import com.getapi.user.domain.Users;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCensoredResponse {
	private Long id;
	private String title;
	private String content;
	private String uuid;
	private LocalDateTime date;
	private Users user;
	
	public AdminCensoredResponse(Long id, String title, String content, String uuid, LocalDateTime date, Users user) {
		this.id = id;
		this.title = title;
		this.content = content;
		this.uuid = uuid;
		this.date = date;
		this.user = user;
	}
}
