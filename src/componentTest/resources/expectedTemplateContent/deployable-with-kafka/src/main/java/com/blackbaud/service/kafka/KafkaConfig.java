package com.blackbaud.service.kafka;

import com.blackbaud.kafka.config.ConsumerConfig;
import com.blackbaud.kafka.config.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ConsumerConfig.class, ProducerConfig.class})
public class KafkaConfig {

    @Value("${kafka.topic.name}")
    private String topic;

}