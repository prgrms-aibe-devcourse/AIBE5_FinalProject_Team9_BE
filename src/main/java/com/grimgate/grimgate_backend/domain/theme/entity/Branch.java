package com.grimgate.grimgate_backend.domain.theme.entity;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long managerId;

    @Column(nullable = false)
    private String branchCode;

    @Column(nullable = false)
    private String branchName;
    private String region;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String phone;
    private String operatingHours;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
