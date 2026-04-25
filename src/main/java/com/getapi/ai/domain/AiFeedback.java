package com.getapi.ai.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class AiFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false)
    private String text;

    private String contentType;   // "post", "postComment", "apiComment"
    private String contentUuid;

    private String detectedLabels; // comma-separated labels from AI (e.g. "여성/가족,남성")
    private String adminLabel;     // "clean" (admin ignored) or "confirmed" (admin deleted = AI was right)

    private LocalDateTime createdAt = LocalDateTime.now();

    public AiFeedback(String text, String contentType, String contentUuid,
                      String detectedLabels, String adminLabel) {
        this.text = text;
        this.contentType = contentType;
        this.contentUuid = contentUuid;
        this.detectedLabels = detectedLabels;
        this.adminLabel = adminLabel;
        this.createdAt = LocalDateTime.now();
    }
}
