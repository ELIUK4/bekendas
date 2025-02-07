package com.galerija.service;

import com.galerija.entity.SearchHistory;
import com.galerija.entity.UserEntity;
import com.galerija.repository.SearchHistoryRepository;
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

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private UserService userService;

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
    }

    @Test
    void saveSearch_Success() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
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
    }

    @Test
    void getUserSearchHistory_Success() {
        // Arrange
        Page<SearchHistory> expectedPage = new PageImpl<>(Arrays.asList(testSearchHistory));
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(searchHistoryRepository.findByUserOrderBySearchDateDesc(eq(testUser), any(Pageable.class)))
                .thenReturn(expectedPage);

        // Act
        Page<SearchHistory> result = searchHistoryService.getUserSearchHistory(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(QUERY, result.getContent().get(0).getSearchQuery());
        verify(searchHistoryRepository).findByUserOrderBySearchDateDesc(testUser, pageable);
    }

    @Test
    void getUserSearchHistory_EmptyHistory() {
        // Arrange
        Page<SearchHistory> emptyPage = new PageImpl<>(Arrays.asList());
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(searchHistoryRepository.findByUserOrderBySearchDateDesc(eq(testUser), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<SearchHistory> result = searchHistoryService.getUserSearchHistory(pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(searchHistoryRepository).findByUserOrderBySearchDateDesc(testUser, pageable);
    }

    @Test
    void clearUserSearchHistory_Success() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        doNothing().when(searchHistoryRepository).deleteByUser(testUser);

        // Act
        searchHistoryService.clearUserSearchHistory();

        // Assert
        verify(searchHistoryRepository).deleteByUser(testUser);
    }

    @Test
    void clearUserSearchHistory_Error() {
        // Arrange
        when(userService.getCurrentUser()).thenReturn(testUser);
        doThrow(new RuntimeException("Error clearing history"))
                .when(searchHistoryRepository).deleteByUser(testUser);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            searchHistoryService.clearUserSearchHistory()
        );
        verify(searchHistoryRepository).deleteByUser(testUser);
    }
}
