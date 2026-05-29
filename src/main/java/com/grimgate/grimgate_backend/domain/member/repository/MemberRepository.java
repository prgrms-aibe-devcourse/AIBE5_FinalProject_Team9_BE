package com.grimgate.grimgate_backend.domain.member.repository;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByAccount(Account account);

    Optional<Member> findByAccount_Id(Long accountId);
}
