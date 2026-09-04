package com.grimgate.grimgate_backend.domain.review.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    private String imageUrl;
    @NotNull
    private String imageOrder;


    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
