package com.evallab.websearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AgentProperties.class)
public class WebSearchAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSearchAgentApplication.class, args);
    }
}
