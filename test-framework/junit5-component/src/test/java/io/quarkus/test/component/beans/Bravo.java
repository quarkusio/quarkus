package io.quarkus.test.component.beans;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@SimpleQualifier
@Singleton
public class Bravo {

    @Inject
    Charlie charlie;

    public String ping() {
        return charlie.ping();
    }

}
