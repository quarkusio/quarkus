package io.quarkus.arc.test.interceptors.bindings.transitive;

import javax.enterprise.context.ApplicationScoped;

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
