package com.grimgate.grimgate_backend.domain.title.repository;

import com.grimgate.grimgate_backend.domain.title.entity.Title;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TitleRepository extends JpaRepository<Title, Long> {
}
