package com.getapi.points.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // RestController가 아님에 주의!
@RequestMapping("/points")
public class PointsViewController {

    @GetMapping("/charge") // 충전 페이지 주소
    public String chargePage() {
        return "points"; // 실제 파일 위치: templates/points/points.html
    }

    @GetMapping("/withdraw") // 환전 페이지 주소
    public String withdrawPage() {
        return "points-withdraw"; // 실제 파일 위치: templates/points/points-withdraw.html
    }
}