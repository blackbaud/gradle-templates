package com.blackbaud.service.core;
import com.blackbaud.service.core.domain.RandomCarEntityBuilder;
import com.blackbaud.service.core.domain.CarRepository;

import org.springframework.beans.factory.annotation.Autowired;

public class CoreRandomBuilderSupport {


    @Autowired
    private CarRepository carRepository;

    public RandomCarEntityBuilder carEntity() {
        return new RandomCarEntityBuilder(carRepository);
    }

}
