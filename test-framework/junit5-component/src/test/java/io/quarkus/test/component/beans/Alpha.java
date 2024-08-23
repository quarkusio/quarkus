package io.quarkus.test.component.beans;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class Alpha {

    @Inject
    @SimpleQualifier
    Bravo bravo;

    public String ping() {
        return bravo.ping() + bravo.ping();
    }

}
