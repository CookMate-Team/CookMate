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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/cooking-sessions")
@RequiredArgsConstructor
public class CookingSessionController {

    private final CookingSessionService cookingSessionService;

    @PostMapping("/progress")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CookingSessionProgressDto> receiveProgress(
            @Valid @RequestBody StepCompletionEventDto event
    ) {
        CookingSessionProgressDto saved = cookingSessionService.handleProgressEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/recipes/{recipeId}/history")
    public ResponseEntity<List<CookingSessionProgressDto>> getHistory(
            @PathVariable String recipeId
    ) {
        return ResponseEntity.ok(cookingSessionService.getHistoryByRecipe(recipeId));
    }

    @GetMapping("/recipes/{recipeId}/latest")
    public ResponseEntity<CookingSessionProgressDto> getLatest(
            @PathVariable String recipeId
    ) {
        CookingSessionProgressDto latest = cookingSessionService.getLatestByRecipe(recipeId);
        return latest == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(latest);
    }

    @GetMapping("/recipes/{recipeId}/active")
    public ResponseEntity<ActiveCookingSessionDto> getActiveSession(
            @PathVariable String recipeId
    ) {
        return cookingSessionService.getActiveSessionDetails(recipeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<ActiveCookingSessionDto> getActiveSessionGlobal() {
        return cookingSessionService.getActiveSessionGlobal()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> completeSession(
            @PathVariable String sessionId
    ) {
        cookingSessionService.completeSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/recipes/{recipeId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<CookingSessionProgressDto>> streamProgress(
            @PathVariable String recipeId
    ) {
        return cookingSessionService.streamProgress(recipeId)
                .map(progress -> ServerSentEvent.builder(progress)
                        .event("progress")
                        .build());
    }
}
