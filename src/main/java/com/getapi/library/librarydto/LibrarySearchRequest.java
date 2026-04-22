package com.getapi.library.librarydto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LibrarySearchRequest {
    private int page = 0;
    private String keyword;
    private List<String> filters;
    private String sort;
}