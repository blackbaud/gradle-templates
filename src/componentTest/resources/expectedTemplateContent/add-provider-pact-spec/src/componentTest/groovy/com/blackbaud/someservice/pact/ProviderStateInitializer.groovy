package com.blackbaud.someservice.pact

import com.blackbaud.pact.api.InteractionDetails
import com.blackbaud.pact.support.ProviderPactInitializer
import org.springframework.stereotype.Component

@Component
class ProviderStateInitializer implements ProviderPactInitializer {

    @Override
    void initialize(InteractionDetails interactionDetails) {
    }

}
