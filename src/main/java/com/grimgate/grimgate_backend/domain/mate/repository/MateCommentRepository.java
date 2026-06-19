package com.grimgate.grimgate_backend.domain.mate.repository;

import com.grimgate.grimgate_backend.domain.mate.entity.MateComment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 메이트 모집글 댓글 레포지토리 */
@Repository
public interface MateCommentRepository extends JpaRepository<MateComment, Long> {

    /**
     * 특정 모집글의 전체 댓글 조회 (삭제 포함).
     * 원댓글+대댓글 트리 조립 시 사용하며, 삭제 여부 필터링은 Service에서 처리한다.
     * N+1 방지를 위해 member, account 를 함께 fetch.
     */
    @EntityGraph(attributePaths = {"member", "member.account"})
    @Query("""
            select c from MateComment c
            where c.matePost.id = :postId
            order by c.createdAt asc
            """)
    List<MateComment> findAllByMatePostId(@Param("postId") Long postId);

    /**
     * 단건 조회 (수정/삭제/대댓글 작성 시 member 정보 함께 로드).
     * deletedAt 필터 없음 — 삭제 여부 판단은 호출부에서 처리.
     */
    @EntityGraph(attributePaths = {"member", "member.account"})
    @Query("select c from MateComment c where c.id = :id")
    Optional<MateComment> findWithMemberById(@Param("id") Long id);
}
