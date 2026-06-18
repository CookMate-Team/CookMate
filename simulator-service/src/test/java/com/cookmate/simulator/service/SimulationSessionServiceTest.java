package com.cookmate.simulator.service;

import com.cookmate.simulator.client.CookingSessionClient;
import com.cookmate.simulator.client.MainServiceClient;
import com.cookmate.simulator.dto.*;
import com.cookmate.simulator.exception.*;
import com.cookmate.simulator.model.*;
import com.cookmate.simulator.repository.SimulationSessionRepository;
import com.cookmate.simulator.repository.SimulationStepRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SimulationSessionService — komunikacja simulator ↔ main")
class SimulationSessionServiceTest {

    @Mock private SimulationSessionRepository sessionRepo;
    @Mock private SimulationStepRepository stepRepo;
    @Mock private MainServiceClient mainClient;
    @Mock private CookingSessionClient cookingSessionClient;
    @InjectMocks private SimulationService service;

    private SimulationSession running;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        running = new SimulationSession();
        running.setId("s-123");
        running.setUserId("user-123");
        running.setStatus(SimulationStatus.RUNNING);
        running.setCurrentStep(0);
        running.setTotalSteps(3);
        running.setRecipeId("r-42");
        running.setTotalRecipes(1);
    }

    // --- startSession ---

    @Test
    @DisplayName("startSession — tworzy sesję i pobiera kroki z main-service")
    void startSession_createsSessionAndFetchesSteps() {
        var steps = List.of(
            new MainServiceStepDto(1L, 1, "Step 1", null, 5, "r-42"),
            new MainServiceStepDto(2L, 2, "Step 2", null, 3, "r-42")
        );
        when(mainClient.getRecipeSteps("r-42")).thenReturn(steps);
        when(sessionRepo.findByStatusAndUserId(SimulationStatus.RUNNING, "user-123")).thenReturn(List.of());
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stepRepo.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        var resp = service.startSession(new StartSimulationRequestDto("r-42", 4), "user-123", "Bearer test-token");

        assertNotNull(resp.sessionId());
        assertEquals("RUNNING", resp.status());
        assertEquals(2, resp.totalSteps());
        verify(mainClient).getRecipeSteps("r-42");
        verify(stepRepo).saveAll(any());
    }

    @Test
    @DisplayName("startSession — rzuca wyjątek gdy brak kroków")
    void startSession_throwsWhenNoSteps() {
        when(mainClient.getRecipeSteps("r-42")).thenReturn(List.of());
        assertThrows(InvalidSimulationStateException.class,
            () -> service.startSession(new StartSimulationRequestDto("r-42", 4), "user-123", "Bearer test-token"));
    }

    @Test
    @DisplayName("startSession — rzuca wyjątek gdy main-service niedostępny")
    void startSession_throwsOnMainServiceError() {
        when(mainClient.getRecipeSteps("r-42")).thenThrow(new RuntimeException("Connection refused"));
        var ex = assertThrows(MainServiceCommunicationException.class,
            () -> service.startSession(new StartSimulationRequestDto("r-42", 4), "user-123", "Bearer test-token"));
        assertTrue(ex.getMessage().contains("r-42"));
    }

    @Test
    @DisplayName("startSession — rzuca wyjątek przy pustej description")
    void startSession_throwsOnBlankDescription() {
        when(mainClient.getRecipeSteps("r-42")).thenReturn(
            List.of(new MainServiceStepDto(1L, 1, "", null, 5, "r-42")));
        assertThrows(InvalidSimulationStateException.class,
            () -> service.startSession(new StartSimulationRequestDto("r-42", 4), "user-123", "Bearer test-token"));
    }

    // --- executeNextStep ---

    @Test
    @DisplayName("executeNextStep — wykonuje PENDING krok")
    void executeNextStep_executesPendingStep() {
        var pending = mkStep("s-123", 1, StepStatus.PENDING);
        when(sessionRepo.findByIdAndUserId("s-123", "user-123")).thenReturn(Optional.of(running));
        when(stepRepo.findFirstBySessionIdAndStatusOrderByStepNumberAsc("s-123", StepStatus.PENDING))
            .thenReturn(Optional.of(pending));
        when(stepRepo.countBySessionIdAndStatus("s-123", StepStatus.PENDING)).thenReturn(2L);
        when(stepRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var r = service.executeNextStep("s-123", "user-123", "Bearer test-token");
        assertTrue(r.getSuccess());
        assertEquals(1, r.getStepNumber());
    }

    @Test
    @DisplayName("executeNextStep — false gdy brak PENDING")
    void executeNextStep_falseWhenNoPending() {
        when(sessionRepo.findByIdAndUserId("s-123", "user-123")).thenReturn(Optional.of(running));
        when(stepRepo.findFirstBySessionIdAndStatusOrderByStepNumberAsc("s-123", StepStatus.PENDING))
            .thenReturn(Optional.empty());
        when(stepRepo.countBySessionIdAndStatus("s-123", StepStatus.PENDING)).thenReturn(0L);
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        assertFalse(service.executeNextStep("s-123", "user-123", "Bearer test-token").getSuccess());
    }

    @Test
    @DisplayName("executeNextStep — wyjątek dla nieistniejącej sesji")
    void executeNextStep_throwsSessionNotFound() {
        when(sessionRepo.findByIdAndUserId("x", "user-123")).thenReturn(Optional.empty());
        assertThrows(SimulationSessionNotFoundException.class, () -> service.executeNextStep("x", "user-123", "Bearer test-token"));
    }

    @Test
    @DisplayName("executeNextStep — wyjątek gdy sesja COMPLETED")
    void executeNextStep_throwsWhenCompleted() {
        running.setStatus(SimulationStatus.COMPLETED);
        when(sessionRepo.findByIdAndUserId("s-123", "user-123")).thenReturn(Optional.of(running));
        assertThrows(InvalidSimulationStateException.class, () -> service.executeNextStep("s-123", "user-123", "Bearer test-token"));
    }

    // --- processStep ---

    @Test
    @DisplayName("processStep — przetwarza krok z sukcesem")
    void processStep_success() {
        when(sessionRepo.findByIdAndUserId("s-123", "user-123")).thenReturn(Optional.of(running));
        when(stepRepo.findBySessionIdAndStepNumber("s-123", 1)).thenReturn(Optional.empty());
        when(stepRepo.countBySessionIdAndStatus("s-123", StepStatus.PENDING)).thenReturn(2L);
        when(stepRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var r = service.processStep("s-123", new RecipeStepRequestDto(1, "Heat pan", 30, "200C", "100g", "note"), "user-123", "Bearer test-token");
        assertTrue(r.getSuccess());
        assertEquals(1, r.getStepNumber());
    }

    // --- getStatus ---

    @Test
    @DisplayName("getStatus — wyjątek dla nieistniejącej sesji")
    void getStatus_throwsNotFound() {
        when(sessionRepo.findByIdAndUserId("x", "user-123")).thenReturn(Optional.empty());
        assertThrows(SimulationSessionNotFoundException.class, () -> service.getStatus("x", "user-123"));
    }

    @Test
    @DisplayName("getStatus — zwraca poprawny status")
    void getStatus_returnsCorrectStatus() {
        var steps = List.of(mkStep("s-123", 1, StepStatus.EXECUTED), mkStep("s-123", 2, StepStatus.PENDING));
        when(sessionRepo.findByIdAndUserId("s-123", "user-123")).thenReturn(Optional.of(running));
        when(stepRepo.findBySessionIdOrderByStepNumberAsc("s-123")).thenReturn(steps);
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var st = service.getStatus("s-123", "user-123");
        assertEquals("RUNNING", st.status());
        assertEquals(1, st.currentStep());
    }

    // --- rewindToStep ---

    @Test
    @DisplayName("rewindToStep — cofa sesję")
    void rewindToStep_rewinds() {
        var s1 = mkStep("s-123", 1, StepStatus.EXECUTED); s1.setExecutedAt(LocalDateTime.now());
        var s2 = mkStep("s-123", 2, StepStatus.EXECUTED); s2.setExecutedAt(LocalDateTime.now());
        when(sessionRepo.findByIdAndUserId("s-123", "user-123")).thenReturn(Optional.of(running));
        when(stepRepo.findBySessionIdOrderByStepNumberAsc("s-123")).thenReturn(new ArrayList<>(List.of(s1, s2)));
        when(stepRepo.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        var r = service.rewindToStep("s-123", 1, "user-123");
        assertEquals(1, r.currentStep());
        assertEquals("RUNNING", r.status());
    }

    private SimulationStep mkStep(String sid, int num, StepStatus status) {
        var s = new SimulationStep();
        s.setSessionId(sid); s.setStepNumber(num); s.setStatus(status);
        s.setRecipeName("Step " + num); s.setPreparationTime(num + " min");
        return s;
    }
}
