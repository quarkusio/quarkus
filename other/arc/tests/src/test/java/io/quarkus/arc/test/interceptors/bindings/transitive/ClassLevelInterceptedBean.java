package io.quarkus.arc.test.interceptors.bindings.transitive;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@SomeAnnotation
public class ClassLevelInterceptedBean {

    public void ping() {

    }
}
