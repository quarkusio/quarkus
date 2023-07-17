package io.quarkus.arc.test.interceptors.bindings.transitive;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@AnotherAnnotation
public class TwoLevelsDeepClassLevelInterceptedBean {

    public void ping() {

    }
}
