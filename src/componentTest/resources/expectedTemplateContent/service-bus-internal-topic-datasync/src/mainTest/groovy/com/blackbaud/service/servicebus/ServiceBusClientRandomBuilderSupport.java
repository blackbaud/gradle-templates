package com.blackbaud.service.servicebus;

import org.springframework.beans.factory.annotation.Autowired;

public class ServiceBusClientRandomBuilderSupport {


    public RandomDatasyncPayloadBuilder datasyncPayload() {
        return new RandomDatasyncPayloadBuilder();
    }

}
