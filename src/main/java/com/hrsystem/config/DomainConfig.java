package com.hrsystem.config;

import com.hrsystem.domain.state.ApplicationStateMachine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public ApplicationStateMachine applicationStateMachine() {
        return new ApplicationStateMachine();
    }
}
