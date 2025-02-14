package com.galerija.controller;

import com.galerija.dto.SearchHistoryRequest;
import com.galerija.entity.SearchHistory;
import com.galerija.entity.UserEntity;
import com.galerija.service.SearchHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryControllerTest {

    @Mock
    private SearchHistoryService searchHistoryService;

    @InjectMocks
    private SearchHistoryController searchHistoryController;

    private SearchHistory testSearchHistory;
    private UserEntity testUser;
    private Pageable pageable;
    private SearchHistoryRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        testSearchHistory = new SearchHistory();
        testSearchHistory.setId(1L);
        testSearchHistory.setUser(testUser);
        testSearchHistory.setSearchQuery("test query");
        testSearchHistory.setFilters("type=photo");
        testSearchHistory.setResultsCount(20);
        testSearchHistory.setSearchDate(LocalDateTime.now());

        testRequest = new SearchHistoryRequest();
        testRequest.setSearchQuery("test query");
        testRequest.setResultsCount(20);

        pageable = PageRequest.of(0, 10);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserSearchHistory_Success() {
        // Arrange
        Page<SearchHistory> expectedPage = new PageImpl<>(Arrays.asList(testSearchHistory));
        when(searchHistoryService.getUserSearchHistory(any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        ResponseEntity<Page<SearchHistory>> response = searchHistoryController.getUserSearchHistory(pageable);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        Page<SearchHistory> result = response.getBody();
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testSearchHistory.getSearchQuery(), result.getContent().get(0).getSearchQuery());
        verify(searchHistoryService).getUserSearchHistory(pageable);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getUserSearchHistory_EmptyHistory() {
        // Arrange
        Page<SearchHistory> emptyPage = new PageImpl<>(Arrays.asList());
        when(searchHistoryService.getUserSearchHistory(any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        ResponseEntity<Page<SearchHistory>> response = searchHistoryController.getUserSearchHistory(pageable);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        Page<SearchHistory> result = response.getBody();
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(searchHistoryService).getUserSearchHistory(pageable);
    }

    @Test
    @WithMockUser(roles = "USER")
    void saveSearch_Success() {
        // Arrange
        when(searchHistoryService.saveSearch(anyString(), anyInt()))
                .thenReturn(testSearchHistory);

        // Act
        ResponseEntity<SearchHistory> response = searchHistoryController.saveSearch(testRequest);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        SearchHistory result = response.getBody();
        assertNotNull(result);
        assertEquals(testSearchHistory.getSearchQuery(), result.getSearchQuery());
        verify(searchHistoryService).saveSearch(testRequest.getSearchQuery(), testRequest.getResultsCount());
    }

    @Test
    @WithMockUser(roles = "USER")
    void clearSearchHistory_Success() {
        // Arrange
        doNothing().when(searchHistoryService).clearUserSearchHistory();

        // Act
        ResponseEntity<?> response = searchHistoryController.clearSearchHistory();

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(searchHistoryService).clearUserSearchHistory();
    }
}
