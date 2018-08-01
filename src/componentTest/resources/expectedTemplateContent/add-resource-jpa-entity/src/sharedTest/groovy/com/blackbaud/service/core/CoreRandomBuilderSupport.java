package com.blackbaud.service.core;
import com.blackbaud.service.core.domain.RandomAccountEntityBuilder;
import com.blackbaud.service.core.domain.AccountRepository;

import org.springframework.beans.factory.annotation.Autowired;

public class CoreRandomBuilderSupport {


    @Autowired
    private AccountRepository accountRepository;

    public RandomAccountEntityBuilder accountEntity() {
        return new RandomAccountEntityBuilder(accountRepository);
    }

}
