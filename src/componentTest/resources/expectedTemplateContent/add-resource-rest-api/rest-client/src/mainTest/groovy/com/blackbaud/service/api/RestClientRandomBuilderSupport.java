package com.blackbaud.service.api;

import org.springframework.beans.factory.annotation.Autowired;

public class RestClientRandomBuilderSupport {


    public RandomUserBuilder user() {
        return new RandomUserBuilder();
    }

}
