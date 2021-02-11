package io.quarkus.arc.test.interceptors.bindings.transitive;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@AnotherAnnotation
public class TwoLevelsDeepClassLevelInterceptedBean {

    public void ping() {

    }
}
