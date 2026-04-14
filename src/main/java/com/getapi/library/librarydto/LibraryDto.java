package com.getapi.library.librarydto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LibraryDto {
    private Long id;
    private String title;
    private String description;
    private int price;
    private String authorName;
    private long viewCount;
    private long starCount;
    private List<String> tags;
}