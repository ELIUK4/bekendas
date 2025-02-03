package com.galerija.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "search_history")
public class SearchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String searchQuery;

    private String filters; // JSON string containing applied filters

    @Column(name = "search_date")
    private java.time.LocalDateTime searchDate;

    private Integer resultsCount;

    @PrePersist
    protected void onCreate() {
        searchDate = java.time.LocalDateTime.now();
    }
}
