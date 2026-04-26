package com.getapi.library.libraryController;

import com.getapi.api.domain.Api;
import com.getapi.api.domain.ApiTagMapping;
import com.getapi.api.service.ApiService;
import com.getapi.api.service.StarService;
import com.getapi.comment.domain.ApiComment;
import com.getapi.comment.service.ApiCommentService;
import com.getapi.user.domain.Users;
import com.getapi.user.repository.UserProfileRepository;
import com.getapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class LibraryController {

    private final ApiService apiService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final StarService starService;
    private final ApiCommentService apiCommentService;

    @GetMapping("library")
    public String viewLibrary() {
        return "library";
    }

    @GetMapping("library/{uuid}/star")
    public String toggleStar(@PathVariable("uuid") String uuid) {
        String sub = SecurityContextHolder.getContext().getAuthentication().getName();
        if (sub == null || "anonymousUser".equals(sub)) return "redirect:/";

        Users user = userRepository.findByProviderId(sub);
        if (user == null) return "redirect:/";

        Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
        if (apiOpt.isEmpty()) return "redirect:/library";

        starService.toggle(apiOpt.get(), user);
        return "redirect:/library/view/" + uuid;
    }

    @GetMapping("library/view/{uuid}")
    public String viewLibraryView(@PathVariable("uuid") String uuid, Model model) {
        Optional<Api> apiOpt = apiService.findByUuid(UUID.fromString(uuid));
        if (apiOpt.isEmpty()) return "redirect:/library";

        Api api = apiOpt.get();
        apiService.incrementViewCount(api);

        List<String> tags = api.getApiTagMappings().stream()
                .map(ApiTagMapping::getTag)
                .filter(t -> t != null && !t.isCensored())
                .map(t -> t.getTag())
                .toList();

        List<ApiComment> comments = apiCommentService.findByApi(api);
        long starCount = starService.countByApi(api);
        long commentCount = apiCommentService.countByApi(api);

        String authorName = userProfileRepository.findByUser(api.getUser())
                .map(p -> p.getNickname() != null ? p.getNickname() : p.getName())
                .orElse("알 수 없음");
        String authorUuid = api.getUser().getUserUuid().toString();

        boolean starred = false;
        try {
            String sub = SecurityContextHolder.getContext().getAuthentication().getName();
            if (sub != null && !"anonymousUser".equals(sub)) {
                Users currentUser = userRepository.findByProviderId(sub);
                if (currentUser != null) starred = starService.isStarred(api, currentUser);
            }
        } catch (Exception ignored) {}

        model.addAttribute("api", api);
        model.addAttribute("uuid", uuid);
        model.addAttribute("tags", tags);
        model.addAttribute("comments", comments);
        model.addAttribute("starCount", starCount);
        model.addAttribute("commentCount", commentCount);
        model.addAttribute("starred", starred);
        model.addAttribute("authorName", authorName);
        model.addAttribute("authorUuid", authorUuid);
        return "library-view";
    }
}
