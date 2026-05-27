package com.grimgate.grimgate_backend.domain.theme.controller;
import com.grimgate.grimgate_backend.domain.theme.dto.BranchDetailResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeDetailResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeSearchCondition;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import com.grimgate.grimgate_backend.domain.theme.service.ThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ThemeController {

    private final ThemeService themeService;

    @GetMapping("/themes")
    public List<ThemeResponse> getThemes(
            ThemeSearchCondition condition
    ) {
        return themeService.getThemes(condition);
    }

    @GetMapping("/themes/{id}")
    public ThemeDetailResponse getThemeDetail(@PathVariable Long id) {
        return themeService.getThemeDetail(id);
    }

    @GetMapping("/branches/{id}")
    public BranchDetailResponse getBranches(@PathVariable Long id) {
        return themeService.getBranches(id);
    }


}
