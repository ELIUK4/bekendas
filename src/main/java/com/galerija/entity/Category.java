package com.galerija.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    // Pixabay API categories
    public static final String[] PIXABAY_CATEGORIES = {
            "backgrounds", "fashion", "nature", "science", "education", 
            "feelings", "health", "people", "religion", "places", 
            "animals", "industry", "computer", "food", "sports", 
            "transportation", "travel", "buildings", "business", "music"
    };
}
