package io.quarkus.arc.test.interceptors.bindings.transitive;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@NotABinding
public class NotInterceptedBean {

    public void ping() {

    }
}
