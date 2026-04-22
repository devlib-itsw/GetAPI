package com.getapi.library.libraryController;



import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller // 1. 이게 반드시 있어야 스프링이 인식합니다!

public class LibraryController {

    // 결과 주소: http://localhost:8080/library
    @GetMapping("library") 
    public String viewLibrary() {
        return "library"; 
    }

    // 결과 주소: http://localhost:8080/library/view/123
    @GetMapping("library/view/{uuid}")
    public String viewLibraryView(@PathVariable("uuid") String uuid, Model model) {
        model.addAttribute("uuid", uuid);
        return "library-view";
    }
}