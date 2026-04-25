package com.getapi.proxy.domain;

import com.getapi.api.domain.Api;
import com.getapi.user.domain.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_calllog_api_called_at", columnList = "api_id, calledAt"),
        @Index(name = "idx_calllog_called_at", columnList = "calledAt")
})
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private Api api;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users calledUser;

    @Column(nullable = false)
    private int statusCode;

    @Column(nullable = false)
    private long responseTimeMs;
    
    @Column(nullable = false)
    private String httpMethod;

    @Column(nullable = false)
    private Long pointsCharged;

    @Column(nullable = false)
    private LocalDateTime calledAt;
}
