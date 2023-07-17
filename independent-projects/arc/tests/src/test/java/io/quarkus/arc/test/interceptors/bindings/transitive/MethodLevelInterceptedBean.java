package io.quarkus.arc.test.interceptors.bindings.transitive;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MethodLevelInterceptedBean {

    @SomeAnnotation
    public void oneLevelDeepBinding() {

    }

    @AnotherAnnotation
    public void twoLevelsDeepBinding() {

    }

    @NotABinding
    public void shouldNotBeIntercepted() {

    }
}
