package com.getapi.comment.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.getapi.comment.domain.PostComment;
import com.getapi.comment.service.PostCommentService;
import com.getapi.post.domain.Post;
import com.getapi.post.service.PostService;
import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import com.getapi.user.service.UserProfileService;
import com.getapi.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/comment")
public class PostCommentController {

	private final PostCommentService postCommentService;
	private final UserService userService;
	private final PostService postService;
	private final UserProfileService userProfileService;
	
	@PostMapping("/write/{id}")
	@PreAuthorize("isAuthenticated()")
	public String registPost(@PathVariable("id") Long id, @Valid PostCommentForm postCommentForm, BindingResult bindingResult, Principal principal,
			@AuthenticationPrincipal String sub) {
//		if(bindingResult.hasErrors()) {
//			bindingResult.getAllErrors().forEach(error -> {
//		        System.out.println("Validation Error: " + error.getDefaultMessage());
//		    });
//			return "redirect:/community/list";
//		}
		
		Post post = this.postService.findById(id);
		Users user = this.userService.getProviderId(sub);

		
		
		PostComment postComment = this.postCommentService.create(postCommentForm.getContent(), user, post);
		
		return String.format("redirect:/community/view/%s#postComment_%s", post.getPostUuid(), postComment.getCommentId());
	}
	
	@GetMapping("/modify/{postUuid}/{commentUuid}")
	@PreAuthorize("isAuthenticated()")
	public String modifyPage(Model model, @PathVariable("postUuid") UUID postUuid, @PathVariable("commentUuid") UUID commentUuid, PostCommentForm postCommentForm) {
		model.addAttribute("postUuid", postUuid);
		model.addAttribute("commentUuid", commentUuid);
		model.addAttribute("postCommentForm", postCommentForm);
		return "postComment-modify";
	}
	
	@PostMapping("modify/{postUuid}/{commentUuid}")
	@PreAuthorize("isAuthenticated()")
	public String modifyPostComment(@PathVariable("postUuid") UUID postUuid, @PathVariable("commentUuid") UUID commentUuid, PostCommentForm postCommentForm, BindingResult bindingResult, Principal principal,
			@AuthenticationPrincipal String sub) {
		PostComment postComment = this.postCommentService.findByCommentUuid(commentUuid);
		this.postCommentService.modify(postComment, postCommentForm.getContent());
		return String.format("redirect:/community/view/%s#postComment_%s", postUuid, postComment.getCommentId());
	}
	
	@GetMapping("delete/{postId}/{postCommentId}")
	@PreAuthorize("isAuthenticated()")
	public String deleteAction(@PathVariable("postId") Long postId, @PathVariable("postCommentId") Long postCommentId) {
		Post post = this.postService.findById(postId);
		this.postCommentService.delete(postCommentId);
		return String.format("redirect:/community/view/%s", post.getPostUuid());
	}
	
}
