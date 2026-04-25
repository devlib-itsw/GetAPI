package com.getapi.auth.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.getapi.user.domain.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class ApiAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long authId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    @Column(unique = true)
    private String apiKey;

    private String secretKey;

    private String rotationToken;

    private LocalDate expiredDate;

    private LocalDateTime apiUpdatedAt;

    private LocalDateTime rotationTokenUpdatedAt;
}
