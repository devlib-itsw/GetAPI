package com.getapi.library.service;

import com.getapi.api.domain.Api;
import com.getapi.api.domain.ApiTagMapping;
import com.getapi.api.repository.ApiRepository;
import com.getapi.api.repository.StarRepository;
import com.getapi.library.librarydto.LibraryDto;
import com.getapi.library.librarydto.LibrarySearchRequest;
import com.getapi.user.repository.UserProfileRepository;
import jakarta.persistence.criteria.Expression;
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
    private final UserProfileRepository userProfileRepository;
    private final StarRepository starRepository;

    public Page<LibraryDto> getLibraryList(LibrarySearchRequest req) {
        String sortKey = req.getSort() != null ? req.getSort() : "latest";

        // stars 정렬은 native query 사용
        if ("stars".equals(sortKey)) {
            Pageable pageable = PageRequest.of(req.getPage(), 6);
            return apiRepository.findAllOrderByStars(pageable).map(this::convertToDto);
        }

        Sort sort = switch (sortKey) {
            case "views"      -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "price-low"  -> Sort.by(Sort.Direction.ASC,  "price");
            case "price-high" -> Sort.by(Sort.Direction.DESC, "price");
            default           -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(req.getPage(), 6, sort);

        Specification<Api> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String rawKeyword = req.getKeyword() != null ? req.getKeyword().trim() : "";
            String sanitizedKeyword = rawKeyword.replaceAll("\\s+", "").toLowerCase();
            String keywordLike = "%" + sanitizedKeyword + "%";

            if (!rawKeyword.isEmpty()) {
                List<String> filters = req.getFilters();
                List<Predicate> kp = new ArrayList<>();

                java.util.function.Function<String, Expression<String>> normalize = fieldName -> {
                    Expression<String> replaced = cb.function("REPLACE", String.class,
                            root.get(fieldName), cb.literal(" "), cb.literal(""));
                    return cb.lower(replaced);
                };

                if (filters == null || filters.isEmpty() || filters.size() >= 4) {
                    kp.add(cb.or(
                            cb.like(normalize.apply("name"), keywordLike),
                            cb.like(normalize.apply("description"), keywordLike),
                            cb.like(cb.lower(root.get("user").get("userProfile").get("nickname")),
                                    "%" + rawKeyword.toLowerCase() + "%")
                    ));
                } else {
                    if (filters.contains("title"))
                        kp.add(cb.like(normalize.apply("name"), keywordLike));
                    if (filters.contains("content"))
                        kp.add(cb.like(normalize.apply("description"), keywordLike));
                    if (filters.contains("user"))
                        kp.add(cb.like(cb.lower(root.get("user").get("userProfile").get("nickname")),
                                "%" + rawKeyword.toLowerCase() + "%"));
                    if (filters.contains("hashtag")) {
                        Join<Api, ApiTagMapping> tagMappings = root.join("apiTagMappings", JoinType.LEFT);
                        kp.add(cb.and(
                            cb.isFalse(tagMappings.get("tag").get("isCensored")),
                            cb.like(cb.lower(tagMappings.get("tag").get("tag")),
                                "%" + rawKeyword.toLowerCase() + "%")
                        ));
                        query.distinct(true);
                    }
                }

                if (!kp.isEmpty()) predicates.add(cb.or(kp.toArray(new Predicate[0])));
            }
            predicates.add(cb.isFalse(root.get("isCensored")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return apiRepository.findAll(spec, pageable).map(this::convertToDto);
    }

    private LibraryDto convertToDto(Api api) {
        List<String> tags = api.getApiTagMappings().stream()
                .map(ApiTagMapping::getTag)
                .filter(t -> t != null && !t.isCensored())
                .map(tag -> tag.getTag())
                .toList();

        String authorName = userProfileRepository.findByUser(api.getUser())
                .map(p -> p.getNickname() != null ? p.getNickname() : p.getName())
                .orElse("알 수 없음");

        long starCount = starRepository.countByApi(api);

        return new LibraryDto(
                api.getApiUuid().toString(),
                api.getName(),
                api.getDescription(),
                api.getPrice().intValue(),
                authorName,
                api.getViewCount(),
                starCount,
                tags
        );
    }
}
