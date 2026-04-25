package com.getapi.admin.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.getapi.admin.domain.AdminCensoredResponse;
import com.getapi.ai.service.AiFeedbackService;
import com.getapi.user.domain.UserProfile;
import com.getapi.api.service.ApiService;
import com.getapi.comment.service.ApiCommentService;
import com.getapi.comment.service.PostCommentService;
import com.getapi.payments.service.PaymentsService;
import com.getapi.post.domain.Post;
import com.getapi.post.service.PostService;
import com.getapi.user.domain.UserRequest;
import com.getapi.user.domain.Users;
import java.util.LinkedHashMap;
import java.util.Map;
import com.getapi.user.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
	private final UserService userService;
	private final PostService postService;
	private final ApiService apiService;
	private final PostCommentService postCommentService;
	private final ApiCommentService apiCommentService;
	private final PaymentsService paymentsService;
	@Autowired
	private AiFeedbackService aiFeedbackService;

	@GetMapping("")
	public String returnHtml(Model model) {
		// 포인트 관리 탭 (회원 포인트 잔액)
		Page<Users> users=this.userService.getUsersBySearchPage(0, null);
		model.addAttribute("userList", users);

		// 포인트 관리 탭 (전체 회원 수)
		long totalUsers=this.userService.totalUsers();
		model.addAttribute("totalUsers", totalUsers);
		
		// 포인트 관리 탭 (전체 보유 포인트)
		Long totalPoints=this.paymentsService.totalPoints();
		model.addAttribute("totalPoints", totalPoints);
		
		// 검열 게시물 관리 탭
		Page<AdminCensoredResponse> posts=this.postService.getPostsByIsCensoredPage(0);
		model.addAttribute("censoredPostList", posts);
		
		Page<AdminCensoredResponse> apis=this.apiService.getApisByIsCensoredPage(0);
		model.addAttribute("censoredApiList", apis);
		
		Page<AdminCensoredResponse> postComments=this.postCommentService.getPostCommentsByIsCensoredPage(0);
		model.addAttribute("censoredPostCommentList", postComments);
		
		Page<AdminCensoredResponse> apiComments=this.apiCommentService.getApiCommentsByIsCensoredPage(0);
		model.addAttribute("censoredApiCommentList", apiComments);
		
		Page<AdminCensoredResponse> userlist=this.userService.getUsersByIsCensoredPage(0);
		model.addAttribute("censoredUserList", userlist);
		
		return "admin";
	}

	@PostMapping("/js/searchUsers")
	@ResponseBody
	public Page<Map<String, Object>> getUsersJson(@RequestBody UserRequest userRequest) {
		Page<Users> page = this.userService.getUsersBySearchPage(userRequest.getPage(), userRequest.getKeyword());
		return page.map(user -> {
			UserProfile profile = user.getUserProfile();
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("userUuid", user.getUserUuid());
			m.put("email", user.getEmail());
			m.put("point", user.getPoint());
			m.put("profileCreatedAt", user.getProfileCreatedAt());
			m.put("name", profile != null ? profile.getName() : null);
			m.put("nickname", profile != null ? profile.getNickname() : null);
			return m;
		});
	}

	@GetMapping("/community/{page}")
	@ResponseBody
	public Page<AdminCensoredResponse> getPostPage(@PathVariable("page") int page) {
		return this.postService.getPostsByIsCensoredPage(page);
	}

	@GetMapping("/library/{page}")
	@ResponseBody
	public Page<AdminCensoredResponse> getLibraryPage(@PathVariable("page") int page) {
		return this.apiService.getApisByIsCensoredPage(page);
	}
	
	@GetMapping("/postComment/{page}")
	@ResponseBody
	public Page<AdminCensoredResponse> getPostCommentPage(@PathVariable("page") int page){
		return this.postCommentService.getPostCommentsByIsCensoredPage(page);
	}
	
	@GetMapping("/apiComment/{page}")
	@ResponseBody
	public Page<AdminCensoredResponse> getApiCommentPage(@PathVariable("page") int page){
		return this.apiCommentService.getApiCommentsByIsCensoredPage(page);
	}
	
	@GetMapping("/user/{page}")
	@ResponseBody
	public Page<AdminCensoredResponse> getUserPage(@PathVariable("page") int page){
		return this.userService.getUsersByIsCensoredPage(page);
	}
	
	@PatchMapping("/community/{uuid}")
	@ResponseBody
	public void ignorePost(@PathVariable("uuid") UUID uuid) {
		try {
			Post post = postService.findByUuid(uuid);
			aiFeedbackService.recordFalsePositive(
				post.getTitle() + " " + post.getContent(), "post", uuid.toString(), null);
		} catch (Exception ignored) {}
		this.postService.ignore(uuid);
	}
	@DeleteMapping("/community/{uuid}")
	@ResponseBody
	public void deletePost(@PathVariable("uuid") UUID uuid) {
		try {
			Post post = postService.findByUuid(uuid);
			aiFeedbackService.recordConfirmed(
				post.getTitle() + " " + post.getContent(), "post", uuid.toString(), null);
		} catch (Exception ignored) {}
		this.postService.delete(uuid);
	}
	
	@PatchMapping("/library/{uuid}")
	@ResponseBody
	public void ignoreApi(@PathVariable("uuid") UUID uuid) {
		this.apiService.ignore(uuid);
	}
	@DeleteMapping("/library/{uuid}")
	@ResponseBody
	public void deleteApi(@PathVariable("uuid") UUID uuid) {
		this.apiService.delete(uuid);
	}

	@PatchMapping("/postComment/{uuid}")
	@ResponseBody
	public void ignorePostComment(@PathVariable("uuid") UUID uuid) {
		try {
			String text = postCommentService.findByCommentUuid(uuid).getContent();
			aiFeedbackService.recordFalsePositive(text, "postComment", uuid.toString(), null);
		} catch (Exception ignored) {}
		this.postCommentService.ignore(uuid);
	}
	@DeleteMapping("/postComment/{uuid}")
	@ResponseBody
	public void deletePostComment(@PathVariable("uuid") UUID uuid) {
		try {
			String text = postCommentService.findByCommentUuid(uuid).getContent();
			aiFeedbackService.recordConfirmed(text, "postComment", uuid.toString(), null);
		} catch (Exception ignored) {}
		this.postCommentService.delete(uuid);
	}

	@PatchMapping("/apiComment/{uuid}")
	@ResponseBody
	public void ignoreApiComment(@PathVariable("uuid") UUID uuid) {
		this.apiCommentService.ignore(uuid);
	}
	@DeleteMapping("/apiComment/{uuid}")
	@ResponseBody
	public void deleteApiComment(@PathVariable("uuid") UUID uuid) {
		this.apiCommentService.delete(uuid);
	}

	@GetMapping("/ai/feedback")
	@ResponseBody
	public Object getFeedback() {
		return aiFeedbackService.getAll();
	}
	
	@PatchMapping("/user/{uuid}")
	@ResponseBody
	public void ignoreUser(@PathVariable("uuid") UUID uuid) {
		try {
			com.getapi.user.domain.UserProfile profile = userService.getProfileByUserUuid(uuid);
			String text = (profile.getNickname() != null ? profile.getNickname() : "") + " "
					+ (profile.getIntroduction() != null ? profile.getIntroduction() : "");
			aiFeedbackService.recordFalsePositive(text.trim(), "user", uuid.toString(), null);
		} catch (Exception ignored) {}
		this.userService.ignore(uuid);
	}
	@DeleteMapping("/user/{uuid}")
	@ResponseBody
	public void deleteUser(@PathVariable("uuid") UUID uuid) {
		try {
			com.getapi.user.domain.UserProfile profile = userService.getProfileByUserUuid(uuid);
			String text = (profile.getNickname() != null ? profile.getNickname() : "") + " "
					+ (profile.getIntroduction() != null ? profile.getIntroduction() : "");
			aiFeedbackService.recordConfirmed(text.trim(), "user", uuid.toString(), null);
		} catch (Exception ignored) {}
		this.userService.delete(uuid);
	}
}
