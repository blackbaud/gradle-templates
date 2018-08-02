package com.blackbaud.service.core;
import com.blackbaud.service.core.domain.RandomTruckEntityBuilder;
import com.blackbaud.service.core.domain.TruckRepository;

import org.springframework.beans.factory.annotation.Autowired;

public class CoreRandomBuilderSupport {


    @Autowired
    private TruckRepository truckRepository;

    public RandomTruckEntityBuilder truckEntity() {
        return new RandomTruckEntityBuilder(truckRepository);
    }

}
