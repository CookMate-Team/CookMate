package com.cookmate.simulator.exception;

public class SimulationSessionNotFoundException extends RuntimeException {

    public SimulationSessionNotFoundException(String sessionId) {
        super("Simulation session not found: " + sessionId);
    }
}
