package com.blackbaud.service.api;

import static com.blackbaud.service.api.RestClientARandom.aRandom

class RandomUserBuilder extends User.UserBuilder {

    public RandomUserBuilder() {
        throw new RuntimeException("add some stuff")
    }

}
