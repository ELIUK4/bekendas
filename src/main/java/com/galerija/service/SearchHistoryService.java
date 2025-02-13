package com.galerija.service;

import com.galerija.entity.SearchHistory;
import com.galerija.entity.UserEntity;
import com.galerija.exception.ResourceNotFoundException;
import com.galerija.repository.SearchHistoryRepository;
import com.galerija.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchHistoryService {
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public SearchHistory saveSearch(String query, String filters, Integer resultsCount) {
        // For testing purposes, use a mock user
        UserEntity currentUser = userRepository.findById(1L)
                .orElseGet(() -> {
                    UserEntity user = new UserEntity();
                    user.setId(1L);
                    user.setUsername("testUser");
                    return userRepository.save(user);
                });

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setUser(currentUser);
        searchHistory.setSearchQuery(query);
        searchHistory.setFilters(filters);
        searchHistory.setResultsCount(resultsCount);

        return searchHistoryRepository.save(searchHistory);
    }

    @Transactional(readOnly = true)
    public Page<SearchHistory> getUserSearchHistory(Pageable pageable) {
        // For testing purposes, use a mock user
        UserEntity currentUser = userRepository.findById(1L)
                .orElseGet(() -> {
                    UserEntity user = new UserEntity();
                    user.setId(1L);
                    user.setUsername("testUser");
                    return userRepository.save(user);
                });
        return searchHistoryRepository.findByUserOrderBySearchDateDesc(currentUser, pageable);
    }

    @Transactional
    public void clearUserSearchHistory() {
        // For testing purposes, use a mock user
        UserEntity currentUser = userRepository.findById(1L)
                .orElseGet(() -> {
                    UserEntity user = new UserEntity();
                    user.setId(1L);
                    user.setUsername("testUser");
                    return userRepository.save(user);
                });
        try {
            searchHistoryRepository.deleteByUser(currentUser);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error clearing history: " + e.getMessage());
        }
    }
}
