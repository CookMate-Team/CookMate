package com.cookmate.main.exception;

public class StepNotFoundException extends ResourceNotFoundException {

    public StepNotFoundException(Long stepId) {
        super(ErrorCode.STEP_NOT_FOUND, "Step with id " + stepId + " not found");
    }
}
