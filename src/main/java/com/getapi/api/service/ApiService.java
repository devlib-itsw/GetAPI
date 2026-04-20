package com.getapi.api.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getapi.admin.domain.AdminCensoredResponse;
import com.getapi.api.domain.Api;
import com.getapi.api.repository.ApiRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiService {
	private final ApiRepository apiRepository;
	
	public Api getApi(UUID uuid) {
		return this.apiRepository.findByApiUuid(uuid);
	}
	
	public Page<AdminCensoredResponse> getApisByIsCensoredPage(int page){
		Pageable pageable=PageRequest.of(page, 10, Sort.by("apiId").descending());
		
		Page<Api> list=this.apiRepository.findByIsCensoredTrue(pageable);
		
		Page<AdminCensoredResponse> dtolist=list.map(api->new AdminCensoredResponse(
			api.getApiId(),
			api.getName(),
			api.getDescription(),
			api.getApiUuid().toString(),
			api.getUpdatedAt(),
			api.getUser()
		));
		
		return dtolist;
	}
	
	@Transactional
	public void ignore(UUID uuid) {
		Api api=this.apiRepository.findByApiUuid(uuid);
		if(api!=null) {
			api.setCensored(false);
		}
	}
	
	@Transactional
	public void delete(UUID uuid) {
		this.apiRepository.deleteByApiUuidAndIsCensoredTrue(uuid);
	}
}
