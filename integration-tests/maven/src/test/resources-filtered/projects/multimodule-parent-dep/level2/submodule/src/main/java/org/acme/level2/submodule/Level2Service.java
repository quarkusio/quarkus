package org.acme.level2.submodule;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.acme.level1.Level1Service;

@ApplicationScoped
public class Level2Service {

    @Inject
    Level1Service level1Service;

    public String getGreetingFromLevel1() {
        return level1Service.getGreetingFromLevel0();
    }
}
