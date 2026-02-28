package com.getapi.post.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.getapi.post.domain.Post;
import com.getapi.post.domain.PostTagMapping;
import com.getapi.post.service.PostService;
import com.getapi.post.service.PostTagMappingService;
import com.getapi.tag.domain.Tag;
import com.getapi.user.domain.Users;
import com.getapi.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/community")
public class PostController {
	private final UserService userService;
	private final PostService postService;
	private final PostTagMappingService postTagMappingService;
	
	
	
	@GetMapping("/list")
	public String getPost(Model model,
	        @RequestParam(value="page", defaultValue="0") int page,
	        @AuthenticationPrincipal Users userDetails) {

	    Page<Post> paging = this.postService.getList(page);
	    model.addAttribute("paging", paging);

	    Map<Long, List<Tag>> postTagMap = new HashMap<>();

	    for (Post post : paging.getContent()) {

	        List<Tag> tags = this.postTagMappingService.getMappings(post)
	                .stream()
	                .map(PostTagMapping::getTag)
	                .toList();

	        postTagMap.put(post.getPostId(), tags);
	    }

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
		
		return "redirect:/community/list";
	}
}
