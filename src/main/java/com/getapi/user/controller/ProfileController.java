package com.getapi.user.controller;

import java.util.UUID;

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

	  @GetMapping("/avatar/{uuid}")
	  public ResponseEntity<byte[]> getAvatar(@PathVariable("uuid") UUID uuid) {
	      byte[] image = profileImgService.getOrFetch(uuid);
	      if (image == null) return ResponseEntity.notFound().build();
	      return ResponseEntity.ok()
	          .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
	          .contentType(MediaType.IMAGE_JPEG)
	          .body(image);
	  }
}
