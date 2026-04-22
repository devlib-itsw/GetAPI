package com.getapi.post.controller;

import java.util.Arrays; // 1. Arrays import 추가 필요
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WriteForm {
    @NotEmpty(message="제목을 입력해주세요.")
    @Size(max=200)
    private String title;
    
    @NotEmpty(message="내용을 입력해주세요.")
    private String content;
    
    // HTML의 <input name="tags"> 로부터 들어오는 문자열 데이터
    @NotEmpty(message="태그를 입력해주세요.")
    private String tags;
    
    /**
     * 문자열로 된 tags를 리스트로 변환하여 반환
     */
    public List<String> getTagList() {
        if (this.tags == null || this.tags.isBlank()) { // isEmpty보다 isBlank가 공백 문자열 체크에 더 안전함
            return Collections.emptyList();
        }
        return Arrays.stream(this.tags.split(","))
                     .map(String::trim) 
                     .filter(tag -> !tag.isEmpty()) 
                     .collect(Collectors.toList());
    }
}