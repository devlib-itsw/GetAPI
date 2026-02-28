package com.getapi.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateDto {
	private String nickname;
    private String introduction;
    private String webUrl;
    private String profileImage;
}
