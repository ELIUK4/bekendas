package com.galerija.service;

import com.galerija.entity.SearchHistory;
import com.galerija.entity.UserEntity;
import com.galerija.exception.ResourceNotFoundException;
import com.galerija.repository.SearchHistoryRepository;
import com.galerija.repository.UserRepository;
import com.galerija.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SearchHistoryServiceTest {

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private SearchHistoryService searchHistoryService;

    private static final String QUERY = "test query";
    private static final String FILTERS = "type=photo";
    private static final Integer RESULTS_COUNT = 20;

    private SearchHistory testSearchHistory;
    private UserEntity testUser;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        testSearchHistory = new SearchHistory();
        testSearchHistory.setId(1L);
        testSearchHistory.setUser(testUser);
        testSearchHistory.setSearchQuery(QUERY);
        testSearchHistory.setFilters(FILTERS);
        testSearchHistory.setResultsCount(RESULTS_COUNT);
        testSearchHistory.setSearchDate(LocalDateTime.now());

        pageable = PageRequest.of(0, 10);

        // Mock SecurityUtils
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void saveSearch_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(searchHistoryRepository.save(any(SearchHistory.class)))
                .thenReturn(testSearchHistory);

        // Act
        SearchHistory result = searchHistoryService.saveSearch(QUERY, FILTERS, RESULTS_COUNT);

        // Assert
        assertNotNull(result);
        assertEquals(QUERY, result.getSearchQuery());
        assertEquals(FILTERS, result.getFilters());
        assertEquals(RESULTS_COUNT, result.getResultsCount());
        assertEquals(testUser, result.getUser());
        verify(searchHistoryRepository).save(any(SearchHistory.class));
        verify(securityUtils).getCurrentUserId();
    }

    @Test
    void getUserSearchHistory_Success() {
        // Arrange
        Page<SearchHistory> expectedPage = new PageImpl<>(Arrays.asList(testSearchHistory));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(searchHistoryRepository.findByUserOrderBySearchDateDesc(eq(testUser), any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        Page<SearchHistory> result = searchHistoryService.getUserSearchHistory(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(QUERY, result.getContent().get(0).getSearchQuery());
        verify(searchHistoryRepository).findByUserOrderBySearchDateDesc(testUser, pageable);
        verify(securityUtils).getCurrentUserId();
    }

    @Test
    void clearUserSearchHistory_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(searchHistoryRepository).deleteByUser(testUser);

        // Act & Assert
        assertDoesNotThrow(() -> searchHistoryService.clearUserSearchHistory());
        verify(searchHistoryRepository).deleteByUser(testUser);
        verify(securityUtils).getCurrentUserId();
    }

    @Test
    void saveSearch_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            searchHistoryService.saveSearch(QUERY, FILTERS, RESULTS_COUNT)
        );
        verify(searchHistoryRepository, never()).save(any());
        verify(securityUtils).getCurrentUserId();
    }

    @Test
    void getUserSearchHistory_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            searchHistoryService.getUserSearchHistory(pageable)
        );
        verify(searchHistoryRepository, never()).findByUserOrderBySearchDateDesc(any(), any());
        verify(securityUtils).getCurrentUserId();
    }

    @Test
    void clearUserSearchHistory_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            searchHistoryService.clearUserSearchHistory()
        );
        verify(searchHistoryRepository, never()).deleteByUser(any());
        verify(securityUtils).getCurrentUserId();
    }

    @Test
    void saveSearch_NoAuthentication() {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            searchHistoryService.saveSearch(QUERY, FILTERS, RESULTS_COUNT)
        );
        verify(searchHistoryRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUserSearchHistory_NoAuthentication() {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            searchHistoryService.getUserSearchHistory(pageable)
        );
        verify(searchHistoryRepository, never()).findByUserOrderBySearchDateDesc(any(), any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void clearUserSearchHistory_NoAuthentication() {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            searchHistoryService.clearUserSearchHistory()
        );
        verify(searchHistoryRepository, never()).deleteByUser(any());
        verify(userRepository, never()).findById(any());
    }
}
