package com.galerija.controller;

import com.galerija.dto.SearchHistoryRequest;
import com.galerija.entity.SearchHistory;
import com.galerija.service.SearchHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search-history")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class SearchHistoryController {
    private static final Logger logger = LoggerFactory.getLogger(SearchHistoryController.class);

    @Autowired
    private SearchHistoryService searchHistoryService;

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<SearchHistory>> getUserSearchHistory(Pageable pageable) {
        logger.debug("Getting search history for user with pageable: {}", pageable);
        try {
            Page<SearchHistory> history = searchHistoryService.getUserSearchHistory(pageable);
            logger.debug("Found {} search history entries", history.getTotalElements());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error getting search history", e);
            throw e;
        }
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SearchHistory> saveSearch(@RequestBody SearchHistoryRequest request) {
        logger.debug("Saving search with query: {}", request.getSearchQuery());
        try {
            SearchHistory savedSearch = searchHistoryService.saveSearch(request.getSearchQuery(), request.getResultsCount());
            logger.debug("Successfully saved search with id: {}", savedSearch.getId());
            return ResponseEntity.ok(savedSearch);
        } catch (Exception e) {
            logger.error("Error saving search", e);
            throw e;
        }
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> clearSearchHistory() {
        logger.debug("Clearing search history");
        try {
            searchHistoryService.clearUserSearchHistory();
            logger.debug("Successfully cleared search history");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error clearing search history", e);
            throw e;
        }
    }
}
