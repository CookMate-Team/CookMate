package com.cookmate.cookingsession.controller;

import com.cookmate.cookingsession.dto.ActiveCookingSessionDto;
import com.cookmate.cookingsession.dto.CookingSessionProgressDto;
import com.cookmate.cookingsession.dto.StepCompletionEventDto;
import com.cookmate.cookingsession.service.CookingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/cooking-sessions")
@RequiredArgsConstructor
public class CookingSessionController {

    private final CookingSessionService cookingSessionService;

    @PostMapping("/progress")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<ResponseEntity<CookingSessionProgressDto>> receiveProgress(
            @Valid @RequestBody StepCompletionEventDto event
    ) {
        return Mono.fromCallable(() -> {
            CookingSessionProgressDto saved = cookingSessionService.handleProgressEvent(event);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        });
    }

    @GetMapping("/recipes/{recipeId}/history")
    public ResponseEntity<List<CookingSessionProgressDto>> getHistory(
            @PathVariable String recipeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(cookingSessionService.getHistoryByRecipe(recipeId, extractUserId(jwt)));
    }

    @GetMapping("/recipes/{recipeId}/latest")
    public ResponseEntity<CookingSessionProgressDto> getLatest(
            @PathVariable String recipeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        CookingSessionProgressDto latest = cookingSessionService.getLatestByRecipe(recipeId, extractUserId(jwt));
        return latest == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(latest);
    }

    @GetMapping("/recipes/{recipeId}/active")
    public ResponseEntity<ActiveCookingSessionDto> getActiveSession(
            @PathVariable String recipeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(cookingSessionService.getActiveSessionDetails(recipeId, extractUserId(jwt)).orElse(null));
    }

    /**
     * Returns the calling user's own active cooking session (if any).
     * Multiple users can have simultaneous independent sessions.
     */
    @GetMapping("/active")
    public ResponseEntity<ActiveCookingSessionDto> getActiveSessionForCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(cookingSessionService.getActiveSessionForUser(extractUserId(jwt)).orElse(null));
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<ResponseEntity<Void>> completeSession(
            @PathVariable String sessionId
    ) {
        return Mono.fromRunnable(() -> cookingSessionService.completeSession(sessionId))
                .thenReturn(ResponseEntity.ok().build());
    }

    @GetMapping(value = "/recipes/{recipeId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<CookingSessionProgressDto>> streamProgress(
            @PathVariable String recipeId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String sessionId
    ) {
        return cookingSessionService.streamProgress(recipeId, sessionId)
                .map(progress -> ServerSentEvent.builder(progress)
                        .event("progress")
                        .build());
    }

    private String extractUserId(Jwt jwt) {
        if (jwt == null) return null;
        // Prefer 'sub' (subject) as the stable user identifier
        String sub = jwt.getSubject();
        return sub != null ? sub : jwt.getClaimAsString("preferred_username");
    }
}
