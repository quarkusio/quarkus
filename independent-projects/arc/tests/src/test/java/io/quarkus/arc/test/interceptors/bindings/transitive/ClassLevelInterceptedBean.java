package io.quarkus.arc.test.interceptors.bindings.transitive;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@SomeAnnotation
public class ClassLevelInterceptedBean {

    public void ping() {

    }
}
