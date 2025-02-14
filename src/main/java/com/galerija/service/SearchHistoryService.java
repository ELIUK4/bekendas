package com.galerija.service;

import com.galerija.entity.SearchHistory;
import com.galerija.entity.UserEntity;
import com.galerija.exception.ResourceNotFoundException;
import com.galerija.repository.SearchHistoryRepository;
import com.galerija.repository.UserRepository;
import com.galerija.security.SecurityUtils;
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

    @Autowired
    private SecurityUtils securityUtils;

    @Transactional
    public SearchHistory saveSearch(String query, Integer resultsCount) {
        return saveSearch(query, null, resultsCount);
    }

    @Transactional
    public SearchHistory saveSearch(String query, String filters, Integer resultsCount) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResourceNotFoundException("User not found or not authenticated");
        }

        UserEntity currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setUser(currentUser);
        searchHistory.setSearchQuery(query);
        searchHistory.setFilters(filters);
        searchHistory.setResultsCount(resultsCount);

        return searchHistoryRepository.save(searchHistory);
    }

    @Transactional(readOnly = true)
    public Page<SearchHistory> getUserSearchHistory(Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResourceNotFoundException("User not found or not authenticated");
        }

        UserEntity currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return searchHistoryRepository.findByUserOrderBySearchDateDesc(currentUser, pageable);
    }

    @Transactional
    public void clearUserSearchHistory() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResourceNotFoundException("User not found or not authenticated");
        }

        UserEntity currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        try {
            searchHistoryRepository.deleteByUser(currentUser);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Error clearing history: " + e.getMessage());
        }
    }
}
