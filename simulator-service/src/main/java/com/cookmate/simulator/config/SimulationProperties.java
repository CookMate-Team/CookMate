package com.cookmate.simulator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulator.meal-plan")
@Getter
@Setter
public class SimulationProperties {

    private int defaultDays = 3;
    private int minDays = 1;
    private int maxDays = 365;
}
