package com.DevLib.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class User {
    private String name;
    private String phone;
    private String email;
    private String imageUrl;

    @Override
    public String toString() {
        return String.format("이름: %s | 번호: %s | 메일: %s | 이미지: %s",
                              name, phone, email, imageUrl);
    }
}