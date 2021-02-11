package io.quarkus.funqy.test;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;

public class ContextFunction {

    @Funq
    public void context(String body, @Context CloudEvent event) {
        if (event == null)
            throw new RuntimeException();
        if (!event.specVersion().equals("1.0"))
            throw new RuntimeException();
        if (!event.id().equals("1234"))
            throw new RuntimeException();
        if (!event.subject().equals("bb"))
            throw new RuntimeException();
        if (!event.source().equals("test"))
            throw new RuntimeException();
        if (event.time() == null)
            throw new RuntimeException();
    }
}
