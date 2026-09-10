package com.hrsystem;

import com.hrsystem.delivery.cli.CliRunner;
import com.hrsystem.service.SeedDataGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HrApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(HrApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = application.run(args);
        context.getBean(SeedDataGenerator.class).seed();
        context.getBean(CliRunner.class).start();
        context.close();
    }
}
