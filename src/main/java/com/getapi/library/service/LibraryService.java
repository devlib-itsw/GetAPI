package com.getapi.library.service;

import com.getapi.api.domain.Api;
import com.getapi.api.domain.ApiTagMapping;
import com.getapi.library.librarydto.LibraryDto;
import com.getapi.library.librarydto.LibrarySearchRequest;
import com.getapi.library.repository.ApiRepository;
import jakarta.persistence.criteria.Join;
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
        Sort sort = "popular".equals(req.getSort())
                ? Sort.by(Sort.Direction.DESC, "viewCount")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = PageRequest.of(req.getPage(), 6, sort);

        Specification<Api> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) {
                String keyword = "%" + req.getKeyword().trim() + "%";
                predicates.add(
                        cb.or(
                                cb.like(root.get("name"), keyword),
                                cb.like(root.get("description"), keyword)
                        )
                );
            }

            if (req.getFilters() != null && !req.getFilters().isEmpty()) {
                Join<Api, ApiTagMapping> tagMappings = root.join("apiTagMappings");
                predicates.add(tagMappings.get("tag").get("tag").in(req.getFilters()));
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return apiRepository.findAll(spec, pageable)
                .map(this::convertToDto);
    }

    private LibraryDto convertToDto(Api api) {
        List<String> tags = api.getApiTagMappings().stream()
                .map(ApiTagMapping::getTag)
                .filter(Objects::nonNull)
                .map(tag -> tag.getTag())
                .toList();

        return new LibraryDto(
                api.getApiId(),
                api.getName(),
                api.getDescription(),
                api.getPrice().intValue(),
                api.getUser().getNickname(),
                api.getViewCount(),
                0L,
                tags
        );
    }
}