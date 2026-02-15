package com.getapi.global.config;

import com.getapi.auth.filter.JwtAuthenticationFilter;
import com.getapi.auth.handler.OAuth2SuccessHandler;
import com.getapi.auth.service.CustomOAuth2UserService;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler,
                         JwtAuthenticationFilter jwtAuthenticationFilter,
                         CustomOAuth2UserService customOAuth2UserService) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
//        	.csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
        	.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 세션 사용 안 함
            )
            .authorizeHttpRequests(auth -> auth
            	.requestMatchers("/css/**", "/js/**", "/images/**", "/static/**", "/favicon.ico").permitAll()
                .requestMatchers("/", "/login**", "/error**", "/oauth2/**", "/login/oauth2/**", "/phoneVerify/**").permitAll()
                .requestMatchers(PathRequest.toH2Console()).permitAll()
                .anyRequest().authenticated()
            ) 
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)  // DB role → 권한 매핑
                )
                .successHandler(oAuth2SuccessHandler)  // JWT 생성 핸들러
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .deleteCookies("JWT-TOKEN", "REFRESH-TOKEN")  // JWT + 리프레시 토큰 쿠키 삭제
                .permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);  // JWT 필터 추가
        
        return http.build();
    }
}