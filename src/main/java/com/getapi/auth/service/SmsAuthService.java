package com.getapi.auth.service;

import com.getapi.auth.domain.SmsAuth;
import com.getapi.user.domain.UserProfile;
import com.getapi.user.domain.Users;
import com.getapi.auth.repository.SmsAuthRepository;
import com.getapi.user.repository.UserProfileRepository;
import com.getapi.user.repository.UserRepository;
import com.getapi.auth.util.ImapUtil;
import com.getapi.auth.util.SecureUtil;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsAuthService {
    private final SmsAuthRepository smsAuthRepository;
    private final UserRepository UserRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public void saveUser(String id, String name, String phone, String mail, String profileImg) {
        Users user = new Users(null, UUID.randomUUID(), id, mail, phone, "ROLE_USER", LocalDateTime.now(), null);
        UserRepository.save(user);
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setName(name);
        profile.setProfileImage(profileImg != null ? profileImg : "");
        userProfileRepository.save(profile);
    }

    public void updateProfile(String sub, String name, String picture) {
        Users user = UserRepository.findByProviderId(sub);
        if (user == null) return;
        UserProfile profile = userProfileRepository.findByUser(user).orElse(new UserProfile());
        profile.setUser(user);
        profile.setName(name);
        if (picture != null) profile.setProfileImage(picture);
        userProfileRepository.save(profile);
    }

    public void saveToken(String id, String token, String userEmail, String userName, String userImg) {
        SmsAuth entity = new SmsAuth(token, id, userEmail, userName, userImg, null);
        smsAuthRepository.save(entity);
    }
    
    public void updateToken(String token, String phone) {
	    SmsAuth user = smsAuthRepository.findById(token)
	            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 토큰입니다. 수정됨"));

	    // 2. 찾아온 객체에 전화번호 설정 (데이터 업데이트)
	    user.setPhone(phone);

	    // 3. Redis에 다시 저장 (동일 Id이므로 덮어쓰기)
	    smsAuthRepository.save(user);
    }
    
    public SmsAuth findToken(String token) {
    	SmsAuth user = smsAuthRepository.findById(token)
    			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유효하지 않은 토큰입니다. 수정됨2222"));
    	
    	return user;
    }

    public boolean checkVerification(String token) {
        return smsAuthRepository.existsById(token); // 존재 여부 반환
    }

    public void deleteToken(String token) {
        smsAuthRepository.deleteById(token);
    }

    public boolean existsBySub(String sub) {
        return UserRepository.existsByProviderId(sub);
    }

    public List<Users> getAllUsers() {
        return UserRepository.findAll();
    }

    /**
     * IMAP으로 수신된 메일을 처리하여 SMS 인증을 검증합니다.
     * @return 인증 성공 시 토큰 문자열, 실패 시 null
     */
    public SmsAuth processSmsAuth(MimeMessage mimeMessage) {
        try {
            // 1. 발신자 정보 가져오기 (예: 01062735002 <01062735002@vmms.nate.com>)
            String from = mimeMessage.getFrom()[0].toString();

            // 2. 발신자 이메일 주소 추출 (<> 사이 추출 및 @ 분리)
            Pattern senderPattern = Pattern.compile("<(.*?)>");
            Matcher senderMatcher = senderPattern.matcher(from);

            String phone = "";
            String domain = "";

            if (senderMatcher.find()) {
                String fullEmail = senderMatcher.group(1);
                String[] parts = fullEmail.split("@");
                if (parts.length == 2) {
                    phone = parts[0];
                    domain = parts[1];
                }
            } else {
                int atIndex = from.indexOf("@");
                if (atIndex != -1) {
                    phone = from.substring(0, atIndex);
                    domain = from.substring(atIndex + 1);
                }
            }

            // SPF 결과 확인
            String[] spfHeaders = mimeMessage.getHeader("Received-SPF");
            if (spfHeaders != null) {
                String spf = spfHeaders[0];
                log.info("SPF 결과: {}", spf);

                if (!spf.toLowerCase().startsWith("pass")) {
                    log.warn("SPF 인증 실패 - 스푸핑 가능성: {}", spf);
                    return null;
                }
            }
            
            if(!domain.equals("vmms.nate.com") && !domain.equals("mmsmail.uplus.co.kr")) {
            	log.warn("도메인 인증 실패 - 위조 가능성: {}", domain);
                return null;
            }

            // Authentication-Results 확인
            String[] authHeaders = mimeMessage.getHeader("Authentication-Results");
            if (authHeaders != null) {
                log.info("인증 결과: {}", authHeaders[0]);
            }

            // 3. 메일 본문 추출 및 토큰 파싱
            String content = ImapUtil.getTextFromMessage(mimeMessage);

            // 본문 예: 수정하지말고 그대로 전송해주세요.
            Pattern bodyPattern = Pattern.compile("\\((.*?)\\)");
            Matcher bodyMatcher = bodyPattern.matcher(content);

            if (bodyMatcher.find()) {
                String contentResult = bodyMatcher.group(1);
                SmsAuth user;

                try {
                    user = findToken(contentResult);
                } catch (Exception e) {
                    log.error("토큰 조회 실패 (만료 또는 미존재): {}", contentResult);
                    return null;
                }

                String getPhone = user.getPhone();

                // null-safe 비교: phone은 빈 문자열일 수 있어도 null은 아님
                if (phone.equals(getPhone)) {
                    saveUser(user.getUserId(), user.getUserName(), getPhone, user.getUserEmail(), user.getUserImg());
                    deleteToken(contentResult);
                    log.info("인증 성공 - 번호: {}, 도메인: {}, 토큰: {}", phone, domain, contentResult);
                    return user;
                } else {
                    log.warn("인증 전화번호 불일치. Redis: {}, 발신: {}", getPhone, phone);
                }
            }

        } catch (Exception e) {
            log.error("메일 인증 처리 중 에러 발생", e);
        }
        return null;
    }
}