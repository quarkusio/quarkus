package io.quarkus.arc.test.observers.inheritance;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NonObservingSubBean extends ObservingBean {

    public void watchFor(SimpleEvent event) {
        value = event.content;
    }
}
