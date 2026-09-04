package com.grimgate.grimgate_backend.domain.achievement.repository;

import com.grimgate.grimgate_backend.domain.achievement.entity.MemberAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberAchievementRepository extends JpaRepository<MemberAchievement, Long> {

    long countByMember_Id(Long memberId);

    @Query("SELECT ma FROM MemberAchievement ma JOIN FETCH ma.achievement WHERE ma.member.id = :memberId")
    List<MemberAchievement> findByMember_Id(@Param("memberId") Long memberId);
}
