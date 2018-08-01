package com.blackbaud.service.servicebus;

import com.blackbaud.azure.servicebus.config.ServiceBusProperties;
import com.blackbaud.azure.servicebus.config.ServiceBusConsumerConfig;
import com.blackbaud.azure.servicebus.config.ServiceBusPublisherConfig;
import com.blackbaud.azure.servicebus.consumer.ServiceBusConsumer;
import com.blackbaud.azure.servicebus.consumer.ServiceBusConsumerBuilder;
import com.blackbaud.azure.servicebus.publisher.JsonMessagePublisher;
import com.blackbaud.azure.servicebus.publisher.ServiceBusPublisherBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ServiceBusConsumerConfig.class, ServiceBusPublisherConfig.class})
@EnableConfigurationProperties(ProducerServiceBusProperties.class)
public class ServiceBusConfig {

    @Bean
    public ProducerMessageHandler ProducerMessageHandler() {
        return new ProducerMessageHandler();
    }

    @Bean
    public ServiceBusConsumer ProducerConsumer(
            ServiceBusConsumerBuilder.Factory serviceBusConsumerFactory,
            ProducerMessageHandler ProducerMessageHandler,
            ProducerServiceBusProperties serviceBusProperties) {
        return serviceBusConsumerFactory.create()
                .dataSyncTopicServiceBus(serviceBusProperties)
                .jsonMessageHandler(ProducerMessageHandler, ProducerPayload.class)
                .build();
    }

}
