package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@ApplicationScoped
@Alternative
public class BonjourService extends GreetingService {

    @Override
    public String greet(String greeting) {
        return "Bonjour " + greeting;
    }
}
