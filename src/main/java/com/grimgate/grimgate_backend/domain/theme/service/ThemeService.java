package com.grimgate.grimgate_backend.domain.theme.service;

import com.grimgate.grimgate_backend.domain.theme.dto.BranchDetailResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeDetailResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeSearchCondition;
import com.grimgate.grimgate_backend.domain.theme.entity.Branch;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.BranchRepository;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final BranchRepository branchRepository;

    public List<ThemeResponse> getThemes(
            ThemeSearchCondition condition
    ){
        return themeRepository.findAll()
                .stream()
                //난이도 필터
                .filter(theme -> condition.getDifficulty() == null
                        || theme.getDifficulty().equals(condition.getDifficulty())
                )
                //지역 필터
                .filter(theme -> condition.getRegion() == null
                        || theme.getBranch().getRegion().equals(condition.getRegion())
                )

                // 최소 인원 필터
                .filter(theme ->
                        condition.getMin_people() == null
                                || theme.getMinPeople().equals( condition.getMin_people())
                )

                //최소 평점
                .filter(theme -> condition.getMin_rating()== null
                        || theme.getRating() >=condition.getMin_rating()
                )
                //공포도
                .filter(theme -> condition.getHorror_level() == null
                || theme.getHorrorLevel().equals(condition.getHorror_level()))

                .map(theme -> new ThemeResponse(
                        theme.getId(),
                        theme.getThumbnailUrl(),
                        theme.getBranch().getBranchName(),
                        theme.getTitle(),
                        theme.getDifficulty(),
                        theme.getHorrorLevel(),
                        theme.getRating(),
                        theme.getReviewCount(),
                        theme.getMinPeople(),
                        theme.getMaxPeople(),
                        theme.getPlayTime(),
                        theme.getDescription()
                ))
                .toList();
    }

    public ThemeDetailResponse getThemeDetail(Long id){
        Theme theme = themeRepository.findById(id).orElseThrow();

        return new ThemeDetailResponse(
                theme.getBranch().getBranchCode(),
                theme.getBranch().getBranchName(),
                theme.getBranch().getRegion(),
                theme.getBranch().getAddress(),
                theme.getBranch().getPhone(),
                theme.getBranch().getOperatingHours(),
                theme.getMinPeople(),
                theme.getMaxPeople(),
                theme.getDescription(),
                theme.getPlayTime()
        );
    }

    public BranchDetailResponse getBranches(Long id){
        Branch branch = branchRepository.findById(id).orElseThrow();
        return new BranchDetailResponse(
                branch.getBranchName(),
                branch.getRegion(),
                branch.getOperatingHours(),
                branch.getPhone(),
                branch.getAddress()


        );
    }

}
