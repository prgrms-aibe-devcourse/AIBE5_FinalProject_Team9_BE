package com.grimgate.grimgate_backend.domain.member.repository;

import com.grimgate.grimgate_backend.domain.member.entity.ProfileCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileCharacterRepository extends JpaRepository<ProfileCharacter, Long> {

    Optional<ProfileCharacter> findFirstByType(String type);
}
