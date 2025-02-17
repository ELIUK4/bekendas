package com.galerija.repository;

import com.galerija.entity.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    @Query("SELECT i FROM Image i WHERE " +
           "LOWER(i.tags) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.type) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Image> searchImages(@Param("query") String query, Pageable pageable);
    
    List<Image> findByIdIn(List<Long> ids);
    
    Optional<Image> findByWebformatURL(String webformatURL);
    
    @Query("SELECT i FROM Image i WHERE i.id IN " +
           "(SELECT f.image.id FROM Favorite f WHERE f.user.id = :userId)")
    Page<Image> findUserFavorites(@Param("userId") Long userId, Pageable pageable);

    List<Image> findByUserId(Long userId);
}
