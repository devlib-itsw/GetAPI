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
	
    @GetMapping("/points")
    public String pointsPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && !auth.getPrincipal().equals("anonymousUser")) {
            String email;
            if (auth.getPrincipal() instanceof OAuth2User) {
                email = ((OAuth2User) auth.getPrincipal()).getAttribute("email");
            } else {
                email = auth.getName(); 
            }

            // DB에서 포인트가 갱신된 최신 유저 정보 조회
            Users user = userRepository.findByProviderId(email);
            
            if (user != null) {
                // HTML에서 ${user.point}로 쓸 수 있게 담아줍니다.
                model.addAttribute("user", user);
            }
        }
        return "points"; // points.html 렌더링
    }
    
    
	@GetMapping("/mypage")
	public String returnHtml(@AuthenticationPrincipal OAuth2User oAuth2User,Model model) {
		
		// 1. 현재 로그인된 유저의 식별값(이메일)을 직접 가져옵니다. 2026-3-18일
	    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    
	    // 로그인이 안 된 경우 처리
	    if (auth == null || auth.getPrincipal().equals("anonymousUser")) {
	        return "redirect:/login";
	    }

	    // 2. 이메일 추출 (JWT 필터가 넣은 값이 문자열이든 객체든 대응 가능)
	    String email;
	    if (auth.getPrincipal() instanceof OAuth2User) {
	        email = ((OAuth2User) auth.getPrincipal()).getAttribute("email");
	    } else {
	        email = auth.getName(); // JWT 필터 등을 거쳤을 때 보통 이메일이 담깁니다.
	    }

	    // 3. DB에서 유저 정보 조회
	    Users user = userRepository.findByEmail(email);
	    
	    if (user != null) {
	        // HTML에서 사용하는 이름인 "user"로 담아줍니다.
	        model.addAttribute("user", user);
	    }
        //2026-3-18일
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
