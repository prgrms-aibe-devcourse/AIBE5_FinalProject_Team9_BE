package com.grimgate.grimgate_backend.domain.mate.repository;

import com.grimgate.grimgate_backend.domain.mate.entity.MatePost;
import com.grimgate.grimgate_backend.domain.mate.entity.MatePostStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MatePostRepository
        extends JpaRepository<MatePost, Long>, JpaSpecificationExecutor<MatePost> {

    /**
     * 동적 필터 목록 조회 시 Theme → Branch 까지 한 번에 로딩.
     * MatePostResponse 에 storeName/branchName/region/address 를 노출하므로 N+1 방지
     * 을 위해 EntityGraph 적용. (member 는 작성자 닉네임 노출에도 필요)
     */
    @Override
    @EntityGraph(attributePaths = {"member", "member.account", "theme", "theme.branch"})
    Page<MatePost> findAll(Specification<MatePost> spec, Pageable pageable);

    /**
     * 상세 조회용 단건 조회 (member/theme/branch 까지 로딩).
     * 비관적 락이 필요한 findByIdForUpdate 와는 용도가 다르다.
     */
    @EntityGraph(attributePaths = {"member", "member.account", "theme", "theme.branch"})
    @Query("select p from MatePost p where p.id = :id")
    Optional<MatePost> findDetailById(@Param("id") Long id);

    /** soft delete 되지 않은 글만 조회 */
    long countByStatusAndDeletedAtIsNull(MatePostStatus status);

    long countByStatus(MatePostStatus status);

    long countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(LocalDateTime since);

    Page<MatePost> findByMemberIdAndDeletedAtIsNull(Long memberId, Pageable pageable);

    /**
     * 참여/취소/강퇴 시 currentPeople 갱신을 직렬화하기 위한 비관적 쓰기 락 조회.
     * 동시 참여 요청에서 정원 초과 / lock acquisition 충돌을 방지한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MatePost p where p.id = :id")
    Optional<MatePost> findByIdForUpdate(@Param("id") Long id);

    /**
     * 동적 필터 Specification.
     *
     * @param keyword           제목/내용 LIKE 검색 (null/blank 시 무시)
     * @param status            상태 (null 시 무시, DELETED는 항상 제외)
     * @param themeId           테마 ID (null 시 무시)
     * @param experienceLevel   경험 레벨 문자열 (null/blank 시 무시)
     * @param authorMemberId    작성자 (null 시 무시) — '내글' 탭에서 사용
     */
    static Specification<MatePost> withFilter(String keyword,
                                              MatePostStatus status,
                                              Long themeId,
                                              String experienceLevel,
                                              Long authorMemberId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // soft delete 제외
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.notEqual(root.get("status"), MatePostStatus.DELETED));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (themeId != null) {
                predicates.add(cb.equal(root.get("theme").get("id"), themeId));
            }

            if (experienceLevel != null && !experienceLevel.isBlank()) {
                predicates.add(cb.equal(
                        cb.literal(experienceLevel),
                        cb.function("CAST", String.class, root.get("experienceLevel"))
                ));
            }

            if (authorMemberId != null) {
                predicates.add(cb.equal(root.get("member").get("id"), authorMemberId));
            }

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("content")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
