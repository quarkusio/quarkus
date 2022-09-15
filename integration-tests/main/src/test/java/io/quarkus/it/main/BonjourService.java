package io.quarkus.it.main;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import io.quarkus.it.rest.GreetingService;

@ApplicationScoped
@Alternative
public class BonjourService extends GreetingService {

    @Override
    public String greet(String greeting) {
        return "Bonjour " + greeting;
    }
}
