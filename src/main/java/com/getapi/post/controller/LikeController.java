package com.getapi.post.controller;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.getapi.post.domain.Post;
import com.getapi.post.service.LikeService;
import com.getapi.post.service.PostService;
import com.getapi.user.domain.Users;
import com.getapi.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/post/like")
public class LikeController {
	
	private final UserService userService;
	private final PostService postService;
	private final LikeService likeService;
	
	@GetMapping("/add/{postId}")
	@PreAuthorize("isAuthenticated()")
	public String addLike(@PathVariable("postId") Long postId, Principal principal,
			@AuthenticationPrincipal String sub) {
		Post post = this.postService.findById(postId); // post(게시물) 정보
		Users user = this.userService.getProviderId(sub); // 현재 로그인 되어있는 유저정보
		if(this.likeService.isLiked(postId, user.getUserId())) {
			this.likeService.remove(postId, user.getUserId());
		} else {
			this.likeService.add(post, user);			
		}
		return String.format("redirect:/community/view/%s", post.getPostUuid());
	}
}
