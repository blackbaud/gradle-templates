package com.blackbaud.service;

import org.springframework.context.annotation.Import;
import com.blackbaud.service.servicebus.ServiceBusConfig;
import com.blackbaud.boot.config.CommonSpringConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.blackbaud.service.core", "com.blackbaud.service.resources"})
@Import(ServiceBusConfig.class)
public class Service extends CommonSpringConfig {

    public static void main(String[] args) {
        SpringApplication.run(Service.class, args);
    }

}
