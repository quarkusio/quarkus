package io.quarkus.test.component.beans;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@SimpleQualifier
@Singleton
public class Bravo {

    @Inject
    Charlie charlie;

    public String ping() {
        try {
            Thread.sleep(7l);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return charlie.ping();
    }

}
