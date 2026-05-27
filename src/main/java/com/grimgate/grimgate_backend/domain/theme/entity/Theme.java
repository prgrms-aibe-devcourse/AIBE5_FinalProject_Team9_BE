package com.grimgate.grimgate_backend.domain.theme.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Builder
    public Theme(Long id, String title) {
        this.id = id;
        this.title = title;
    }
}
