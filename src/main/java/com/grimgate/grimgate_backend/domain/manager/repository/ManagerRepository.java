package com.grimgate.grimgate_backend.domain.manager.repository;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import com.grimgate.grimgate_backend.domain.manager.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

    Optional<Manager> findByAccount(Account account);

    Optional<Manager> findByAccount_Id(Long accountId);
}
