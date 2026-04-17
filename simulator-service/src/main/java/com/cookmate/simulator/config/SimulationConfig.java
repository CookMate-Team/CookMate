package com.cookmate.simulator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
@EnableConfigurationProperties(SimulationProperties.class)
public class SimulationConfig {

    @Bean
    public Random simulationRandom() {
        return new Random();
    }
}
