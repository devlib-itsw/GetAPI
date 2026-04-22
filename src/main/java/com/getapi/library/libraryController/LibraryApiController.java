package com.getapi.library.libraryController;

import com.getapi.library.librarydto.LibraryDto;
import com.getapi.library.librarydto.LibrarySearchRequest;
import com.getapi.library.service.LibraryService;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/library")
public class LibraryApiController {

    private final LibraryService libraryService;

    @PostMapping("/list")
    public ResponseEntity<Page<LibraryDto>> list(@RequestBody LibrarySearchRequest req) {
        return ResponseEntity.ok(libraryService.getLibraryList(req));
    }
}