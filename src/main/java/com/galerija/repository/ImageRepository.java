package com.galerija.repository;

import com.galerija.entity.Image;
import com.galerija.entity.ImagePrivacy;
import com.galerija.entity.UserEntity;
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
           "(i.privacy = com.galerija.entity.ImagePrivacy.PUBLIC OR i.user.id = :currentUserId) AND " +
           "(LOWER(i.tags) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.type) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Image> searchImages(@Param("query") String query, @Param("currentUserId") Long currentUserId, Pageable pageable);
    
    List<Image> findByIdIn(List<Long> ids);
    
    @Query("SELECT i FROM Image i JOIN i.user u WHERE u.id = :userId AND " +
           "(i.privacy = com.galerija.entity.ImagePrivacy.PUBLIC OR i.user.id = :currentUserId)")
    Page<Image> findUserFavorites(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId, Pageable pageable);

    @Query("SELECT i FROM Image i JOIN i.user u WHERE u.id = :userId AND " +
           "(i.privacy = com.galerija.entity.ImagePrivacy.PUBLIC OR i.user.id = :currentUserId)")
    List<Image> findByUserId(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId);
    
    @Query("SELECT i FROM Image i LEFT JOIN FETCH i.user u WHERE i.user = :user")
    List<Image> findByUser(@Param("user") UserEntity user);
    
    @Query("SELECT i FROM Image i WHERE i.privacy = :privacy OR i.user.id = :currentUserId")
    Page<Image> findByPrivacyOrUser(@Param("privacy") ImagePrivacy privacy, @Param("currentUserId") Long currentUserId, Pageable pageable);
    
    @Query("SELECT i FROM Image i WHERE i.privacy = :privacy")
    Page<Image> findByPrivacy(@Param("privacy") ImagePrivacy privacy, Pageable pageable);
    
    Optional<Image> findByWebformatURL(String webformatURL);
}
