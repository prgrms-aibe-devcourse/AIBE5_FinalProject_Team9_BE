package com.grimgate.grimgate_backend.domain.mate.entity;

/**
 * 모집글 경험 레벨.
 *
 * 명세서(MT-008) 기준:
 * - ANY           : 무관
 * - BEGINNER      : 입문 (1~3회)
 * - INTERMEDIATE  : 중급
 * - EXPERT        : 고수 (30회+)
 */
public enum ExperienceLevel {
    ANY,
    BEGINNER,
    INTERMEDIATE,
    EXPERT
}
