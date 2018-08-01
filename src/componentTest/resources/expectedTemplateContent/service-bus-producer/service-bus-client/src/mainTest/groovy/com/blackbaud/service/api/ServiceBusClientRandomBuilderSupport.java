package com.blackbaud.service.api;

import org.springframework.beans.factory.annotation.Autowired;

public class ServiceBusClientRandomBuilderSupport {


    public RandomProducerPayloadBuilder producerPayload() {
        return new RandomProducerPayloadBuilder();
    }

}
