package com.grimgate.grimgate_backend.domain.theme.repository;

import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 테마(Theme) 엔티티에 대한 데이터베이스 액세스 처리를 담당하는 Repository 인터페이스입니다.
 * 
 * [빠른예약 조회에서 이 Repository가 필요한 이유]
 * - 테스트 데이터를 구성하거나 특정 조건의 테마 상세 데이터를 관리할 때 테마 도메인의 영속성 제어를 담당합니다.
 */
@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
    List<Theme> findByBranchId(Long branchId);
    List<Theme> findByTagsContaining(String Tags);

    @Query(value = "SELECT * FROM theme WHERE deleted_at IS NULL ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Theme> findRandom(@Param("limit") int limit);

    List<Theme> findByHorrorLevel(Integer horrorLevel);
    List<Theme> findByHorrorLevelLessThanEqual(Integer horrorLevel);
    List<Theme> findByDifficultyGreaterThanEqual(Integer difficulty);
    List<Theme> findByDifficultyLessThanEqual(Integer difficulty);
    List<Theme> findByMinPeopleLessThanEqual(Integer minPeople);
    List<Theme> findByPlayTimeBetween(Integer start, Integer end);
}
