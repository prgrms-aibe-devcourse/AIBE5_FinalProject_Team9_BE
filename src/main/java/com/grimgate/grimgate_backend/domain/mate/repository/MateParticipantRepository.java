package com.grimgate.grimgate_backend.domain.mate.repository;

import com.grimgate.grimgate_backend.domain.mate.entity.MateParticipant;
import com.grimgate.grimgate_backend.domain.mate.entity.MateParticipantStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MateParticipantRepository extends JpaRepository<MateParticipant, Long> {

    /** 모집글-회원 조합으로 참여 row 조회 (있으면 status 무관) */
    Optional<MateParticipant> findByMatePost_IdAndMember_Id(Long matePostId, Long memberId);

    /** 활성 참여자 수 (작성자 제외 기준이 아니라 row 자체 카운트) */
    long countByMatePost_IdAndStatus(Long matePostId, MateParticipantStatus status);

    /** 특정 모집글의 활성 참여자 목록 — 참여자 카드/목록 응답용.
     *  nickname 접근 시 N+1 방지 위해 member.account 까지 fetch join. */
    @Query("""
            select p
              from MateParticipant p
              join fetch p.member m
              join fetch m.account a
             where p.matePost.id = :matePostId
               and p.status = :status
             order by p.joinedAt asc
            """)
    List<MateParticipant> findActiveByMatePostId(
            @Param("matePostId") Long matePostId,
            @Param("status") MateParticipantStatus status);

    /** 내가 참여한(또는 참여했던) 모집글 목록 — 마이페이지 등에서 활용.
     *  nickname 접근 시 N+1 방지 위해 member.account 까지 fetch join. */
    @Query("""
            select p
              from MateParticipant p
              join fetch p.matePost mp
              join fetch p.member m
              join fetch m.account a
             where p.member.id = :memberId
               and p.status = :status
             order by p.joinedAt desc
            """)
    List<MateParticipant> findMyParticipations(
            @Param("memberId") Long memberId,
            @Param("status") MateParticipantStatus status);

    /**
     * 모집글 soft delete 시 활성 참여자 일괄 제거 용도 — status 를 CANCELLED 로 전환 +
     * cancelled_at 기록. 타겟 참여자 수는 호출측에서 다시 조회하여 MatePost 의 currentPeople 동기화.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MateParticipant p
               set p.status = 'CANCELLED',
                   p.cancelledAt = :now
             where p.matePost.id = :matePostId
               and p.status = 'JOINED'
            """)
    int cancelAllJoinedByMatePostId(@Param("matePostId") Long matePostId,
                                    @Param("now") LocalDateTime now);
}
