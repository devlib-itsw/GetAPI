package com.getapi.auth.service;

import org.springframework.stereotype.Service;

import com.getapi.auth.repository.RefreshTokenRepository;
import com.getapi.auth.util.SecureUtil;
import com.getapi.auth.domain.RefreshToken;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
	private final RefreshTokenRepository refreshTokenRepository;

	 public RefreshToken save(String sub, String ip, String userAgent) {
	    	return refreshTokenRepository.save(new RefreshToken(SecureUtil.generate64Token(), sub, ip, userAgent));
	    }

	 public RefreshToken rotate(String oldToken, String ip, String userAgent) {
		 RefreshToken old = refreshTokenRepository.findById(oldToken).orElse(null);
	      if (old == null) return null;

	      refreshTokenRepository.deleteById(oldToken);
	      return refreshTokenRepository.save(
	          new RefreshToken(SecureUtil.generate64Token(), old.getId(), ip, userAgent)
	      );
	    }

	 public void delete(String token) {
		 refreshTokenRepository.deleteById(token);
	    }

}
