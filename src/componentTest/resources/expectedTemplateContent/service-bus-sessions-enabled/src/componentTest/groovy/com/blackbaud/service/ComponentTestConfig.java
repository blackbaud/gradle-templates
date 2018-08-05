package com.blackbaud.service;

import com.blackbaud.azure.servicebus.publisher.ServiceBusPublisherBuilder;
import com.blackbaud.azure.servicebus.publisher.JsonMessagePublisher;
import com.blackbaud.service.servicebus.ProducerServiceBusProperties;
import com.blackbaud.service.core.CoreRandomBuilderSupport;
import com.blackbaud.testsupport.BaseTestConfig;
import com.blackbaud.testsupport.TestClientSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static com.blackbaud.service.core.CoreARandom.aRandom;

@Configuration
@Import({Service.class})
public class ComponentTestConfig extends BaseTestConfig {

    @Autowired
    TestClientSupport testClientSupport;

    @Bean
    CoreRandomBuilderSupport coreRandomBuilderSupport() {
        return aRandom.coreRandomBuilderSupport;
    }

    @Bean
    public JsonMessagePublisher ProducerPublisher(
            ServiceBusPublisherBuilder.Factory serviceBusPublisherFactory,
            ProducerServiceBusProperties serviceBusProperties) {
        return serviceBusPublisherFactory.create()
                .supportsSessions(ProducerPayload::getId)
                .buildJsonPublisher(serviceBusProperties);
    }

}
