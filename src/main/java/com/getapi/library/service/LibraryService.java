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
            
            // 검색어 가공
            String rawKeyword = req.getKeyword() != null ? req.getKeyword().trim() : "";
            String keywordLike = "%" + rawKeyword + "%";

            // [중요] 키워드가 있을 때만 필터링 조건을 추가합니다.
            if (!rawKeyword.isEmpty()) {
                List<String> filters = req.getFilters();
                List<Predicate> keywordPredicates = new ArrayList<>();

                // 필터가 '전체'이거나 비어있을 때
                if (filters == null || filters.isEmpty() || filters.contains("all")) {
                    keywordPredicates.add(cb.or(
                        cb.like(root.get("name"), keywordLike),
                        cb.like(root.get("description"), keywordLike),
                        cb.like(root.get("user").get("nickname"), keywordLike)
                    ));
                } 
                // 특정 필터가 체크되어 있을 때
                else {
                    if (filters.contains("title")) {
                        keywordPredicates.add(cb.like(root.get("name"), keywordLike));
                    }
                    if (filters.contains("content")) {
                        keywordPredicates.add(cb.like(root.get("description"), keywordLike));
                    }
                    if (filters.contains("user")) {
                        keywordPredicates.add(cb.like(root.get("user").get("nickname"), keywordLike));
                    }
                    if (filters.contains("hashtag")) {
                        Join<Api, ApiTagMapping> tagMappings = root.join("apiTagMappings", JoinType.LEFT);
                        keywordPredicates.add(cb.like(tagMappings.get("tag").get("tag"), keywordLike));
                        query.distinct(true);
                    }
                }

                if (!keywordPredicates.isEmpty()) {
                    predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
                }
            }

            // [핵심] 키워드가 없다면 predicates는 비어있게 되며, 
            // cb.and(empty)는 SQL에서 아무 조건이 없는 "SELECT * FROM api"와 같이 동작하여 
            // 초기 로딩 시 모든 데이터를 가져옵니다.
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