package com.cookmate.simulator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulator")
@Getter
@Setter
public class SimulationProperties {

    // Reserved for future configuration of simulator behavior
    // Currently unused as simulator-service only handles step-by-step cooking simulation
}
