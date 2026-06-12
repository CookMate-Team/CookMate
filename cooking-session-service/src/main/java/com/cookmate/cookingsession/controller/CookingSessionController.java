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
            @Valid @RequestBody StepCompletionEventDto event,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromCallable(() -> {
            String userId = jwt.getSubject();
            CookingSessionProgressDto saved = cookingSessionService.handleProgressEvent(event, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        });
    }

    @GetMapping("/recipes/{recipeId}/history")
    public Mono<ResponseEntity<List<CookingSessionProgressDto>>> getHistory(
            @PathVariable String recipeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromCallable(() -> 
            ResponseEntity.ok(cookingSessionService.getHistoryByRecipe(recipeId, jwt.getSubject()))
        );
    }

    @GetMapping("/recipes/{recipeId}/latest")
    public Mono<ResponseEntity<CookingSessionProgressDto>> getLatest(
            @PathVariable String recipeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromCallable(() -> {
            CookingSessionProgressDto latest = cookingSessionService.getLatestByRecipe(recipeId, jwt.getSubject());
            return latest == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(latest);
        });
    }

    @GetMapping("/recipes/{recipeId}/active")
    public Mono<ResponseEntity<ActiveCookingSessionDto>> getActiveSession(
            @PathVariable String recipeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromCallable(() -> 
            cookingSessionService.getActiveSessionDetails(recipeId, jwt.getSubject())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build())
        );
    }

    @GetMapping("/active")
    public Mono<ResponseEntity<ActiveCookingSessionDto>> getActiveSessionGlobal(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromCallable(() -> 
            cookingSessionService.getActiveSessionGlobal(jwt.getSubject())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build())
        );
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<ResponseEntity<Void>> completeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromRunnable(() -> cookingSessionService.completeSession(sessionId, jwt.getSubject()))
                .thenReturn(ResponseEntity.ok().build());
    }

    @GetMapping(value = "/recipes/{recipeId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<CookingSessionProgressDto>> streamProgress(
            @PathVariable String recipeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return cookingSessionService.streamProgress(recipeId, jwt.getSubject())
                .map(progress -> ServerSentEvent.builder(progress)
                        .event("progress")
                        .build());
    }
}
