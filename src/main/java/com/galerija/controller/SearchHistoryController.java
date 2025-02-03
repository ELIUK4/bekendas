package com.galerija.controller;

import com.galerija.entity.SearchHistory;
import com.galerija.service.SearchHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/history")
@PreAuthorize("hasRole('USER')")
public class SearchHistoryController {
    @Autowired
    private SearchHistoryService searchHistoryService;

    @GetMapping
    public Page<SearchHistory> getUserSearchHistory(Pageable pageable) {
        return searchHistoryService.getUserSearchHistory(pageable);
    }

    @DeleteMapping
    public ResponseEntity<?> clearSearchHistory() {
        searchHistoryService.clearUserSearchHistory();
        return ResponseEntity.ok().build();
    }
}
