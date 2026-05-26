package com.cookmate.simulator.client;

import com.cookmate.simulator.dto.MainServiceStepDto;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testy MainServiceClient (Feign interface) — weryfikuje zachowanie
 * przy różnych odpowiedziach z main-service: sukces, 404, 500, błąd sieci.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MainServiceClient — komunikacja z main-service")
class MainServiceClientTest {

    @Mock
    private MainServiceClient mainServiceClient;

    // --- getRecipeSteps success ---

    @Test
    @DisplayName("getRecipeSteps — zwraca listę kroków przy sukcesie")
    void getRecipeSteps_returnsStepsOnSuccess() {
        var expected = List.of(
            new MainServiceStepDto(1L, 1, "Pokrój cebulę", null, 5, "52772"),
            new MainServiceStepDto(2L, 2, "Podsmaż", null, 10, "52772")
        );
        when(mainServiceClient.getRecipeSteps("52772")).thenReturn(expected);

        List<MainServiceStepDto> result = mainServiceClient.getRecipeSteps("52772");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Pokrój cebulę", result.get(0).description());
        assertEquals(2, result.get(1).stepNumber());
        verify(mainServiceClient).getRecipeSteps("52772");
    }

    @Test
    @DisplayName("getRecipeSteps — zwraca pustą listę gdy brak kroków")
    void getRecipeSteps_returnsEmptyList() {
        when(mainServiceClient.getRecipeSteps("99999")).thenReturn(List.of());

        List<MainServiceStepDto> result = mainServiceClient.getRecipeSteps("99999");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- getRecipeSteps 404 ---

    @Test
    @DisplayName("getRecipeSteps — rzuca FeignException.NotFound przy 404")
    void getRecipeSteps_throwsNotFoundOn404() {
        Request feignRequest = Request.create(
            Request.HttpMethod.GET, "/api/recipes/unknown/steps",
            Collections.emptyMap(), null, new RequestTemplate()
        );
        when(mainServiceClient.getRecipeSteps("unknown"))
            .thenThrow(new FeignException.NotFound("Not Found", feignRequest, null, null));

        FeignException.NotFound ex = assertThrows(FeignException.NotFound.class,
            () -> mainServiceClient.getRecipeSteps("unknown"));

        assertTrue(ex.getMessage().contains("Not Found"));
    }

    // --- getRecipeSteps 500 ---

    @Test
    @DisplayName("getRecipeSteps — rzuca FeignException.InternalServerError przy 500")
    void getRecipeSteps_throwsOnServerError() {
        Request feignRequest = Request.create(
            Request.HttpMethod.GET, "/api/recipes/52772/steps",
            Collections.emptyMap(), null, new RequestTemplate()
        );
        when(mainServiceClient.getRecipeSteps("52772"))
            .thenThrow(new FeignException.InternalServerError("Internal Server Error", feignRequest, null, null));

        assertThrows(FeignException.InternalServerError.class,
            () -> mainServiceClient.getRecipeSteps("52772"));
    }

    // --- network error ---

    @Test
    @DisplayName("getRecipeSteps — rzuca RuntimeException przy błędzie sieci")
    void getRecipeSteps_throwsOnNetworkError() {
        when(mainServiceClient.getRecipeSteps("52772"))
            .thenThrow(new RuntimeException("Connection refused"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> mainServiceClient.getRecipeSteps("52772"));

        assertEquals("Connection refused", ex.getMessage());
    }

}
