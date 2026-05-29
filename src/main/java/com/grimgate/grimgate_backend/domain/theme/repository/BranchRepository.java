package com.grimgate.grimgate_backend.domain.theme.repository;

import com.grimgate.grimgate_backend.domain.theme.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 지점(Branch) 엔티티에 대한 데이터베이스 액세스 처리를 담당하는 Repository 인터페이스입니다.
 * 
 * [빠른예약 조회에서 이 Repository가 필요한 이유]
 * - 테스트 데이터 적재 및 개별 지점 정보 관리를 위해 지점 도메인의 영속성 제어를 담당할 기본 Repository가 필요합니다.
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
}
