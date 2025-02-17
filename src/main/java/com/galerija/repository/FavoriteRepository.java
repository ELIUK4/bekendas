package com.galerija.repository;

import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndImage(UserEntity user, Image image);
    boolean existsByUserAndImage(UserEntity user, Image image);
    void deleteByUserAndImage(UserEntity user, Image image);
    Page<Favorite> findByUser(UserEntity user, Pageable pageable);

    @Query(value = "SELECT DISTINCT f FROM Favorite f LEFT JOIN FETCH f.user LEFT JOIN FETCH f.image WHERE f.user.id = :userId",
           countQuery = "SELECT COUNT(f) FROM Favorite f WHERE f.user.id = :userId")
    Page<Favorite> findByUserIdWithUser(Long userId, Pageable pageable);
}
