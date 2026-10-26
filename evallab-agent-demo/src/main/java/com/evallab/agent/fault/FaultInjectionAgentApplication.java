package com.evallab.agent.fault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FaultInjectionAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaultInjectionAgentApplication.class, args);
    }
}
