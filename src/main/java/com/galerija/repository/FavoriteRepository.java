package com.galerija.repository;

import com.galerija.entity.Favorite;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndImage(UserEntity user, Image image);
    boolean existsByUserAndImage(UserEntity user, Image image);
    void deleteByUserAndImage(UserEntity user, Image image);
    Page<Favorite> findByUser(UserEntity user, Pageable pageable);
}
