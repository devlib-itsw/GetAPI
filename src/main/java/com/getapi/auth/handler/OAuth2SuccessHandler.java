package com.getapi.auth.handler;

import com.getapi.auth.domain.RefreshToken;
import com.getapi.auth.service.RefreshTokenService;
import com.getapi.auth.service.SmsAuthService;
import com.getapi.auth.util.JwtUtil;
import com.getapi.auth.util.SecureUtil;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final RefreshTokenService refreshTokenService;
	private final JwtUtil jwtUtil;
	private final SmsAuthService smsAuthService;
	private final UserRepository userRepository;

	public OAuth2SuccessHandler(JwtUtil jwtUtil, SmsAuthService smsAuthService,
			RefreshTokenService refreshTokenService, UserRepository userRepository) {
		this.jwtUtil = jwtUtil;
		this.smsAuthService = smsAuthService;
		this.refreshTokenService = refreshTokenService;
		this.userRepository = userRepository;
	}

	private String extractDeviceRedirectCookie(HttpServletRequest request, HttpServletResponse response) {
		if (request.getCookies() == null) return null;
		return Arrays.stream(request.getCookies())
				.filter(c -> "_device_redirect".equals(c.getName()))
				.findFirst()
				.map(c -> {
					// 쿠키 삭제
					Cookie expired = new Cookie("_device_redirect", "");
					expired.setMaxAge(0);
					expired.setPath("/");
					response.addCookie(expired);
					return c.getValue();
				})
				.orElse(null);
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

		String email = oAuth2User.getAttribute("email");
		Boolean emailVerified = (Boolean) oAuth2User.getAttribute("email_verified");
		String name = oAuth2User.getAttribute("name");
		String picture = oAuth2User.getAttribute("picture");
		String sub = oAuth2User.getAttribute("sub");
		String secureToken = SecureUtil.generate64Token();

		if (emailVerified == null || !emailVerified) {
			getRedirectStrategy().sendRedirect(request, response, "/?error=email_not_verified");
			return;
		} else if (smsAuthService.existsBySub(sub)) {
			Users user = smsAuthService.updateProfile(sub, name, picture);
			if (user == null) {
				getRedirectStrategy().sendRedirect(request, response, "/?error=user_not_found");
				return;
			}
			String jwt = jwtUtil.generateToken(sub, user.getRole());

			Cookie cookie = new Cookie("JWT-TOKEN", jwt);
			cookie.setHttpOnly(true);
			cookie.setPath("/");
			cookie.setMaxAge(1800); // 30분
			response.addCookie(cookie);

			RefreshToken rt = refreshTokenService.save(sub, request.getRemoteAddr(), request.getHeader("User-Agent"), user.getRole());
			String token = rt.getToken(); // 쿠키에 넣을 값

			Cookie refreshCookie = new Cookie("REFRESH-TOKEN", token);
			refreshCookie.setHttpOnly(true);
			refreshCookie.setPath("/");
			refreshCookie.setMaxAge(2592000); // 30일
			response.addCookie(refreshCookie);

			String redirectTo = extractDeviceRedirectCookie(request, response);
			getRedirectStrategy().sendRedirect(request, response, redirectTo != null ? redirectTo : "/");
		} else {
			// 신규 유저: 번호인증 진행
			smsAuthService.saveToken(sub, secureToken, email, name, picture);
			getRedirectStrategy().sendRedirect(request, response, "/phoneVerify/" + secureToken);
		}
	}
}