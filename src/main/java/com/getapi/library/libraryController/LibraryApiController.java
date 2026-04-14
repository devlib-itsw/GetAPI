package com.getapi.library.libraryController;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.getapi.library.librarydto.LibraryDto;
import com.getapi.library.librarydto.LibrarySearchRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/library")
public class LibraryApiController {

    @PostMapping("/list")
    public ResponseEntity<Page<LibraryDto>> list(@RequestBody LibrarySearchRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), 6);

        List<LibraryDto> items = List.of(
                new LibraryDto(1L, "Translation API", "번역 API입니다.", 1, "김개발", 15420, 342, List.of("번역", "다국어", "NLP")),
                new LibraryDto(2L, "Image Resize API", "이미지 리사이즈 API입니다.", 2, "이코딩", 8932, 198, List.of("이미지", "미디어", "변환")),
                new LibraryDto(3L, "Weather Data API", "날씨 데이터 API입니다.", 1, "박데이터", 12876, 276, List.of("날씨", "데이터", "실시간")),
                new LibraryDto(4L, "OCR Recognition API", "OCR API입니다.", 3, "최인식", 9543, 231, List.of("OCR", "AI", "텍스트")),
                new LibraryDto(5L, "Payment Gateway API", "결제 API입니다.", 5, "정페이", 7821, 189, List.of("결제", "금융", "보안")),
                new LibraryDto(6L, "Sentiment Analysis API", "감정 분석 API입니다.", 2, "한감정", 6234, 156, List.of("AI", "NLP", "분석")),
                new LibraryDto(7L, "Map Route API", "경로 탐색 API입니다.", 2, "지도맨", 4123, 88, List.of("지도", "경로", "위치")),
                new LibraryDto(8L, "Speech To Text API", "음성을 텍스트로 변환합니다.", 4, "홍개발", 3210, 77, List.of("음성", "AI", "변환"))
        );

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<LibraryDto> pageContent = start >= items.size() ? List.of() : items.subList(start, end);

        Page<LibraryDto> result = new PageImpl<>(pageContent, pageable, items.size());

        return ResponseEntity.ok(result);
    }
}