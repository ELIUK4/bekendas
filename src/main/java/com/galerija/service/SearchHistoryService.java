package com.galerija.service;

import com.galerija.entity.SearchHistory;
import com.galerija.entity.UserEntity;
import com.galerija.repository.SearchHistoryRepository;
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
    private UserService userService;

    @Transactional
    public SearchHistory saveSearch(String query, String filters, Integer resultsCount) {
        UserEntity currentUser = userService.getCurrentUser();

        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setUser(currentUser);
        searchHistory.setSearchQuery(query);
        searchHistory.setFilters(filters);
        searchHistory.setResultsCount(resultsCount);

        return searchHistoryRepository.save(searchHistory);
    }

    @Transactional(readOnly = true)
    public Page<SearchHistory> getUserSearchHistory(Pageable pageable) {
        UserEntity currentUser = userService.getCurrentUser();
        return searchHistoryRepository.findByUserOrderBySearchDateDesc(currentUser, pageable);
    }

    @Transactional
    public void clearUserSearchHistory() {
        UserEntity currentUser = userService.getCurrentUser();
        searchHistoryRepository.deleteByUser(currentUser);
    }
}
