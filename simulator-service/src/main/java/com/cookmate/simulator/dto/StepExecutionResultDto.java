package com.cookmate.simulator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimalist response DTO for step execution result.
 * Used by simulator-service to confirm step completion to main-service.
 * 
 * Only contains essential information:
 * - stepNumber: identifies which step was processed
 * - success: indicates if the step execution succeeded
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Minimal step execution result")
public class StepExecutionResultDto {

    /**
     * Step number that was executed.
     * Must be positive and not null.
     */
    @NotNull(message = "Step number cannot be null")
    @Positive(message = "Step number must be positive")
    @Schema(description = "Executed step number", example = "3")
    private Integer stepNumber;

    /**
     * Whether the step execution succeeded.
     * true = step completed successfully
     * false = step execution failed
     */
    @NotNull(message = "Success status cannot be null")
    @Schema(description = "Execution outcome", example = "true")
    private Boolean success;
}
