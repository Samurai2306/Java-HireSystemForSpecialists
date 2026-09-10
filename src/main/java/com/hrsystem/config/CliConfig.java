package com.hrsystem.config;

import com.hrsystem.delivery.cli.utils.ConsoleTableFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CliConfig {

    @Bean
    public ConsoleTableFormatter consoleTableFormatter() {
        return new ConsoleTableFormatter();
    }
}
