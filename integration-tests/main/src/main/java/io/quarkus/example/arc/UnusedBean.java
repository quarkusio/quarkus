package io.quarkus.example.arc;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

@Dependent
public class UnusedBean {

    @Inject
    InjectionPoint injectionPoint;

    public InjectionPoint getInjectionPoint() {
        return injectionPoint;
    }

}
