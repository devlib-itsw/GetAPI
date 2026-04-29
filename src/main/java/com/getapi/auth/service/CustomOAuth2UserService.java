package com.getapi.auth.service;

import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	private final UserRepository userRepository;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest request) {
		OAuth2User oAuth2User = super.loadUser(request);

		String sub = oAuth2User.getAttribute("sub");
		Users user = userRepository.findByProviderId(sub);

		if (user != null) {
			if (user.getProfileDeletedAt() != null) {
				user.setProfileDeletedAt(null);
				userRepository.save(user);
			}
			List<GrantedAuthority> authorities = List.of(
				new SimpleGrantedAuthority(user.getRole())
			);
			return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "sub");
		}

		return oAuth2User;
	}
}
