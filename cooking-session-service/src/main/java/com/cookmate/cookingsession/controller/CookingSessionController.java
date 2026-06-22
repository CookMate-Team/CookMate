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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/cooking-sessions")
@RequiredArgsConstructor
@Tag(name = "Cooking Sessions", description = "Endpoints for tracking active cooking sessions and step progress")
public class CookingSessionController {

    private final CookingSessionService cookingSessionService;

    @Operation(summary = "Receive step progress", description = "Record completion of a specific cooking step")
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

    @Operation(summary = "Get cooking history", description = "Get full cooking progress history for a recipe")
    @GetMapping("/recipes/{recipeId}/history")
    public Mono<ResponseEntity<List<CookingSessionProgressDto>>> getHistory(
            @PathVariable String recipeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromCallable(() -> 
            ResponseEntity.ok(cookingSessionService.getHistoryByRecipe(recipeId, jwt.getSubject()))
        );
    }

    @Operation(summary = "Get latest progress", description = "Get latest completed step for a specific recipe")
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

    @Operation(summary = "Get active session by recipe", description = "Get details of an active cooking session for a specific recipe")
    @GetMapping("/recipes/{recipeId}/active")
    public Mono<ResponseEntity<ActiveCookingSessionDto>> getActiveSession(
            @PathVariable String recipeId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromCallable(() -> 
            cookingSessionService.getActiveSessionDetails(recipeId, jwt.getSubject())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build())
        );
    }

    @Operation(summary = "Get global active session", description = "Get details of the currently active cooking session for the user across all recipes")
    @GetMapping("/active")
    public Mono<ResponseEntity<ActiveCookingSessionDto>> getActiveSessionGlobal(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromCallable(() -> 
            cookingSessionService.getActiveSessionGlobal(jwt.getSubject())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build())
        );
    }

    @Operation(summary = "Complete session", description = "Mark an active cooking session as complete")
    @PostMapping("/sessions/{sessionId}/complete")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Mono<ResponseEntity<Void>> completeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Mono.fromRunnable(() -> cookingSessionService.completeSession(sessionId, jwt.getSubject()))
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "Stream session progress", description = "SSE stream for real-time cooking progress updates")
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
