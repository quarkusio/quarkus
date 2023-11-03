package io.quarkus.arc.test.transform.injectionPoint.diffPackage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Foo {
    // this IP is deliberately private
    @Inject
    private SomeBean bean;

    public SomeBean getSomeBean() {
        return bean;
    }
}
