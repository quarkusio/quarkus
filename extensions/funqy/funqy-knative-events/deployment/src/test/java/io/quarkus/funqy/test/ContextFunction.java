package io.quarkus.funqy.test;

import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;

public class ContextFunction {

    @Funq
    public void context(String body, @Context io.cloudevents.CloudEvent event) {
        if (event == null)
            throw new RuntimeException();
        if (!event.getSpecVersion().toString().equals("1.0"))
            throw new RuntimeException();
        if (!event.getId().equals("1234"))
            throw new RuntimeException();
        if (!event.getSubject().equals("bb"))
            throw new RuntimeException();
        if (!event.getSource().toString().equals("test"))
            throw new RuntimeException();
        if (event.getTime() == null)
            throw new RuntimeException();
    }
}
