package com.galerija.repository;

import com.galerija.entity.SearchHistory;
import com.galerija.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    Page<SearchHistory> findByUserOrderBySearchDateDesc(UserEntity user, Pageable pageable);
    void deleteByUser(UserEntity user);
}
