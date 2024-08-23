package io.quarkus.arc.test.interceptors.noclassinterceptors;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.NoClassInterceptors;

@ApplicationScoped
@ClassLevel
public class InterceptedBean extends SuperclassWithInterceptor implements Superinterface {
    @MethodLevel
    @NoClassInterceptors
    public InterceptedBean() {
        super();
    }

    public void classLevel() {
    }

    @MethodLevel
    public void classLevelAndMethodLevel() {
    }

    @NoClassInterceptors
    @MethodLevel
    public void methodLevelOnly() {
    }

    @NoClassInterceptors
    public void noInterceptors() {
    }
}
