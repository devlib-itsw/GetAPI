package com.getapi.user.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ui.Model;

import com.getapi.auth.service.RefreshTokenService;
import com.getapi.auth.util.SecureUtil;
import com.getapi.user.domain.Users;
import com.getapi.user.dto.UserUpdateDto;
import com.getapi.user.repository.UserRepository;
import com.getapi.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
	private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
	
	@GetMapping("/mypage")
	public String returnHtml(@AuthenticationPrincipal OAuth2User oAuth2User,Model model) {
	    
		return "mypage";
	}
	
	@GetMapping("/profile.html")
	public String profilePage() {
	    return "profile"; // src/main/resources/templates/profile.html 파일을 보여줌
	}

	
	// 1. 마이페이지 화면 이동 및 데이터 전달
	@PostMapping("/mypage/edit/{uuid}") // PostMapping 사용
	public String updateMyPage(
	        @PathVariable("uuid") UUID uuid,
	        UserUpdateDto updateDto) { // @RequestBody 제거! 폼 데이터는 그냥 객체로 받습니다.
	    
	    // 서비스 호출하여 DB 수정
	    userService.updateUserInfo(uuid, 
	                               updateDto.getNickname(), 
	                               updateDto.getIntroduction(), 
	                               updateDto.getWebUrl());
	    
	    // 6. 중요: 수정이 끝난 후 다시 마이페이지 화면으로 보냅니다. (새로고침 효과)
	    return "redirect:/user/mypage/view/" + uuid;
	}//이시우
	@GetMapping("/mypage/view/{uuid}")
	public String viewMyPage(@PathVariable("uuid") UUID uuid, Model model) {
	    Users user = userService.getUserByUUID(uuid);
	    model.addAttribute("loginUser", user); // 여기서 loginUser에 "이시우"가 담김
	    return "mypage";
	}
	
	@DeleteMapping("/{id}")
	@ResponseBody // JSON 또는 상태 코드를 직접 반환하기 위해 필요
	public ResponseEntity<?> deleteUser(
			@PathVariable("id") UUID uuid,
			@CookieValue("REFRESH-TOKEN") String token,
			HttpServletRequest request, 
			HttpServletResponse response
			) {
	    try {
	        Users user = this.userService.getUserByUUID(uuid);
	        if (user == null) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                                 .body(Map.of("message", "사용자를 찾을 수 없습니다."));
	        }
	        
	        // 1. Spring Security 로그아웃 처리 (세션 무효화 및 SecurityContext 전용)
	        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	        if (auth != null) {
	            new SecurityContextLogoutHandler().logout(request, response, auth);
	        }
	        
	        // 3. Redis 토큰 삭제
            if (token != null) {
                refreshTokenService.delete(token);
            }

	        // 3. JWT 쿠키 직접 삭제 (Security 설정에 있는 것과 동일한 이름 사용)
	        SecureUtil.deleteCookie(response, "JWT-TOKEN");
	        SecureUtil.deleteCookie(response, "REFRESH-TOKEN");
	        
	        
	        // 실제 삭제 대신 삭제 플래그만 변경 (Soft Delete)
	        this.userService.softDelete(user);
	        
	        // 성공 시 200 OK와 함께 메시지 전달
	        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다. 90일간 데이터를 보관합니다."));
	    } catch (Exception e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body(Map.of("message", "서버 오류가 발생했습니다."));
	    }
	}
}
