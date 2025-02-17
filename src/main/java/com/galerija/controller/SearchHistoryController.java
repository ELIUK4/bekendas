package com.galerija.controller;

import com.galerija.dto.SearchHistoryRequest;
import com.galerija.entity.SearchHistory;
import com.galerija.exception.ResourceNotFoundException;
import com.galerija.service.SearchHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search-history")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class SearchHistoryController {
    private static final Logger logger = LoggerFactory.getLogger(SearchHistoryController.class);

    @Autowired
    private SearchHistoryService searchHistoryService;

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserSearchHistory(Pageable pageable) {
        logger.debug("Getting search history for user with pageable: {}", pageable);
        try {
            Page<SearchHistory> history = searchHistoryService.getUserSearchHistory(pageable);
            logger.debug("Found {} search history entries", history.getTotalElements());
            return ResponseEntity.ok(history);
        } catch (ResourceNotFoundException e) {
            logger.error("Error getting search history: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Vartotojas nerastas", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting search history", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Nepavyko gauti paieškos istorijos", "message", e.getMessage()));
        }
    }

    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> saveSearch(@RequestBody SearchHistoryRequest request) {
        logger.debug("Saving search with query: {}", request.getSearchQuery());
        try {
            SearchHistory savedSearch = searchHistoryService.saveSearch(request.getSearchQuery(), request.getResultsCount());
            logger.debug("Successfully saved search with id: {}", savedSearch.getId());
            return ResponseEntity.ok(savedSearch);
        } catch (ResourceNotFoundException e) {
            logger.error("Error saving search: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Vartotojas nerastas", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error saving search", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Nepavyko išsaugoti paieškos", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> clearSearchHistory() {
        logger.debug("Clearing search history");
        try {
            searchHistoryService.clearUserSearchHistory();
            logger.debug("Successfully cleared search history");
            return ResponseEntity.ok()
                    .body(Map.of("message", "Paieškos istorija sėkmingai išvalyta"));
        } catch (ResourceNotFoundException e) {
            logger.error("Error clearing search history: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Vartotojas nerastas", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error clearing search history", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Nepavyko išvalyti paieškos istorijos", "message", e.getMessage()));
        }
    }
}
