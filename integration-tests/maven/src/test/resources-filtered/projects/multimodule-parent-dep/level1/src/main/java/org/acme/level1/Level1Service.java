package org.acme.level1;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.acme.level0.Level0Service;

@ApplicationScoped
public class Level1Service {

    @Inject
    Level0Service level0Service;

    public String getGreetingFromLevel0() {
        return level0Service.getGreeting();
    }
}
