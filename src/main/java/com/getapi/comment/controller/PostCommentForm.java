package com.getapi.comment.controller;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostCommentForm {
	@NotEmpty(message="내용을 입력해주세요.")
    private String content;
}
