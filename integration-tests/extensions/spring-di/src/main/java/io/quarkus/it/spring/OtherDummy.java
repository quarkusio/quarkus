package io.quarkus.it.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OtherDummy {

    private final Dummy dummy;

    public OtherDummy(@Qualifier("dumb") Dummy dummy) {
        this.dummy = dummy;
    }
}
