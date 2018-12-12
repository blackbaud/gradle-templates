package com.blackbaud.service;

import com.blackbaud.service.core.CosmosConfig;
import com.blackbaud.boot.config.WebMvcRestServiceConfig;
import com.blackbaud.service.core.CoreConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan("com.blackbaud.service.resources")
@Import(CosmosConfig.class, 
        CoreConfig.class,
        WebMvcRestServiceConfig.class,
)
public class Service extends CommonSpringConfig {

    public static void main(String[] args) {
        SpringApplication.run(Service.class, args);
    }

}
