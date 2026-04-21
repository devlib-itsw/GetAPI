package com.getapi.user.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {
	private String keyword;
	private int page;
}
