package com.getapi.user.domain;

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
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users user;

    @Column(nullable = false)
    private String name;

    private String nickname;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    private String webUrl;

    @Column(nullable = false)
    private String profileImage;
    
	private boolean isCensored;

}
