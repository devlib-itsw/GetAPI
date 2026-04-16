package com.getapi.post.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.getapi.comment.controller.PostCommentForm;
import com.getapi.comment.domain.PostComment;
import com.getapi.comment.service.PostCommentService;
import com.getapi.post.domain.Like;
import com.getapi.post.domain.Post;
import com.getapi.post.domain.PostTagMapping;
import com.getapi.post.service.LikeService;
import com.getapi.post.service.PostService;
import com.getapi.post.service.PostTagMappingService;
import com.getapi.tag.domain.Tag;
import com.getapi.user.domain.Users;
import com.getapi.user.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/community")
public class PostController {
	private final UserService userService;
	private final PostService postService;
	private final PostTagMappingService postTagMappingService;
	private final PostCommentService postCommentService;
	private final LikeService likeService;
	
	
	
	@GetMapping("")
	public String getPost(Model model,
	        @RequestParam(value="page", defaultValue="0") int page,
	        @AuthenticationPrincipal Users userDetails) {

		Page<Post> paging = this.postService.getList(page);

		Map<Long, Long> likeMap = this.likeService.getLikeCountMap(paging.getContent());

		model.addAttribute("likeMap", likeMap);
	    model.addAttribute("paging", paging);

	    Map<Long, List<Tag>> postTagMap = this.postTagMappingService.getTagMap(paging.getContent());

	    model.addAttribute("postTagMap", postTagMap);
	    

	    return "community";
	}
	
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/write")
	public String getWrite(WriteForm writeForm) {
		return "community-write";
	}
	
	@PostMapping("/write")
	@PreAuthorize("isAuthenticated()")
	public String registPost(@Valid WriteForm writeForm, BindingResult bindingResult, Principal principal,
			@AuthenticationPrincipal String sub) {
		if(bindingResult.hasErrors()) {
			bindingResult.getAllErrors().forEach(error -> {
		        System.out.println("Validation Error: " + error.getDefaultMessage());
		    });
			return "community-write";
		}
		
		Users user = this.userService.getProviderId(sub);
		
		this.postService.create(writeForm.getTitle(), writeForm.getContent(), writeForm.getTagList(), user);
		
		return "redirect:/community";
	}
	
	@GetMapping("/view/{postUuid}")
	@PreAuthorize("isAuthenticated()")
	public String viewPost(Model model,
	                       @PathVariable("postUuid") UUID postUuid,
	                       @AuthenticationPrincipal String sub,
	                       PostCommentForm postCommentForm,
	                       HttpServletRequest request,
	                       HttpServletResponse response) {

	    String cookieName = "view_post_" + postUuid;
	    boolean alreadyViewed = false;

	    Cookie[] cookies = request.getCookies();
	    if (cookies != null) {
	        for (Cookie cookie : cookies) {
	            if (cookie.getName().equals(cookieName)) {
	                alreadyViewed = true;
	                break;
	            }
	        }
	    }

	    Post post;

	    if (!alreadyViewed) {
	        post = this.postService.getPost(postUuid); // 조회수 증가
	        Cookie newCookie = new Cookie(cookieName, "true");
	        newCookie.setMaxAge(60 * 60);
	        newCookie.setPath("/");
	        response.addCookie(newCookie);
	    } else {
	        post = this.postService.findByUuid(postUuid);
	    }
	    
	    List<PostComment> postComments = this.postCommentService.getPostCommentsByPost(post);
	    List<Like> likes = this.likeService.findLikesByPost(post);

	    List<Tag> tags = this.postTagMappingService.getMappings(post)
	            .stream()
	            .map(PostTagMapping::getTag)
	            .toList();

	    boolean liked = false;

	    Users user = this.userService.getProviderId(sub);

	    if (user != null) {
	        liked = likes.stream()
	            .anyMatch(like -> like.getUser().getUserId().equals(user.getUserId()));
	    }

	    model.addAttribute("liked", liked);
	    model.addAttribute("tags", tags);
	    model.addAttribute("post", post);
	    model.addAttribute("likes", likes);
	    model.addAttribute("postComments", postComments);

	    return "community-view";
	}
}
