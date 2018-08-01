package com.blackbaud.service.api;

import org.springframework.beans.factory.annotation.Autowired;

public class KafkaClientRandomBuilderSupport {


    public RandomUserBuilder user() {
        return new RandomUserBuilder();
    }

}
