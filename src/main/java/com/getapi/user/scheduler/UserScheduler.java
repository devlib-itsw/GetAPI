package com.getapi.user.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.getapi.auth.service.ApiAuthService;
import com.getapi.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserScheduler {
	private final UserService userService;
	private final ApiAuthService apiAuthService;

	@Scheduled(cron="0 0 0 * * *")
	public void hardDelete() {
		this.userService.hardDelete();
	}

	@Scheduled(cron="0 0 0 * * *")
	public void issueApiKey() {
		apiAuthService.issueApiKey(apiAuthService.getUsersBeforeApiUpdatedAt());
	}
}
