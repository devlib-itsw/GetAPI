package com.getapi.user.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {
	private String nickname;
	private String introduction;
	private String website;
}
