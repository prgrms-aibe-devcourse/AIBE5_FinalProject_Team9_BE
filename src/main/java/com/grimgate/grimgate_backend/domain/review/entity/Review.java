package com.grimgate.grimgate_backend.domain.review.entity;


import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewCreateRequest;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewUpdateRequest;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false)

    private Integer rating;

    @Column(name = "horror_rating")
    private Integer horrorRating;

    @Column(name = "difficulty_rating")
    private Integer difficultyRating;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String tags;

    @Column(nullable = false)
    private String status;

    private Boolean spoiler = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "review", fetch = FetchType.LAZY)
    private List<ReviewImage> images = new ArrayList<>();

    public static Review create(Member member, Theme theme, Reservation reservation, ReviewCreateRequest request){
        Review review = new Review();
        review.member = member;
        review.theme = theme;
        review.reservation = reservation;
        review.rating = request.getRating();
        review.horrorRating = request.getHorrorRating();
        review.difficultyRating = request.getDifficultyRating();
        review.content = request.getContent();
        review.tags = request.getTags();
        review.content = request.getContent();
        Boolean spoiler = request.getSpoiler();
        review.spoiler = request.getSpoiler();
        review.status = "ACTIVE";
        return review;

    }

    /** 관리자 승인 시 후기 숨김 처리 */
    public void hide() {
        this.status = "HIDDEN";
    }

    /** 관리자 반려 시 후기 복구 처리 */
    public void restore() {
        this.status = "ACTIVE";
    }

    public void update(ReviewUpdateRequest request) {
        this.rating = request.getRating();
        this.horrorRating = request.getHorrorRating();
        this.difficultyRating = request.getDifficultyRating();
        this.content = request.getContent();
        this.tags = request.getTags();
        this.spoiler = request.getSpoiler();
    }
}
