package com.getapi.api.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.getapi.api.domain.Api;
import com.getapi.api.service.ApiService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/library")
public class ApiController {
	private final ApiService apiService;
	
	@GetMapping("")
	public String returnHtml(Model model) {
		
		return "library";
	}
	
	@GetMapping("/{id}")
	public String returnView(
		@PathVariable("id") UUID uuid,
		Model model
	) {
		Api api=this.apiService.getApi(uuid);
		model.addAttribute("api", api);
		
		return "library-view";
	}
}
