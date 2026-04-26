package com.cookmate.main.exception;

public class StepNotFoundException extends RuntimeException {

    public StepNotFoundException(Long stepId) {
        super("Step with id " + stepId + " not found");
    }
}
