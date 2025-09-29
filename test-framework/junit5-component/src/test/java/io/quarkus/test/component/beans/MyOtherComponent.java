package io.quarkus.test.component.beans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyOtherComponent {

    @Inject
    Charlie charlie;

    // not proxyable - needs transformation
    private MyOtherComponent() {

    }

    public Object ping() {
        return charlie.ping();
    }

}
