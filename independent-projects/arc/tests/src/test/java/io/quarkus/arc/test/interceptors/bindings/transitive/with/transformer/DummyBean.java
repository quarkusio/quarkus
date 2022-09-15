package io.quarkus.arc.test.interceptors.bindings.transitive.with.transformer;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@PlainBinding
public class DummyBean {

    public void ping() {

    }
}
