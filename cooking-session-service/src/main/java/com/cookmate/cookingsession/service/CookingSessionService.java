package com.cookmate.cookingsession.service;

import com.cookmate.cookingsession.dto.ActiveCookingSessionDto;
import com.cookmate.cookingsession.dto.CookingSessionProgressDto;
import com.cookmate.cookingsession.dto.StepCompletionEventDto;
import com.cookmate.cookingsession.model.CookingSession;
import com.cookmate.cookingsession.model.CookingSessionProgress;
import com.cookmate.cookingsession.model.CookingSessionStatus;
import com.cookmate.cookingsession.repository.CookingSessionProgressRepository;
import com.cookmate.cookingsession.repository.CookingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CookingSessionService {

    private static final Logger logger = LoggerFactory.getLogger(CookingSessionService.class);

    private final CookingSessionRepository cookingSessionRepository;
    private final CookingSessionProgressRepository cookingSessionProgressRepository;

    private final Sinks.Many<CookingSessionProgressDto> progressSink = Sinks.many()
            .multicast()
            .directBestEffort();

    @Transactional
    public CookingSessionProgressDto handleProgressEvent(StepCompletionEventDto event) {
        logger.info("Otrzymano event postępu: sessionId={}, stepNumber={}, recipeId={}",
                event.sessionId(), event.stepNumber(), event.recipeId());

        Optional<CookingSessionProgress> existing = cookingSessionProgressRepository
                .findFirstBySessionIdAndStepNumber(event.sessionId(), event.stepNumber());
        if (existing.isPresent()) {
            logger.info("Event już istnieje: sessionId={}, stepNumber={}", event.sessionId(), event.stepNumber());
            return toProgressDto(existing.get());
        }

        upsertSession(event);

        CookingSessionProgress progress = CookingSessionProgress.builder()
                .sessionId(event.sessionId())
                .recipeId(event.recipeId())
                .stepNumber(event.stepNumber())
                .status(event.status())
                .executedAt(event.executedAt())
                .build();

        CookingSessionProgress saved = cookingSessionProgressRepository.save(progress);
        logger.info("Event zapisany: sessionId={}, stepNumber={}, id={}",
                event.sessionId(), event.stepNumber(), saved.getId());

        progressSink.tryEmitNext(toProgressDto(saved));

        return toProgressDto(saved);
    }

    public List<CookingSessionProgressDto> getHistoryByRecipe(String recipeId, String userId) {
        Optional<CookingSession> activeSession = userId != null
                ? getActiveSessionForUser(userId, recipeId)
                : getActiveSession(recipeId);
        if (activeSession.isEmpty()) {
            return List.of();
        }
        return cookingSessionProgressRepository
                .findBySessionIdOrderByStepNumberAsc(activeSession.get().getSessionId())
                .stream()
                .map(this::toProgressDto)
                .toList();
    }

    public CookingSessionProgressDto getLatestByRecipe(String recipeId, String userId) {
        Optional<CookingSession> activeSession = userId != null
                ? getActiveSessionForUser(userId, recipeId)
                : getActiveSession(recipeId);
        if (activeSession.isEmpty()) {
            return null;
        }
        return cookingSessionProgressRepository
                .findFirstBySessionIdOrderByStepNumberDesc(activeSession.get().getSessionId())
                .map(this::toProgressDto)
                .orElse(null);
    }

    public Optional<ActiveCookingSessionDto> getActiveSessionDetails(String recipeId, String userId) {
        Optional<CookingSession> session = userId != null
                ? getActiveSessionForUser(userId, recipeId)
                : getActiveSession(recipeId);
        return session.map(this::toActiveDto);
    }

    /** Returns the current user's active session (any recipe). */
    public Optional<ActiveCookingSessionDto> getActiveSessionForUser(String userId) {
        if (userId == null) return Optional.empty();
        return cookingSessionRepository
                .findFirstByUserIdAndStatusOrderByLastExecutedAtDesc(userId, CookingSessionStatus.RUNNING)
                .map(this::toActiveDto);
    }

    /** Returns the current user's active session for a specific recipe. */
    private Optional<CookingSession> getActiveSessionForUser(String userId, String recipeId) {
        return cookingSessionRepository
                .findFirstByUserIdAndRecipeIdAndStatusOrderByLastExecutedAtDesc(
                        userId, recipeId, CookingSessionStatus.RUNNING);
    }

    @Transactional
    public void completeSession(String sessionId) {
        cookingSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setStatus(CookingSessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            cookingSessionRepository.save(session);
            logger.info("Session {} manually completed", sessionId);
        });
    }

    public Flux<CookingSessionProgressDto> streamProgress(String recipeId, String sessionId) {
        return progressSink.asFlux()
                .filter(event -> event.recipeId().equals(recipeId) &&
                        (sessionId == null || event.sessionId().equals(sessionId)));
    }

    private Optional<CookingSession> getActiveSession(String recipeId) {
        return cookingSessionRepository.findFirstByRecipeIdAndStatusOrderByLastExecutedAtDesc(
                recipeId,
                CookingSessionStatus.RUNNING
        );
    }

    private CookingSession upsertSession(StepCompletionEventDto event) {
        CookingSession session = cookingSessionRepository.findById(event.sessionId()).orElse(null);
        boolean isCompletedEvent = "COMPLETED".equals(event.status());
        CookingSessionStatus targetStatus = isCompletedEvent ? CookingSessionStatus.COMPLETED : CookingSessionStatus.RUNNING;

        if (targetStatus == CookingSessionStatus.RUNNING) {
            completeAllOtherSessionsForUser(event.sessionId(), event.userId());
        }

        if (session == null) {
            completeOtherSessionsForUser(event.recipeId(), event.sessionId(), event.userId());
            session = CookingSession.builder()
                    .sessionId(event.sessionId())
                    .recipeId(event.recipeId())
                    .userId(event.userId() != null ? event.userId() : "unknown")
                    .status(targetStatus)
                    .currentStep(event.stepNumber())
                    .lastExecutedAt(event.executedAt())
                    .completedAt(isCompletedEvent ? event.executedAt() : null)
                    .build();
        } else {
            int currentStep = session.getCurrentStep() == null ? 0 : session.getCurrentStep();
            session.setCurrentStep(Math.max(currentStep, event.stepNumber()));
            session.setLastExecutedAt(event.executedAt());
            session.setStatus(targetStatus);
            if (isCompletedEvent) {
                session.setCompletedAt(event.executedAt());
            } else {
                session.setCompletedAt(null);
            }
        }
        return cookingSessionRepository.save(session);
    }

    private void completeOtherSessionsForUser(String recipeId, String currentSessionId, String userId) {
        if (userId == null) {
            userId = "unknown";
        }
        List<CookingSession> runningSessions = cookingSessionRepository
                .findByUserIdAndRecipeIdAndStatus(userId, recipeId, CookingSessionStatus.RUNNING);
        if (runningSessions.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean updated = false;
        for (CookingSession session : runningSessions) {
            if (!session.getSessionId().equals(currentSessionId)) {
                session.setStatus(CookingSessionStatus.COMPLETED);
                session.setCompletedAt(now);
                updated = true;
            }
        }
        if (updated) {
            cookingSessionRepository.saveAll(runningSessions);
        }
    }

    @Transactional
    public void completeAllOtherSessionsForUser(String currentSessionId, String userId) {
        if (userId == null) {
            userId = "unknown";
        }
        List<CookingSession> runningSessions = cookingSessionRepository.findByUserIdAndStatus(userId, CookingSessionStatus.RUNNING);
        if (runningSessions.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean updated = false;
        for (CookingSession session : runningSessions) {
            if (!session.getSessionId().equals(currentSessionId)) {
                session.setStatus(CookingSessionStatus.COMPLETED);
                session.setCompletedAt(now);
                updated = true;
                logger.info("Session {} completed for user {} due to new active session {}", session.getSessionId(), userId, currentSessionId);
            }
        }
        if (updated) {
            cookingSessionRepository.saveAll(runningSessions);
        }
    }

    private CookingSessionProgressDto toProgressDto(CookingSessionProgress progress) {
        return new CookingSessionProgressDto(
                progress.getSessionId(),
                progress.getRecipeId(),
                progress.getStepNumber(),
                progress.getStatus(),
                progress.getExecutedAt()
        );
    }

    private ActiveCookingSessionDto toActiveDto(CookingSession session) {
        return new ActiveCookingSessionDto(
                session.getSessionId(),
                session.getRecipeId(),
                session.getStatus().name(),
                session.getCurrentStep(),
                session.getLastExecutedAt()
        );
    }
}
