package com.getapi.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.getapi.user.service.ProfileImgService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class ProfileController {
	private final ProfileImgService profileImgService;

	  @GetMapping("/avatar/{userId}")
	  public ResponseEntity<byte[]> getAvatar(@PathVariable("userId") Long userId) {
	      byte[] image = profileImgService.getOrFetch(userId);
	      return ResponseEntity.ok()
	          .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
	          .contentType(MediaType.IMAGE_JPEG)
	          .body(image);
	  }
}
