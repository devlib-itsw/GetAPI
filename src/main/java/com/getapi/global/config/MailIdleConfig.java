package com.getapi.global.config;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.getapi.auth.controller.SmsAuthController;
import com.getapi.auth.domain.SmsAuth;
import com.getapi.auth.service.SmsAuthService;

import lombok.RequiredArgsConstructor;


@Configuration
@EnableIntegration
@RequiredArgsConstructor
public class MailIdleConfig {
	private final SmsAuthService smsAuthService;
	private final SmsAuthController smsAuthController;

	@org.springframework.beans.factory.annotation.Value("${imap.username}")
	private String imapUsername;

	@org.springframework.beans.factory.annotation.Value("${imap.password}")
	private String imapPassword;

	@Bean
    public ThreadPoolTaskScheduler mailTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // 스레드 풀 설정
        scheduler.setThreadNamePrefix("mail-idle-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public MessageChannel gmailChannel() {
        return new DirectChannel();
    }

    @Bean
    public ImapMailReceiver imapMailReceiver(@Qualifier("mailTaskScheduler") ThreadPoolTaskScheduler taskScheduler) {
        String encodedUsername = imapUsername.replace("@", "%40");
        String url = "imaps://" + encodedUsername + ":" + imapPassword + "@imap.gmail.com:993/INBOX";

        ImapMailReceiver receiver = new ImapMailReceiver(url);
        receiver.setTaskScheduler(taskScheduler);
        receiver.setShouldMarkMessagesAsRead(true);
        receiver.setJavaMailProperties(javaMailProperties());
        receiver.setSimpleContent(true);

        return receiver;
    }

    @Bean
    public ImapIdleChannelAdapter mailAdapter(
            ImapMailReceiver imapMailReceiver,
            @Qualifier("mailTaskScheduler") ThreadPoolTaskScheduler taskScheduler) {

        ImapIdleChannelAdapter adapter = new ImapIdleChannelAdapter(imapMailReceiver);
        adapter.setOutputChannel(gmailChannel());
        adapter.setTaskScheduler(taskScheduler);
        adapter.setAutoStartup(true);

        return adapter;
    }

    private Properties javaMailProperties() {
        Properties props = new Properties();

        // ━━━━ SSL 설정 (인증서 정상 검증) ━━━━
        props.setProperty("mail.imaps.peek", "false");
        props.setProperty("mail.imap.ssl.enable", "true");

        props.setProperty("mail.imap.socketFactory.class",
            "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.imap.socketFactory.fallback", "false");

        // ━━━━ 프로토콜 ━━━━
        props.setProperty("mail.store.protocol", "imaps");

        // ━━━━ 타임아웃 ━━━━
        props.setProperty("mail.imap.connectiontimeout", "30000");
        props.setProperty("mail.imap.timeout", "30000");

        // ━━━━ 성능 ━━━━
        props.setProperty("mail.imap.partialfetch", "false");

        // ━━━━ 디버깅 ━━━━
        props.setProperty("mail.debug", "false");
        return props;
    }

    // 메일이 도착했을 때 실행될 메서드
    @ServiceActivator(inputChannel = "gmailChannel")
    public void autoRunMailProcess(Message<?> message) {
        // 1. 메시지 봉투에서 실제 메일(MimeMessage)을 꺼냅니다.
        jakarta.mail.internet.MimeMessage payload = (jakarta.mail.internet.MimeMessage) message.getPayload();

        // 2. Service에서 메일 인증 처리 (비즈니스 로직)
        SmsAuth verifiedUser = smsAuthService.processSmsAuth(payload);

        // 3. 인증 성공 시 JWT 생성 + SSE 알림 (HTTP 관심사)
        if (verifiedUser != null) {
            smsAuthController.notifyVerified(
                verifiedUser.getToken(),
                verifiedUser.getUserId(),
                "USER"
            );
        }
    }
}
