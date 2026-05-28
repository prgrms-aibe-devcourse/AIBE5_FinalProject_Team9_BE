package com.grimgate.grimgate_backend.domain.theme.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id",  nullable = false)
    private Branch branch;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;
    private String tags;

    @Column(nullable = false)
    private Integer horrorLevel;

    @Column(nullable = false)
    private Integer difficulty;

    @Column(nullable = false)
    private Integer ageLimit;

    @Column(nullable = false)
    private Integer playTime;

    @Column(nullable = false)
    private Integer minPeople;

    @Column(nullable = false)
    private Integer maxPeople;

    @Column(nullable = false)
    private Integer price;
    private Double rating;
    private Integer reviewCount;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    @Builder
    public Theme(Long id,
                 String title,
                 String description,
                 String tags,
                 Integer horrorLevel,
                 Integer difficulty,
                 Integer ageLimit,
                 Integer playTime,
                 Integer minPeople,
                 Integer maxPeople,
                 Integer price,
                 Double rating,
                 Integer reviewCount,
                 String thumbnailUrl,
                 Branch branch) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.horrorLevel = horrorLevel;
        this.difficulty = difficulty;
        this.ageLimit = ageLimit;
        this.playTime = playTime;
        this.minPeople = minPeople;
        this.maxPeople = maxPeople;
        this.price = price;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.thumbnailUrl = thumbnailUrl;
        this.branch = branch;
    }

}
