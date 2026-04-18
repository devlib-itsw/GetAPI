package com.getapi.library.service;

import com.getapi.api.domain.Api;
import com.getapi.api.domain.ApiTagMapping;
import com.getapi.library.librarydto.LibraryDto;
import com.getapi.library.librarydto.LibrarySearchRequest;
import com.getapi.library.repository.ApiRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.*;
import jakarta.persistence.criteria.Selection; // 필요 시
import java.util.function.Function; // 이 Function을 써야 합니다!

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final ApiRepository apiRepository;

    public Page<LibraryDto> getLibraryList(LibrarySearchRequest req) {
        // 1. 정렬 설정
        Sort sort = "popular".equals(req.getSort())
                ? Sort.by(Sort.Direction.DESC, "viewCount")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(req.getPage(), 6, sort);

        // 2. 동적 쿼리 생성
        Specification<Api> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 1. 검색어 가공 (공백 제거 + 소문자 변환)
            String rawKeyword = req.getKeyword() != null ? req.getKeyword().trim() : "";
            String sanitizedKeyword = rawKeyword.replaceAll("\\s+", "").toLowerCase(); 
            String keywordLike = "%" + sanitizedKeyword + "%";

            if (!rawKeyword.isEmpty()) {
                List<String> filters = req.getFilters();
                List<Predicate> keywordPredicates = new ArrayList<>();

                // [H2 전용 최적화] DB 컬럼의 공백을 지우고 소문자로 바꾼 뒤 비교
                java.util.function.Function<String, Expression<String>> normalize = (fieldName) -> {
                    // SQL: LOWER(REPLACE(field, ' ', ''))
                    Expression<String> replaced = cb.function("REPLACE", String.class, root.get(fieldName), cb.literal(" "), cb.literal(""));
                    return cb.lower(replaced);
                };

                if (filters == null || filters.isEmpty() || filters.contains("all")) {
                    keywordPredicates.add(cb.or(
                        cb.like(normalize.apply("name"), keywordLike),
                        cb.like(normalize.apply("description"), keywordLike),
                        cb.like(cb.lower(root.get("user").get("nickname")), "%" + rawKeyword.toLowerCase() + "%")
                    ));
                } else {
                    if (filters.contains("title")) {
                        keywordPredicates.add(cb.like(normalize.apply("name"), keywordLike));
                    }
                    if (filters.contains("content")) {
                        keywordPredicates.add(cb.like(normalize.apply("description"), keywordLike));
                    }
                    if (filters.contains("user")) {
                        keywordPredicates.add(cb.like(cb.lower(root.get("user").get("nickname")), "%" + rawKeyword.toLowerCase() + "%"));
                    }
                    if (filters.contains("hashtag")) {
                        Join<Api, ApiTagMapping> tagMappings = root.join("apiTagMappings", JoinType.LEFT);
                        keywordPredicates.add(cb.like(cb.lower(tagMappings.get("tag").get("tag")), "%" + rawKeyword.toLowerCase() + "%"));
                        query.distinct(true);
                    }
                }

                if (!keywordPredicates.isEmpty()) {
                    predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return apiRepository.findAll(spec, pageable).map(this::convertToDto);
    }

    private LibraryDto convertToDto(Api api) {
        List<String> tags = api.getApiTagMappings().stream()
                .map(ApiTagMapping::getTag)
                .filter(Objects::nonNull)
                .map(tag -> tag.getTag())
                .toList();

        return new LibraryDto(
        	    api.getApiUuid().toString(), // apiUuid
        	    api.getName(),               // title (화면의 item.title과 매칭)
        	    api.getDescription(),        // description
        	    api.getPrice().intValue(),   // price (Long을 int로 변환)
        	    api.getUser().getNickname(), // authorName
        	    api.getViewCount(),          // viewCount
        	    0L,                          // starCount (필요 시 로직 추가)
        	    tags                         // List<String>
        	);
    }
}