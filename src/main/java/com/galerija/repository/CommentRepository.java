package com.galerija.repository;

import com.galerija.entity.Comment;
import com.galerija.entity.Image;
import com.galerija.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByImage(Image image, Pageable pageable);
    Page<Comment> findByUser(UserEntity user, Pageable pageable);
}
