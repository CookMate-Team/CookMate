package com.cookmate.main.service;

import com.cookmate.main.dto.MealSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealDbClientTest {

    @Mock
    private WebClient webClient;
    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private MealDbClient mealDbClient;

    @BeforeEach
    void setUp() {
        mealDbClient = new MealDbClient(webClient);
    }

    @Test
    void searchByLetter_shouldCallTheMealDbAndReturnResponse() {
        MealSearchResponse expected = new MealSearchResponse(List.of());

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class)).thenReturn(Mono.just(expected));

        MealSearchResponse actual = mealDbClient.searchByLetter("A").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());
        assertTrue(urlCaptor.getValue().contains("/search.php?f=a"));
        assertEquals(expected, actual);
    }

    @Test
    void lookupById_shouldCallTheMealDbAndReturnResponse() {
        MealSearchResponse expected = new MealSearchResponse(List.of());

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class)).thenReturn(Mono.just(expected));

        MealSearchResponse actual = mealDbClient.lookupById("52772").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());
        assertTrue(urlCaptor.getValue().contains("/lookup.php?i=52772"));
        assertEquals(expected, actual);
    }

    @Test
    void searchByLetter_shouldRejectInvalidLetter() {
        assertThrows(IllegalArgumentException.class, () -> mealDbClient.searchByLetter("ab").block());
    }

    @Test
    void lookupById_shouldRejectBlankMealId() {
        assertThrows(IllegalArgumentException.class, () -> mealDbClient.lookupById(" ").block());
    }

    @Test
    void searchByLetter_shouldWrapDownstreamError() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class)).thenReturn(Mono.error(new RuntimeException("boom")));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> mealDbClient.searchByLetter("a").block());

        assertTrue(thrown.getMessage().contains("Error calling TheMealDB API"));
    }
}

