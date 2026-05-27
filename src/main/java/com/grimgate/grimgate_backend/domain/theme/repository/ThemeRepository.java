package com.grimgate.grimgate_backend.domain.theme.repository;

import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
}
